package  kahoot.game;

import kahoot.questions.*;

import java.util.List;
import java.util.Random;

public class GameState {

    private final Quiz currentQuiz;
    private int currentQuestionIndex = 0;
    private int score = 0;

    public GameState(Quiz quiz) {
        this.currentQuiz = quiz;
    }

    public Question getCurrentQuestion() {
        if(currentQuestionIndex < currentQuiz.getQuestions().size()) {
            return currentQuiz.getQuestions().get(currentQuestionIndex);
        }
        return null;
    }

    public boolean hasNextQuestion() {
        return currentQuestionIndex < currentQuiz.getQuestions().size() - 1;
    }

    public void nextQuestion() {
        if(hasNextQuestion()){
            currentQuestionIndex++;
        }
    }

    public void awnserQuestion(int chosenOptionIndex) {
        Question q = getCurrentQuestion();

        if(q == null) return;

        if(chosenOptionIndex == q.getCorrect()){
            System.out.println("Resposta correta " + q.getPoints() + " pontos");
            score += q.getPoints();
        } else {
            System.out.println("Resposta incorreta");
        }
    }

    public int getScore() {
        return score;
    }

    public void shuffleQuestions() {
        List<Question> questions = currentQuiz.getQuestions();
        java.util.Collections.shuffle(questions, new Random());
    }

}
