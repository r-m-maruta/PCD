package kahoot.ui;

import kahoot.net.client.ClientConnection;
import kahoot.net.client.ClientListener;
import kahoot.net.Message;
import kahoot.net.MessageType;
import kahoot.game.Question;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
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

        teamsPanel = new TeamsPanel();
        add(teamsPanel, BorderLayout.EAST);

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
        SwingUtilities.invokeLater(() -> {

            switch (msg.getType()) {

                case TEAMS_UPDATE -> {
                    teamsPanel.clearTeams();

                    @SuppressWarnings("unchecked")
                    Map<String, String> mapa = (Map<String, String>) msg.getData();

                    mapa.values().stream()
                            .distinct()
                            .forEach(teamsPanel::addTeam);
                }

                case SCORE_UPDATE -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> scores = (Map<String, Integer>) msg.getData();

                    scores.forEach((t, s) -> teamsPanel.updateScore(t, s));
                }

                case NEW_QUESTION -> {
                    Question q = (Question) msg.getData();
                    questionPanel.loadQuestion(q);
                }

                case GAME_OVER -> {
                    JOptionPane.showMessageDialog(this, "Jogo terminado!");
                }

                case ERROR -> {
                    JOptionPane.showMessageDialog(this, "Erro do servidor: " + msg.getData());
                }
            }

        });
    }

    public void sendAnswer(int optionIndex) {
        if (connection != null) {
            connection.send(new Message(MessageType.ANSWER, username, null, optionIndex));
        }
    }
}
