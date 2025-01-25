package server;

public class Main {
    public static void main(final String[] args) {
        Server server = new Server();
        server.receiveConnections();
    }
}
