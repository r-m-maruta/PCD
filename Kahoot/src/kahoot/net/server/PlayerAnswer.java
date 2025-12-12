package kahoot.net.server;

public class PlayerAnswer {
    public final String username;
    public final Object answer;
    public PlayerAnswer(String username, Object answer) {
        this.username = username;
        this.answer = answer;
    }
}
