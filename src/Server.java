
class UserManager {}
class AgentManager {}


/**
 * Gere a sessão de um cliente ligado ao servidor através de uma TaggedConnection.
 * 
 * Orquestra a comunicação de forma assíncrona usando duas threads dedicadas por cliente:
 * - readLoop: lê continuamente pedidos com tags únicas, despacha operações rápidas na hora
 *   e lança threads auxiliares para executar tarefas demoradas sem bloquear a leitura.
 * - writeLoop: retira respostas da fila de saída (outQueue) e envia-as pelo socket,
 *   garantindo que apenas esta thread escreve no socket e que clientes lentos não bloqueiam o servidor.
 */
class ServerWorker {
    private final TaggedConnection connection;
    private final UserManager userManager;
    private final AgentManager agentManager;
    private User authenticatedUser = null;

    private final LinkedList<TaggedConnection.Frame> outQueue = new LinkedList<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition hasFrames = queueLock.newCondition();
    private boolean closed = false;

    public ServerWorker(Socket socket, UserManager userManager, AgentManager agentManager) throws IOException {
        this.connection = new TaggedConnection(socket);
        this.userManager = userManager;
        this.agentManager = agentManager;
    }

    public void start() {
        new Thread(this::writeLoop).start();
        new Thread(this::readLoop).start();  
    }

    public void sendReply(int tag, byte[] data) {
        queueLock.lock();
        try {
            if (!closed) {
                outQueue.add(new TaggedConnection.Frame(tag, data));
                hasFrames.signal();
            }
        } finally {
            queueLock.unlock();
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                TaggedConnection.Frame frame;
                queueLock.lock();
                try {
                    while (outQueue.isEmpty() && !closed) {
                        hasFrames.await();
                    }
                    if (closed && outQueue.isEmpty()) break;
                    frame = outQueue.poll();
                } finally {
                    queueLock.unlock();
                }
                connection.send(frame); 
            }
        } catch (Exception e) {
            close();
        }
    }

    private void readLoop() {
        try {
            while (true) {
                TaggedConnection.Frame frame = connection.receive();

                DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.data));
                byte opcode = in.readByte();

                // @TODO: tratar opcodes aqui (ex: Login, Registo, Submeter Tarefa)
                // Se for uma tarefa demorada, lanças: new Thread(() -> ...).start();
                // E no fim respondes usando: sendReply(frame.tag, responseBytes);
            }
        } catch (EOFException e) {
            System.out.println("Cliente desligou-se.");
        } catch (Exception e) {
            System.out.println("Erro na ligação: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void close() {
        queueLock.lock();
        try {
            closed = true;
            hasFrames.signalAll();
        } finally {
            queueLock.unlock();
        }
        try { connection.close(); } catch (IOException ignored) {}
    }
}



public class Server {

    public static void main (String[] args) throws IOException {

        int port = 12345;
        int T = 10;    // tokens gastos por cada etapa
        int Lc = 100;  // limite de tokens por cliente
        int Li = 500;  // limite de tokens por instituicao

        for (int i = 0; i < args.length - 1; i += 2) {
            switch (args[i]) {
                case "-p":  port = Integer.parseInt(args[i + 1]); break;
                case "-T":  T    = Integer.parseInt(args[i + 1]); break;
                case "-Lc": Lc   = Integer.parseInt(args[i + 1]); break;
                case "-Li": Li   = Integer.parseInt(args[i + 1]); break;
            }
        }

        UserManager userManager = new UserManager(T, Lc, Li);
        AgentManager agentManager = new AgentManager();
        userManager.registerAdmin("admin", "admin");
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket socket = serverSocket.accept();
            ServerWorker worker = new ServerWorker(socket, userManager, agentManager);
            worker.start();
        }
    }

}