package kahoot.net.server;

import kahoot.io.QuizLoader;
import kahoot.game.*;
import kahoot.net.Message;
import kahoot.net.MessageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameMaster {

    private final int numTeams;
    private final int playersPerTeam;
    private final int totalPlayers;

    private final Map<String, ClientHandler> jogadores = new ConcurrentHashMap<>();
    private final Map<String, String> equipaPorJogador = new ConcurrentHashMap<>();
    private final Map<String, Integer> pontuacaoEquipa = new ConcurrentHashMap<>();

    private final List<Question> perguntas;
    private int perguntaAtual = 0;

    private boolean jogoAtivo = false;
    private static GameMaster instance;

    private ModifiedCountdownLatch latch;
    private SimpleBarrier barrier;

    private final Map<String, Integer> respostas = new HashMap<>();
    private final Object lock = new Object();

    private static final int TIMEOUT = 30_000;


    private GameMaster(int numTeams, int playersPerTeam) {
        this.numTeams = numTeams;
        this.playersPerTeam = playersPerTeam;
        this.totalPlayers = numTeams * playersPerTeam;

        for (int i = 0; i < numTeams; i++) {
            String name = "" + (char) ('A' + i);
            pontuacaoEquipa.put(name, 0);
        }

        QuizLoader loader = new QuizLoader();
        QuizFile file = loader.loadFromFile("/home/rmpgm/Documents/PCD/Projeto/Kahoot/src/kahoot/quizzes.json");

        if (file == null || file.getQuizzes().isEmpty())
            throw new RuntimeException("Erro: quizzes.json não encontrado");

        perguntas = new ArrayList<>(file.getQuizzes().get(0).getQuestions());
        Collections.shuffle(perguntas);
    }

    public static synchronized GameMaster createInstance(int t, int p) {
        if (instance != null) throw new IllegalStateException("GameMaster já existe");
        instance = new GameMaster(t, p);
        return instance;
    }

    public static GameMaster getInstance() { return instance; }


    public synchronized boolean tryLogin(String username, String equipa, ClientHandler h) {

        if (jogoAtivo) return false;
        if (!pontuacaoEquipa.containsKey(equipa)) return false;
        if (jogadores.containsKey(username)) return false;

        long count = equipaPorJogador.values().stream().filter(t -> t.equals(equipa)).count();
        if (count >= playersPerTeam) return false;

        jogadores.put(username, h);
        equipaPorJogador.put(username, equipa);

        sendTeams();

        if (jogadores.size() == totalPlayers)
            startGame();

        return true;
    }

    private void sendTeams() {
        broadcast(new Message(MessageType.TEAMS_UPDATE, "SERVER", null, new HashMap<>(equipaPorJogador)));
    }


    private void startGame() {
        jogoAtivo = true;

        Thread t = new Thread(this::gameLoop);
        t.start();
    }


    private void gameLoop() {

        while (perguntaAtual < perguntas.size()) {

            Question q = perguntas.get(perguntaAtual);
            boolean individual = (perguntaAtual % 2 == 0);

            respostas.clear();

            if (individual) {
                latch = new ModifiedCountdownLatch(2, 2, TIMEOUT, totalPlayers);
            } else {
                barrier = new SimpleBarrier(playersPerTeam, TIMEOUT);
            }

            broadcast(new Message(MessageType.NEW_QUESTION, "SERVER", null, q));
            System.out.println("Pergunta enviada: " + q.getQuestion());

            long deadline = System.currentTimeMillis() + TIMEOUT;

            synchronized (lock) {
                while (true) {

                    boolean terminou = (respostas.size() == totalPlayers);

                    if (!individual) {
                        for (String team : pontuacaoEquipa.keySet()) {
                            long rc = respostas.entrySet().stream()
                                    .filter(e -> equipaPorJogador.get(e.getKey()).equals(team))
                                    .count();

                            if (rc < playersPerTeam) {
                                terminou = false;
                            }
                        }
                    }

                    if (terminou) break;

                    long falta = deadline - System.currentTimeMillis();
                    if (falta <= 0) break;

                    try { lock.wait(falta); } catch (InterruptedException ignored) {}
                }
            }

            try {
                if (individual)
                    latch.await();
                else {
                    for (String team : pontuacaoEquipa.keySet())
                        barrier.await(team);
                }
            } catch (InterruptedException ignored) {}

            processResults(q, individual);
            sendScoreboard();

            perguntaAtual++;
        }

        broadcast(new Message(MessageType.GAME_OVER, "SERVER", null, new HashMap<>(pontuacaoEquipa)));
        jogoAtivo = false;
    }


    public void registerAwnser(String username, Object ansObj) {

        if (!jogoAtivo || perguntaAtual >= perguntas.size()) return;

        int resposta;
        try {
            resposta = (ansObj instanceof Integer)
                    ? (Integer) ansObj
                    : Integer.parseInt(ansObj.toString());
        } catch (Exception e) {
            return;
        }

        synchronized (lock) {

            if (respostas.containsKey(username)) return;

            respostas.put(username, resposta);
            String team = equipaPorJogador.get(username);

            boolean individual = (perguntaAtual % 2 == 0);

            if (individual) {
                latch.countdown();
            } else {
                barrier.arrive(team);
            }

            lock.notifyAll();
        }
    }


    private void processResults(Question q, boolean individual) {

        if (individual) {

            List<String> ordem = new ArrayList<>(respostas.keySet());
            int bonusCount = 0;

            for (String user : ordem) {

                int escolha = respostas.get(user);
                String team = equipaPorJogador.get(user);

                if (escolha == q.getCorrect()) {

                    int pontos = q.getPoints();
                    if (bonusCount < 2) {
                        pontos *= 2;
                        bonusCount++;
                    }

                    pontuacaoEquipa.put(team, pontuacaoEquipa.get(team) + pontos);
                }
            }

        } else {
            for (String team : pontuacaoEquipa.keySet()) {

                int total = 0, corretos = 0;

                for (var e : respostas.entrySet()) {
                    if (equipaPorJogador.get(e.getKey()).equals(team)) {
                        total++;
                        if (e.getValue() == q.getCorrect()) corretos++;
                    }
                }

                int ganho;
                if (total < playersPerTeam) {
                    ganho = 0;
                } else if (corretos == playersPerTeam) {
                    ganho = q.getPoints() * 2;
                } else if (corretos > 0) {
                    ganho = q.getPoints();
                } else ganho = 0;

                pontuacaoEquipa.put(team, pontuacaoEquipa.get(team) + ganho);
            }
        }
    }


    private void sendScoreboard() {
        broadcast(new Message(MessageType.SCORE_UPDATE, "SERVER", null, new HashMap<>(pontuacaoEquipa)));
    }


    private void broadcast(Message m) {
        for (ClientHandler h : jogadores.values())
            h.send(m);
    }
}
