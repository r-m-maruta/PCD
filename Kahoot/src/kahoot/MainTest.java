package kahoot;

import kahoot.game.GameState;
import kahoot.io.QuizLoader;
import kahoot.questions.*;

import java.util.Scanner;

public class MainTest {
    public static void main(String[] args) {
        QuizLoader loader = new QuizLoader();
        QuizFile quizFile = loader.loadFromFile("src/kahoot/quizzes.json");

        Quiz quiz = quizFile.getQuizzes().getFirst();
        GameState game =  new GameState(quiz);

        Scanner sc = new Scanner(System.in);
        game.shuffleQuestions();

        while (true) {
            Question q = game.getCurrentQuestion();
            if (q == null) break;

            System.out.println("\n Pergunta: " + q.getQuestion());
            for (int i = 0; i < q.getOptions().size(); i++) {
                System.out.println(" " + i + ") " + q.getOptions().get(i));
            }

            System.out.println("Escolha a sua opção: ");
            int resposta = sc.nextInt();

            game.awnserQuestion(resposta);

            if(game.hasNextQuestion()){
                game.nextQuestion();
            } else {
                System.out.println("Fim do jogo! Pontuação final: " + game.getScore());
                break;
            }
        }
    }
}
