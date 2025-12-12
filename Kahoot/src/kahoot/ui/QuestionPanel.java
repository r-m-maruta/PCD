package kahoot.ui;

import kahoot.game.Question;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class QuestionPanel extends JPanel {

    private JLabel questionLabel;
    private JButton[] optionButtons;
    private GameUI parent;

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
            int index = i;
            optionButtons[i] = new JButton("Opção " + (i + 1));
            optionButtons[i].addActionListener(e -> {
                setOptionsEnabled(false);
                parent.sendAnswer(index);
            });
            optionButtons[i].setEnabled(false);
            optionsPanel.add(optionButtons[i]);
        }
    }

    public void loadQuestion(Question q) {
        if (q == null) {
            questionLabel.setText("Nenhuma pergunta disponível.");
            setOptionsEnabled(false);
            return;
        }

        questionLabel.setText(q.getQuestion());

        List<String> opts = q.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            if (i < opts.size()) {
                optionButtons[i].setText(opts.get(i));
                optionButtons[i].setVisible(true);
                optionButtons[i].setEnabled(true);
            } else {
                optionButtons[i].setVisible(false);
                optionButtons[i].setEnabled(false);
            }
        }
        revalidate();
        repaint();
    }

    private void setOptionsEnabled(boolean v) {
        for (JButton b : optionButtons) b.setEnabled(v);
    }
}
