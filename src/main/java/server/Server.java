package server;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.Gson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {
    private final String address;
    private final int port;
    private final int THREAD_POOL_SIZE = 10;
    private boolean active;
    private final String uri = System.getProperty("user.dir") + "/src/server/data/db.json";
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Server() {
        this.address = "127.0.0.1";
        this.port = 23456;
        this.active = true;
    }

    public void receiveConnections(){
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {
            server.setSoTimeout(1000);
            System.out.println("Server started!");
            while (active) {
                try {
                    Socket socket = server.accept();
                    executorService.submit(() -> receiveInput(socket));
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.out.println("Could not found the server");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Something went wrong with the connection!");
        } finally {
            executorService.shutdown();
        }
    }

    private void receiveInput(Socket socket){
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            String command = input.readUTF();
            System.out.println("Received: " + command);

            JsonNode requestJson = objectMapper.readTree(command);

            String response;

            switch (requestJson.get("type").asText()) {
                case "get" -> response = getValue(requestJson);
                case "set" -> response = setValue(requestJson);
                case "delete" -> response = delete(requestJson);
                case "exit" -> response = exit();
                default -> response = error();
            }

            System.out.println("Sent: " + response);
            output.writeUTF(response);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Something went wrong with the connection!");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket: " + e.getMessage());
            }
        }
    }

    private String setValue(JsonNode jsonNode){
        Response response;
        lock.writeLock().lock();
        JsonNode key = jsonNode.get("key");
        JsonNode value = jsonNode.get("value");

        try {
            File file = new File(uri);

            ObjectNode root = file.exists() ?
                    (ObjectNode) objectMapper.readTree(file) : objectMapper.createObjectNode();

            traverseJsonSet(root, key, value);

//            objectMapper.writer().writeValue(file, root);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            response = new Response("OK");
        } catch (IOException e) {
            response = new Response("ERROR");
        } finally {
            lock.writeLock().unlock();
        }
        return new Gson().toJson(response);
    }

    private String getValue(JsonNode jsonNode){
        Response response;
        lock.readLock().lock();
        label: try {
            File file = new File(uri);
            if (!file.exists()) {
                response = new Response("ERROR", "No such key");
                break label;
            }

            ObjectNode root = (ObjectNode) objectMapper.readTree(file);
            JsonNode value = traverseJsonGet(root, jsonNode.get("key"));

            if (value == null) {
                response = new Response("ERROR", "No such key");
            } else {
                if (value.isTextual()) {
                    response = new Response("OK", value.textValue());
                } else {
                    response = new Response("OK", value.toString());
                    StringBuilder val = new StringBuilder(new Gson().toJson(response));
                    int index = val.indexOf("value\"");
                    val.deleteCharAt(index + 7);
                    index = val.length();
                    val.deleteCharAt(index - 2);
                    return val.toString().replace("\\", "");
                }
            }
        } catch (IOException e) {
            response = new Response("ERROR");
        } finally {
            lock.readLock().unlock();
        }
        return new Gson().toJson(response); //.replace("\\", "");
    }

    private String delete(JsonNode jsonNode){
        Response response;
        lock.writeLock().lock();
        JsonNode key = jsonNode.get("key");
        label: try {
            File file = new File(uri);
            if (!file.exists()) {
                response = new Response("ERROR", "No such key");
                break label;
            }

            ObjectNode root = file.exists() ?
                    (ObjectNode) objectMapper.readTree(file) : objectMapper.createObjectNode();

            boolean flag = traverseJsonDelete(root, key);

            if (flag) {
                //            objectMapper.writer().writeValue(file, root);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
                response = new Response("OK");
            } else {
                response = new Response("ERROR", "No such key");
            }
        } catch (IOException e) {
            response = new Response("ERROR");
        } finally {
            lock.writeLock().unlock();
        }
        return new Gson().toJson(response);
    }

    private String exit(){
        active = false;
        Response response = new Response("OK");
        return new Gson().toJson(response);
    }

    private String error(){
        Response response = new Response("ERROR", "Wrong command");
        return new Gson().toJson(response);
    }

    private JsonNode traverseJsonGet(JsonNode root, JsonNode keyNode) {
        if (keyNode.isTextual()) {
            return root.get(keyNode.asText());
        }

        if (keyNode.isArray()) {
            JsonNode current = root;
            for (JsonNode key : keyNode) {
                if (current == null) {
                    return null;
                }
                current = current.get(key.asText());
            }
            return current;
        }
        return null;
    }

    private void traverseJsonSet(ObjectNode root, JsonNode keyNode, JsonNode valueNode) {
        if (keyNode.isTextual()) {
            root.set(keyNode.asText(), valueNode);
        } else if (keyNode.isArray()) {
            JsonNode currentNode = root;
            ArrayNode keyArray = (ArrayNode) keyNode;

            for (int i = 0; i < keyArray.size(); i++) {
                String currentKey = keyArray.get(i).asText();

                if (i == keyArray.size() - 1) {
                    if (currentNode instanceof ObjectNode) {
                        ((ObjectNode) currentNode).set(currentKey, valueNode);
                    }
                } else {
                    if (currentNode instanceof ObjectNode objectNode) {

                        if (!objectNode.has(currentKey)) {
                            objectNode.set(currentKey, objectMapper.createObjectNode());
                        }

                        currentNode = objectNode.get(currentKey);
                    } else {
                        throw new IllegalStateException("Path traversal failed, invalid structure.");
                    }
                }
            }
        }
    }

    private boolean traverseJsonDelete(ObjectNode root, JsonNode keyNode) {
        boolean flag = false;
        if (keyNode.isTextual() && root.has(keyNode.asText())) {
            root.remove(keyNode.asText());
            flag = true;
        } else if (keyNode.isArray()) {
            JsonNode currentNode = root;
            ArrayNode keyArray = (ArrayNode) keyNode;

            for (int i = 0; i < keyArray.size(); i++) {
                String currentKey = keyArray.get(i).asText();

                if (i == keyArray.size() - 1) {
                    if (currentNode instanceof ObjectNode && currentNode.has(currentKey)) {
                        ((ObjectNode) currentNode).remove(currentKey);
                        flag = true;
                    }
                } else {
                    if (currentNode instanceof ObjectNode objectNode) {
                        if (objectNode.has(currentKey)) {
                            currentNode = objectNode.get(currentKey);
                        }
                    } else {
                        throw new IllegalStateException("Path traversal failed, invalid structure.");
                    }
                }
            }
        }
        return flag;
    }
}
