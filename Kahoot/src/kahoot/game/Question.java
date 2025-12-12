package kahoot.game;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private String question;
    private int points;
    private int correct;
    private List<String> options;

    // Gson requires a no-arg constructor or uses reflection — fields private are OK.
    public String getQuestion() { return question; }
    public int getPoints(){ return points; }
    public int getCorrect(){ return correct; }
    public List<String> getOptions() { return options; }

    @Override
    public String toString() {
        return "Question{" +
                "question='" + question + '\'' +
                ", points=" + points +
                ", correct=" + correct +
                ", options=" + options +
                '}';
    }
}
