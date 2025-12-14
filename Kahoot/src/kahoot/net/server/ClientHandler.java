package kahoot.net.server;

import kahoot.net.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username = "??";
    private String team = "?";
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() { return username; }
    public String getTeam() { return team; }

    public void send(Message message){
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Erro ao enviar para " + username + ": " + e.getMessage());
            close();
        }
    }

    @Override
    public void run() {
        try {
            // create out then in (important for Object streams)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // ler mensagens
            while (running.get()) {
                Message m = (Message) in.readObject();
                if (m == null) continue;

                switch (m.getType()) {
                    case LOGIN -> {
                        this.username = m.getSender();
                        this.team = m.getTarget();
                        System.out.println("ClientHandler: LOGIN recebido: " + username + " (team " + team + ")");
                        boolean ok;
                        try {
                            ok = GameMaster.getInstance().tryLogin(username, team, this);
                        } catch (Exception ex) {
                            ok = false;
                        }
                        if (!ok) {
                            send(new Message(MessageType.ERROR, "SERVER", username, "Login rejeitado"));
                            close();
                            return;
                        } else {
                            // ack teams list sent by GameMaster when login
                        }
                    }

                    case ANSWER -> {
                        System.out.println("[ClientHandler] " + username + " respondeu: " + m.getData());
                        // route to server queue
                        ServerMain.handleAnswer(username, m.getData());
                    }

                    default -> {
                        System.out.println("ClientHandler: mensagem não tratada de " + username + " : " + m.getType());
                    }
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Cliente desconectou: " + username);
        } catch (Exception e) {
            System.out.println("Erro em ClientHandler (" + username + "): " + e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        if (!running.getAndSet(false)) return;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        ServerMain.removeClient(this);
        System.out.println("Ligação encerrada: " + username);
    }
}
