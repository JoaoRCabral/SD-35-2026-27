

public class TaggedConnection implements AutoCloseable {
    public static class Frame {
        public final int tag;
        public final byte[] data;

        public Frame(int tag, byte[] data) {
            this.tag = tag;
            this.data = data;
        }
    }

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private ReentrantLock sendLock = new ReentrantLock();
    private ReentrantLock receiveLock = new ReentrantLock();

    public TaggedConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public void send(Frame frame) throws IOException {
        send(frame.tag, frame.data);
    }

    public void send(int tag, byte[] data) throws IOException {
        sendLock.lock();
        try {
            out.writeInt(tag);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } finally {
            sendLock.unlock();
        }
    }

    public Frame receive() throws IOException {
        receiveLock.lock();
        try {
            int tag = in.readInt();
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);
            return new Frame(tag, data);
        } finally {
            receiveLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}