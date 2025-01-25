package client;

public class Main {
    public static void main(final String[] args) {
        Client client = new Client(args);
        client.getConnection();
    }
}

