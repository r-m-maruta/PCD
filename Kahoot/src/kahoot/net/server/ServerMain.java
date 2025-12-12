package kahoot.net.server;

import kahoot.net.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Server main:
 * - primeiro espera comando TUI "new <numTeams> <playersPerTeam>" (ex: new 2 1)
 * - depois aceita ligações e cria ClientHandlers
 * - consumer thread processa a queue e delega para GameMaster
 */
public class ServerMain {

    private static final int PORT = 9090;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final BlockingQueue<PlayerAnswer> answerQueue = new LinkedBlockingQueue<>();
    private static final ExecutorService clientPool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {

        // TUI simples
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
                System.out.println("Uso: new <numTeams> <playersPerTeam>   (ex: new 2 1)");
            } else {
                System.out.println("Comando desconhecido. Use: new <numTeams> <playersPerTeam>");
            }
        }

        // cria GameMaster
        GameMaster.createInstance(numTeams, playersPerTeam);

        // start consumer thread para processar answers enfileiradas
        Thread consumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PlayerAnswer pa = answerQueue.take();
                    // delegate to GameMaster
                    GameMaster.getInstance().registarResposta(pa.username, pa.answer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "AnswerConsumer");
        consumer.setDaemon(true);
        consumer.start();

        // start server socket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);
            while (true) {
                Socket s = serverSocket.accept();
                System.out.println("Cliente ligado: " + s);
                ClientHandler ch = new ClientHandler(s);
                clients.add(ch);
                clientPool.submit(ch);
            }
        } finally {
            clientPool.shutdownNow();
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
        clients.remove(ch);
        System.out.println("Cliente removido da lista: " + ch.getUsername());
    }
}
