package kahoot.net.client;

import kahoot.net.Message;

public interface ClientListener {
    void onMessageReceived(Message message);
}
