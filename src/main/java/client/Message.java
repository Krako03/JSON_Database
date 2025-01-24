package client;

public class Message {
    private final String type;
    private String key;
    private String value;

    public Message(String type){
        this.type = type;
    }

    public Message(String type, String key){
        this.type = type;
        this.key = key;
    }

    public Message(String type, String key, String value){
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
