package kahoot.questions;

import java.util.List;

public class QuizFile {
    private List<Quiz> quizzes;

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    @Override
    public String toString() {
        return "QuizFile{" +
                "quizzes=" + quizzes +
                '}';
    }
}