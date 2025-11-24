package kahoot.questions;

import java.util.List;

public class Quiz {
    private String name;
    private List<Question> questions;

    public String getName() {
        return name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    @Override
    public String toString() {
        return "Quizz{" +
                "name='" + name + '\'' +
                ", questions=" + questions +
                '}';
    }
}