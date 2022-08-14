package com.vladocodes.cn_project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    public static final int PORT = 12345;
    public final Set<ClientHandler> users;
    public final Map<Integer, Set<ClientHandler>> usersInGame;

    public static void main(String[] args) {
        Server server = new Server();
        server.execute();
    }

    public Server() {
        this.users = Collections.synchronizedSet(new HashSet<>());
        this.usersInGame = Collections.synchronizedMap(new HashMap<>());
    }

    private void execute() {
        try (ServerSocket serverSocket = new ServerSocket(this.PORT)) {
            System.err.println("Server is listening on a port " + this.PORT);

            while (true) {
                    Socket clientSocket = serverSocket.accept();

                    try {
                        ClientHandler user = new ClientHandler(clientSocket, this);
                        user.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isUsernameValid(String username) {
        for (ClientHandler user : users) {
            if (user.getUsername().equals(username)) {
                return false;
            }
        }

        return true;
    }

    public void broadcastToAll(ClientHandler sender, String message) {
        synchronized (this.users) {
            this.users.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.sendMessage(message));
        }
    }

    public void broadcastToAllFreePlayers(String message) {
        this.users.stream()
                .filter(u -> !u.isPlaying())
                .forEach(u -> u.sendMessage("[OBAVJESTENJE]: " + message));
    }

    public void broadcast(ClientHandler sender, String opponent, String message) {
        for (ClientHandler u : users) {
            if (u.getUsername().equals(opponent)) {
                u.sendMessage(message);
            }
        }
    }

    public void remove(ClientHandler client) {
        String username = client.getUsername();
        this.users.remove(client);
        System.err.println(username + " left");
    }

    public List<String> getUsers() {
        synchronized (this.users) {
            return this.users.stream()
                    .map(ClientHandler::getUsername)
                    .collect(Collectors.toList());
        }
    }

    public List<String> getFreeUsers(ClientHandler currentUser) {
        synchronized (this.users) {
            return this.users.stream()
                    .filter(u -> u != currentUser)
                    .filter(u -> !u.isPlaying())
                    .map(ClientHandler::getUsername)
                    .collect(Collectors.toList());
        }
    }

    public ClientHandler getUser(String username) {
        for (ClientHandler u : users) {
            if (u.getUsername().equals(username))
                return u;
        }

        return null;
    }
    
    public String getUsersPlaying() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Integer, Set<ClientHandler>> e : usersInGame.entrySet()) {
            sb.append(e.getKey()).append("-[");

            for (ClientHandler u : e.getValue()) {
                sb.append(u.getUsername()).append(" ");
            }

            sb.append("], ");
        }

        return sb.toString();
    }
}
