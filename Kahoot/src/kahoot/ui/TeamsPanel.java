package kahoot.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TeamsPanel extends JPanel {

    private final Map<String, JLabel> teamsLabels = new HashMap<>();

    public TeamsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Equipas"));
        setPreferredSize(new Dimension(160, 0));
    }

    public void addTeam(String teamName){
        if (teamName == null) return;
        if(!teamsLabels.containsKey(teamName)){
            JLabel label = new JLabel(teamName + ": 0 pontos");
            label.setFont(new Font("SansSerif", Font.BOLD, 14));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            teamsLabels.put(teamName, label);
            add(label);
            revalidate();
            repaint();
        }
    }

    public void updateScore(String teamName,  int score){
        JLabel label = teamsLabels.get(teamName);
        if(label != null) {
            label.setText(teamName + ": " + score + " pontos");
        }
    }

    public void removeTeam(String teamName){
        JLabel label = teamsLabels.get(teamName);
        if(label != null){
            remove(label);
            teamsLabels.remove(teamName);
            revalidate();
            repaint();
        }
    }

    public void clearTeams(){
        teamsLabels.clear();
        removeAll();
        revalidate();
        repaint();
    }
}
