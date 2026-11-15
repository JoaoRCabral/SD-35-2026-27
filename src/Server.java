
class UserManager {}
class AgentManager {}


class ServerWorker implements Runnable {
    private Socket socket;
    private final UserManager userManager;
    private final AgentManager agentManager;

    public ServerWorker(Socket socket) {
        this.socket = socket;
        this.userManager = userManager;
        this.agentManager = agentManager;
    }

    @Override
    public void run() {
        try{
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());


            boolean connected = true;
            while(connected){
                try{
                    
                } catch(EOFException e){
                    connected = false;
                    System.out.println("End of file");
                }
            }
            socket.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
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
            Thread worker = new Thread(new ServerWorker(socket, userManager, agentManager));
            worker.start();
        }
    }

}