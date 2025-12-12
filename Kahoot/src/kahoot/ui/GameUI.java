package kahoot.ui;

import kahoot.net.client.ClientConnection;
import kahoot.net.client.ClientListener;
import kahoot.net.Message;
import kahoot.net.MessageType;
import kahoot.game.Question;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GameUI extends JFrame implements ClientListener {

    private final String username;
    private final String team;

    private QuestionPanel questionPanel;
    private TeamsPanel teamsPanel;
    private ClientConnection connection;

    public GameUI(String username, String team) {
        this.username = username;
        this.team = team;

        setTitle("Kahoot - Jogador: " + username + " | Equipa: " + team);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Painel das equipas
        teamsPanel = new TeamsPanel();
        add(teamsPanel, BorderLayout.EAST);

        // Painel das perguntas (inicialmente vazio)
        questionPanel = new QuestionPanel(this);
        add(questionPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    public void connectToServer(String host, int port) {
        connection = new ClientConnection(this);
        try {
            connection.connect(host, port, username, team);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao ligar ao servidor:\n" + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onMessageReceived(Message msg) {
        // All UI updates on EDT
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case TEAMS_UPDATE -> {
                    Object data = msg.getData();
                    teamsPanel.clearTeams();
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (Map<String, String>) data;
                        // extract unique team names
                        map.values().stream().distinct().forEach(teamsPanel::addTeam);
                    } else if (data instanceof java.util.Collection) {
                        @SuppressWarnings("unchecked")
                        java.util.Collection<String> c = (java.util.Collection<String>) data;
                        c.forEach(teamsPanel::addTeam);
                    }
                }
                case SCORE_UPDATE -> {
                    Object d = msg.getData();
                    if (d instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> scores = (Map<String, Integer>) d;
                        scores.forEach((t, s) -> teamsPanel.updateScore(t, s));
                    }
                }
                case NEW_QUESTION -> {
                    Object d = msg.getData();
                    if (d instanceof Question) {
                        questionPanel.loadQuestion((Question) d);
                    }
                }
                case GAME_OVER -> {
                    JOptionPane.showMessageDialog(this, "Jogo terminado!");
                }
                case ERROR -> {
                    JOptionPane.showMessageDialog(this, "Erro do servidor: " + msg.getData());
                }
                default -> System.out.println("Mensagem ignorada: " + msg.getType());
            }
        });
    }

    public void sendAnswer(int optionIndex) {
        if (connection == null) {
            System.err.println("Não ligado ao servidor.");
            return;
        }
        connection.send(new Message(MessageType.ANSWER, username, null, optionIndex));
    }
}
