package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.gson.Gson;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

final public class Client {
    private final String address;
    private final String[] args;
    private final String uri =
            System.getProperty("user.dir") + "/src/main/java/client/data/";
    private String type;
    private String key;
    private String value;
    private String text;

    public Client(final String[] args) {
        this.address = "localhost";
        this.args = args;
        this.type = "";
        this.key = "";
        this.value = "";
        this.text = "";
    }

    public void getConnection() {
        try {
            int port = 23456;
            Socket socket =
                    new Socket(InetAddress.getByName(address), port);
            DataInputStream input =
                    new DataInputStream(socket.getInputStream());
            DataOutputStream output =
                    new DataOutputStream(socket.getOutputStream());
            getCommand();
            sendInput(socket, input, output);
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.out.println("Could not found the server");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Something went wrong with the connection!");
        }
    }

    private void sendInput(
            final Socket socket, final DataInputStream input,
            final DataOutputStream output) {
        System.out.println("Client started!");
        try {
            String command = getJson();
            System.out.println("Sent: " + command);

            output.writeUTF(command);
            String in = input.readUTF();

            System.out.println("Received: " + in);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Something went wrong with the connection!");
        } finally {
            try {
                socket.close();
                input.close();
                output.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Something went wrong with the connection!");
            }
        }
    }

    private String getJson() {
        String gson;
        if (!text.isEmpty()) {
            gson = readFromData();
        } else {
            Message message;
            if (type.equals("exit")) {
                message = new Message(type);
            } else if (value.isEmpty()) {
                message = new Message(type, key);
            } else {
                message = new Message(type, key, value);
            }
            gson = new Gson().toJson(message);
        }
        return gson;
    }

    private void getCommand() {
        ParametersClient parameters = new ParametersClient();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(parameters)
                .build();
        try {
            jCommander.parse(args);
            type = parameters.getTypeRequest();
            key = parameters.getKey();
            value = parameters.getMessage();
            text = parameters.getTextRequest();
        } catch (ParameterException e) {
            System.out.println("Error parsing arguments: " + e.getMessage());
            jCommander.usage();
        }
    }

    private String readFromData() {
        try {
            File file = new File(uri + text);
            if (!file.exists()) {
                System.out.println("File does not exist");
                return "error";
            }
            return new String(Files.readAllBytes(Paths.get(uri + text)));
        } catch (IOException e) {
            System.out.println("Other error");
            return "error";
        }
    }
}
