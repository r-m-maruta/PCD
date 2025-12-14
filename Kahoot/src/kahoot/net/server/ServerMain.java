package kahoot.net.server;

import kahoot.net.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class ServerMain {

    private static final int PORT = 9090;

    private static final ArrayList<ClientHandler> clients = new ArrayList<>();

    private static final BlockingQueue<PlayerAnswer> answerQueue = new BlockingQueue<>(100);

    public static void main(String[] args) throws Exception {

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Servidor pronto. Crie um jogo com: new <numTeams> <playersPerTeam>");

        int numTeams = 0, playersPerTeam = 0;

        while (true) {
            System.out.print("> ");
            String line = console.readLine();
            if (line == null) continue;
            line = line.trim();

            if (line.startsWith("new ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        numTeams = Integer.parseInt(parts[1]);
                        playersPerTeam = Integer.parseInt(parts[2]);
                        if (numTeams > 0 && playersPerTeam > 0) break;
                    } catch (NumberFormatException ignored) {}
                }
                System.out.println("Uso: new <numTeams> <playersPerTeam>");
            } else {
                System.out.println("Comando desconhecido");
            }
        }

        GameMaster.createInstance(numTeams, playersPerTeam);

        Thread consumer = new Thread(() -> {
            while (true) {
                try {
                    PlayerAnswer pa = answerQueue.take(); // usa BlockingQueue tua
                    GameMaster.getInstance().registerAwnser(pa.username, pa.answer);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "AnswerConsumer");

        consumer.setDaemon(true);
        consumer.start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);

            while (true) {
                Socket s = serverSocket.accept();
                System.out.println("Cliente ligado: " + s);

                ClientHandler ch = new ClientHandler(s);

                synchronized (clients) {
                    clients.add(ch);
                }

                new Thread(ch, "Client-" + s.getPort()).start();
            }
        }
    }

    public static void handleAnswer(String username, Object answer) {
        try {
            answerQueue.put(new PlayerAnswer(username, answer));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void removeClient(ClientHandler ch) {
        synchronized (clients) {
            clients.remove(ch);
        }
        System.out.println("Cliente removido: " + ch.getUsername());
    }
}
