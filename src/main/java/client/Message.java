package client;

final public class Message {
    private final String type;
    private String key;
    private String value;

    public Message(final String type) {
        this.type = type;
    }

    public Message(final String type, final String key) {
        this.type = type;
        this.key = key;
    }

    public Message(final String type,
                   final String key, final String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }
}
