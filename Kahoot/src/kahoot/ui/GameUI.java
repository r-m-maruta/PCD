package kahoot.ui;

import kahoot.game.GameState;
import kahoot.questions.Question;

import javax.swing.*;
import java.awt.*;

public class GameUI extends JFrame {

    private GameState gameState;
    private QuestionPanel questionPanel;
    private JLabel scoreLabel;

    public GameUI(GameState gameState) {
        this.gameState = gameState;
        setTitle("Kahoot - Jogo de PCD");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        scoreLabel = new JLabel("Pontuação: 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(scoreLabel, BorderLayout.NORTH);

        questionPanel = new QuestionPanel(this, gameState);
        add(questionPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    public void updateScore(int newScore) {
        scoreLabel.setText("Pontuação: " + newScore);
    }

    public void showNextQuestion() {
        if (gameState.hasNextQuestion()) {
            gameState.nextQuestion();
            questionPanel.loadQuestion(gameState.getCurrentQuestion());
        } else {
            JOptionPane.showMessageDialog(this,
                    "Fim do quiz!\nPontuação final: " + gameState.getScore(),
                    "Fim do Jogo", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
