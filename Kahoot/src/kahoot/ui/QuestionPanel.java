package kahoot.ui;

import kahoot.game.GameState;
import kahoot.questions.Question;
import kahoot.ui.GameUI;

import javax.swing.*;
import java.awt.*;

public class QuestionPanel extends JPanel {

    private GameState gameState;
    private JLabel questionLabel;
    private JButton[] optionButtons;
    private GameUI parent;

    public QuestionPanel(GameUI parent, GameState gameState) {
        this.parent = parent;
        this.gameState = gameState;

        setLayout(new BorderLayout(10, 10));

        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(questionLabel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        add(optionsPanel, BorderLayout.CENTER);

        optionButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            int index = i;
            optionButtons[i] = new JButton();
            optionButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 14));
            optionButtons[i].addActionListener(e -> handleAnswer(index));
            optionsPanel.add(optionButtons[i]);
        }

        loadQuestion(gameState.getCurrentQuestion());
    }

    public void loadQuestion(Question q) {
        if (q == null) return;
        questionLabel.setText( q.getQuestion());

        java.util.List<String> options = q.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            if (i < options.size()) {
                optionButtons[i].setText(options.get(i));
                optionButtons[i].setEnabled(true);
                optionButtons[i].setVisible(true);
            } else {
                optionButtons[i].setVisible(false);
            }
        }
    }

    private void handleAnswer(int index) {
        gameState.awnserQuestion(index);
        parent.updateScore(gameState.getScore());
        parent.showNextQuestion();
    }
}
