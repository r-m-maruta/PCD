package kahoot.game;

import java.io.Serializable;
import java.util.List;

public class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<Question> questions;

    public String getName() { return name; }
    public List<Question> getQuestions() { return questions; }

    @Override
    public String toString() {
        return "Quiz{" + "name='" + name + '\'' + ", questions=" + questions + '}';
    }
}
