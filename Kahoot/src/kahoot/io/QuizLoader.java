package kahoot.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kahoot.questions.QuizFile;

import java.io.FileReader;
import java.io.IOException;

public class QuizLoader {

    public QuizFile loadFromFile(String fileName) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try(FileReader reader = new FileReader(fileName)) {
            QuizFile quizFile = gson.fromJson(reader, QuizFile.class);

            System.out.println("Ficheiro carregado com sucesso");
            return quizFile;
        } catch (IOException e) {
            System.err.println("Erro ao carregar o ficheiro -" + e.getMessage());
            return null;
        }
    }
}