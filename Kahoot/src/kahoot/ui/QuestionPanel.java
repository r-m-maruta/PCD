package kahoot.ui;

import kahoot.game.Question;

import javax.swing.*;
import java.awt.*;

public class QuestionPanel extends JPanel {

    private JLabel questionLabel;
    private JButton[] optionButtons;
    private final GameUI parent;

    public QuestionPanel(GameUI parent) {
        this.parent = parent;

        setLayout(new BorderLayout(10, 10));

        questionLabel = new JLabel("À espera da pergunta...", SwingConstants.CENTER);
        questionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(questionLabel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        add(optionsPanel, BorderLayout.CENTER);

        optionButtons = new JButton[4];

        for (int i = 0; i < 4; i++) {
            final int index = i;

            optionButtons[i] = new JButton("Opção " + (i + 1));
            optionButtons[i].setEnabled(false);

            optionButtons[i].addActionListener(e -> {
                parent.sendAnswer(index);
                setOptionsEnabled(false);
            });

            optionsPanel.add(optionButtons[i]);
        }
    }

    public void loadQuestion(Question q) {
        questionLabel.setText(q.getQuestion());

        for (int i = 0; i < optionButtons.length; i++) {
            if (i < q.getOptions().size()) {
                optionButtons[i].setText(q.getOptions().get(i));
                optionButtons[i].setVisible(true);
                optionButtons[i].setEnabled(true);
            } else {
                optionButtons[i].setVisible(false);
                optionButtons[i].setEnabled(false);
            }
        }
    }

    private void setOptionsEnabled(boolean enabled) {
        for (JButton b : optionButtons) {
            b.setEnabled(enabled);
        }
    }
}
