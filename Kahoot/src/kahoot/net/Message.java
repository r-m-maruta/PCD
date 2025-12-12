package kahoot.net;

import java.io.Serializable;

public class Message implements Serializable {

    private MessageType type;
    private String sender;
    private String target;
    private Object data;

    public Message(MessageType type, String sender, String target, Object data) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.data = data;
    }

    public MessageType getType() { return type; }
    public String getSender() { return sender; }
    public String getTarget() { return target; }
    public Object getData() { return data; }
}
