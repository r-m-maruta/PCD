package kahoot.net.client;

import kahoot.net.Message;
import kahoot.net.MessageType;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClientConnection {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final ClientListener listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread listenThread;

    public ClientConnection(ClientListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String username, String team) throws IOException {
        socket = new Socket(host, port);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        running.set(true);

        // send login
        send(new Message(MessageType.LOGIN, username, team, null));

        listenThread = new Thread(this::listenLoop, "ClientConnection-Listen");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public synchronized void send(Message m) {
        if (out == null || !running.get()) return;
        try {
            out.writeObject(m);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro a enviar mensagem: " + e.getMessage());
            close();
        }
    }

    private void listenLoop() {
        try {
            while (running.get()) {
                Message m = (Message) in.readObject();
                if (m != null) listener.onMessageReceived(m);
            }
        } catch (SocketException se) {
            System.err.println("Ligação terminada.");
        } catch (Exception e) {
            System.err.println("Erro no listener: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        if (!running.getAndSet(false)) return;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        System.out.println("ClientConnection: ligação encerrada.");
    }
}
