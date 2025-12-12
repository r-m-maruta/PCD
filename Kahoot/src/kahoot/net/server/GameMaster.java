package kahoot.net.server;

import kahoot.io.QuizLoader;
import kahoot.game.*;
import kahoot.net.Message;
import kahoot.net.MessageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameMaster simples e linear:
 * - Alterna perguntas: individual / equipa / individual / ...
 * - Timeout por pergunta (30s)
 * - Bónus: primeiros 2 respondentes de perguntas individuais -> multiplier 2
 *
 * Mantém estruturas simples e usa wait/notify para acordar quando respostas chegam.
 */
public class GameMaster {

    private int numTeams;
    private int playersPerTeam;
    private int totalPlayers;

    // jogadores: username -> handler
    private final Map<String, ClientHandler> jogadores = new ConcurrentHashMap<>();
    // equipa por jogador: username -> teamName
    private final Map<String, String> equipaPorJogador = new ConcurrentHashMap<>();
    // pontuação por equipa
    private final Map<String, Integer> pontuacaoEquipa = new ConcurrentHashMap<>();

    // perguntas carregadas do quiz
    private final List<Question> perguntas;
    private int perguntaAtual = 0;

    private boolean jogoAtivo = false;

    private final Object lock = new Object();

    private static GameMaster instance;

    private final long QUESTION_TIMEOUT_MS = 30_000L;
    private final int BONUS_COUNT = 2;
    private final int BONUS_FACTOR = 2;

    private GameMaster(int numTeams, int playersPerTeam) {
        this.numTeams = numTeams;
        this.playersPerTeam = playersPerTeam;
        this.totalPlayers = numTeams * playersPerTeam;

        // inicializa pontuações com nomes genéricos A, B, C...
        for (int i = 0; i < numTeams; i++) {
            String name = Character.toString((char) ('A' + i));
            pontuacaoEquipa.put(name, 0);
        }

        // carrega quiz (único quiz)
        QuizLoader loader = new QuizLoader();
        QuizFile file = loader.loadFromFile("/home/rmpgm/Documents/PCD/Projeto/Kahoot/src/kahoot/quizzes.json");
        if (file == null || file.getQuizzes().isEmpty()) {
            System.err.println("GameMaster: erro a carregar quizzes.json");
            perguntas = Collections.emptyList();
        } else {
            Quiz quiz = file.getQuizzes().get(0);
            perguntas = new ArrayList<>(quiz.getQuestions());
            Collections.shuffle(perguntas);
        }
        System.out.println("GameMaster criado: teams=" + numTeams + ", players/team=" + playersPerTeam
                + ", perguntas=" + perguntas.size());
    }

    public static synchronized GameMaster createInstance(int numTeams, int playersPerTeam) {
        if (instance != null) throw new IllegalStateException("GameMaster já criado");
        instance = new GameMaster(numTeams, playersPerTeam);
        return instance;
    }

    public static synchronized GameMaster getInstance() {
        if (instance == null) throw new IllegalStateException("GameMaster não inicializado");
        return instance;
    }

    // ---------- login dos jogadores ----------
    /**
     * Tenta registar jogador. Retorna true se aceite.
     */
    public synchronized boolean tentarLogin(String username, String equipa, ClientHandler handler) {
        if (jogoAtivo) return false;
        if (jogadores.containsKey(username)) return false;
        // valida equipa (A,B,C...)
        if (!pontuacaoEquipa.containsKey(equipa)) return false;

        long count = equipaPorJogador.values().stream().filter(t -> t.equals(equipa)).count();
        if (count >= playersPerTeam) return false;

        jogadores.put(username, handler);
        equipaPorJogador.put(username, equipa);

        System.out.println("GameMaster: jogador ligado " + username + " (equipa " + equipa + ")");

        // avisa todos com a lista de jogadores->equipas
        enviarTEAMS_UPDATE();

        if (jogadores.size() == totalPlayers) {
            startGame();
        }
        return true;
    }

    private void enviarTEAMS_UPDATE() {
        // enviamos um Map username->team (simples)
        Message m = new Message(MessageType.TEAMS_UPDATE, "SERVER", null, new HashMap<>(equipaPorJogador));
        broadcast(m);
    }

    // ---------- start / main loop ----------
    private void startGame() {
        jogoAtivo = true;
        System.out.println("GameMaster: todos ligados — a iniciar jogo.");
        Thread t = new Thread(this::questionLoop, "GM-QuestionLoop");
        t.setDaemon(true);
        t.start();
    }

    private void questionLoop() {
        while (perguntaAtual < perguntas.size()) {
            Question q = perguntas.get(perguntaAtual);
            boolean isIndividual = (perguntaAtual % 2 == 0);

            // estruturas temporárias para recolha de respostas desta ronda
            Map<String, Integer> respPorJogador = new HashMap<>(); // username -> chosenIndex
            List<String> arrivalOrder = new ArrayList<>(); // order of responders
            Map<String, Map<String, Integer>> respPorTeam = new HashMap<>(); // team -> (username->chosen)
            for (String team : pontuacaoEquipa.keySet()) respPorTeam.put(team, new HashMap<>());

            // disponibilizamos estas estruturas para o registarResposta através de campos de instância mínimos:
            CurrentRound cr = new CurrentRound(isIndividual, q, respPorJogador, arrivalOrder, respPorTeam);
            setCurrentRound(cr);

            // envia pergunta
            broadcast(new Message(MessageType.NEW_QUESTION, "SERVER", null, q));
            System.out.println("[GM] Enviada pergunta #" + perguntaAtual + " (individual=" + isIndividual + "): " + q.getQuestion());

            // espera por respostas ou timeout
            long waitUntil = System.currentTimeMillis() + QUESTION_TIMEOUT_MS;
            synchronized (lock) {
                while (true) {
                    long now = System.currentTimeMillis();
                    long remaining = waitUntil - now;
                    // condição para terminar:
                    if (isIndividual) {
                        if (cr.respPorJogador.size() >= totalPlayers) break;
                    } else {
                        boolean allTeamsReady = true;
                        for (Map<String, Integer> m : cr.respPorTeam.values()) {
                            if (m.size() < playersPerTeam) { allTeamsReady = false; break; }
                        }
                        if (allTeamsReady) break;
                    }
                    if (remaining <= 0) {
                        System.out.println("[GM] Timeout da pergunta #" + perguntaAtual);
                        break;
                    }
                    try {
                        lock.wait(remaining);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // processar respostas recolhidas em 'cr'
            processRoundResults(cr);

            // limpar current round
            clearCurrentRound();

            perguntaAtual++;
        }

        // fim do jogo
        enviarScoreboard();
        broadcast(new Message(MessageType.GAME_OVER, "SERVER", null, new HashMap<>(pontuacaoEquipa)));
        System.out.println("GameMaster: jogo terminado.");
        jogoAtivo = false;
    }

    // objecto que representa ronda actual
    private volatile CurrentRound currentRound = null;
    private void setCurrentRound(CurrentRound cr) { this.currentRound = cr; }
    private void clearCurrentRound() { this.currentRound = null; }

    /**
     * Chamada por ClientHandler quando receber resposta.
     */
    public void registarResposta(String username, Object respostaObj) {
        // respostaObj esperança: Integer (index)
        int chosen = -1;
        if (respostaObj instanceof Integer) chosen = (Integer) respostaObj;
        else {
            try { chosen = Integer.parseInt(respostaObj.toString()); } catch (Exception ignored) {}
        }

        CurrentRound cr = currentRound;
        if (cr == null) {
            System.out.println("[GM] Resposta recebida fora de ronda: " + username);
            return;
        }

        synchronized (lock) {
            if (cr.isIndividual) {
                if (!cr.respPorJogador.containsKey(username)) {
                    cr.respPorJogador.put(username, chosen);
                    cr.arrivalOrder.add(username);
                } else {
                    System.out.println("[GM] já respondeu: " + username);
                }
            } else {
                String team = equipaPorJogador.get(username);
                if (team == null) {
                    System.out.println("[GM] resposta de jogador sem equipa: " + username);
                } else {
                    Map<String, Integer> teamMap = cr.respPorTeam.get(team);
                    if (teamMap != null && !teamMap.containsKey(username)) {
                        teamMap.put(username, chosen);
                    }
                }
            }
            // notifica o questionLoop
            lock.notifyAll();
        }
    }

    private void processRoundResults(CurrentRound cr) {
        Question q = cr.question;
        if (cr.isIndividual) {
            // Para cada jogador, se acertou, soma pontos. Aplica bonus aos primeiros BONUS_COUNT que acertaram.
            Set<String> correctPlayers = new HashSet<>();
            for (Map.Entry<String, Integer> e : cr.respPorJogador.entrySet()) {
                if (e.getValue() == q.getCorrect()) correctPlayers.add(e.getKey());
            }
            // determinar ordem dos que acertaram na arrivalOrder
            int bonusGiven = 0;
            for (String user : cr.arrivalOrder) {
                if (!correctPlayers.contains(user)) continue;
                int multiplier = (bonusGiven < BONUS_COUNT) ? BONUS_FACTOR : 1;
                String team = equipaPorJogador.get(user);
                if (team == null) continue;
                pontuacaoEquipa.put(team, pontuacaoEquipa.getOrDefault(team, 0) + q.getPoints() * multiplier);
                bonusGiven++;
            }
            // quem acertou mas não entrou nos primeiros BONUS_COUNT já foi contemplado acima (multiplier 1 ou 2 accordingly)
        } else {
            // perguntas por equipa: se todos acertam -> pontos duplicados (points*2)
            // se algum falha -> considerar melhor pontuação entre os membros (que neste modelo é q.getPoints() se houve alguém correto, senão 0)
            for (String team : pontuacaoEquipa.keySet()) {
                Map<String, Integer> teamMap = cr.respPorTeam.get(team);
                int teamScoreGain = 0;
                if (teamMap != null && teamMap.size() > 0) {
                    boolean allCorrect = true;
                    boolean anyCorrect = false;
                    for (Integer chosen : teamMap.values()) {
                        if (chosen == null || chosen != q.getCorrect()) {
                            allCorrect = false;
                        } else {
                            anyCorrect = true;
                        }
                    }
                    if (allCorrect && teamMap.size() == playersPerTeam && anyCorrect) {
                        teamScoreGain = q.getPoints() * 2;
                    } else if (anyCorrect) {
                        teamScoreGain = q.getPoints(); // melhor pontuação entre eles (aqui: q.getPoints())
                    } else {
                        teamScoreGain = 0;
                    }
                }
                pontuacaoEquipa.put(team, pontuacaoEquipa.getOrDefault(team, 0) + teamScoreGain);
            }
        }

        // after scoring, send scoreboard
        enviarScoreboard();
    }

    private void enviarScoreboard() {
        broadcast(new Message(MessageType.SCORE_UPDATE, "SERVER", null, new HashMap<>(pontuacaoEquipa)));
    }

    // broadcast helper
    private void broadcast(Message m) {
        for (ClientHandler h : jogadores.values()) {
            h.send(m);
        }
    }

    public Set<String> getTeamNames() {
        return pontuacaoEquipa.keySet();
    }

    // minimal container for round
    private static class CurrentRound {
        final boolean isIndividual;
        final Question question;
        final Map<String, Integer> respPorJogador;
        final List<String> arrivalOrder;
        final Map<String, Map<String, Integer>> respPorTeam;

        CurrentRound(boolean isIndividual, Question q,
                     Map<String, Integer> respPorJogador,
                     List<String> arrivalOrder,
                     Map<String, Map<String, Integer>> respPorTeam) {
            this.isIndividual = isIndividual;
            this.question = q;
            this.respPorJogador = respPorJogador;
            this.arrivalOrder = arrivalOrder;
            this.respPorTeam = respPorTeam;
        }
    }
}
