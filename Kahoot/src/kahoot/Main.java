package kahoot;

import kahoot.game.GameState;
import kahoot.ui.GameUI;
import kahoot.io.QuizLoader;
import kahoot.questions.*;

public class Main {
    public static void main(String[] args) {
        QuizLoader loader = new QuizLoader();
        QuizFile quizFile = loader.loadFromFile("Kahoot/src/kahoot/quizzes.json");

        Quiz quiz = quizFile.getQuizzes().get(0);
        GameState game = new GameState(quiz);
        game.shuffleQuestions();

        javax.swing.SwingUtilities.invokeLater(() -> {
            new GameUI(game);
        });
    }
}
