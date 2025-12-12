package kahoot.game;

import java.io.Serializable;
import java.util.List;

public class QuizFile implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Quiz> quizzes;
    public List<Quiz> getQuizzes() { return quizzes; }

    @Override
    public String toString() { return "QuizFile{quizzes=" + quizzes + '}'; }
}
