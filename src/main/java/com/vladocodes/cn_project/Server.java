package com.vladocodes.cn_project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    public static final int PORT = 12345;
    public final Set<ClientHandler> users;
    public final Map<Game, Set<ClientHandler>> allGames;

    public static void main(String[] args) {
        Server server = new Server();
        server.execute();
    }

    public Server() {
        this.users = Collections.synchronizedSet(new HashSet<>());
        this.allGames = Collections.synchronizedMap(new HashMap<>());
    }

    private void execute() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.err.println("Server is listening on a port " + PORT);

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

    public void broadcast(ClientHandler sender, String message) {
        Set<ClientHandler> group = this.allGames.get(sender.getGame());

        synchronized (group) {
            group
                    .stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.sendMessage(message));
        }
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
                .forEach(u -> u.sendMessage("[OBAVJEŠTENJE]: " + message));
    }

    public void remove(ClientHandler client) {
        String username = client.getUsername();
        this.users.remove(client);
        System.err.println(username + " left");
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

    public String getUsersPlaying() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Game, Set<ClientHandler>> g : allGames.entrySet()) {
            sb.append(g.getKey().getID()).append("-[");

            for (ClientHandler u : g.getKey().getPlayers()) {
                sb.append(u.getUsername()).append(" ");
            }

            sb.append("], ");
        }

        return sb.toString();
    }

    public ClientHandler getUser(String username) {
        for (ClientHandler u : users) {
            if (u.getUsername().equals(username))
                return u;
        }

        return null;
    }

    public String getStatusInfo(ClientHandler user) {
        StringBuilder sb = new StringBuilder();

        sb.append("Igrači koji ne igraju: ").append(this.getFreeUsers(user)).append("\n");
        sb.append("Igrači koji igraju: ").append(this.getUsersPlaying()).append("\n");
        sb.append("\t");

        return sb.toString();
    }

    public void addWatcher(ClientHandler user, int gameID) {
        for (Game g: this.allGames.keySet()) {
            if (g.getID() == gameID) {
                this.allGames.get(g).add(user);
                g.addWatcher(user);
            }
        }
    }

    public void removeWatcher(ClientHandler user) {
        for (Game g: this.allGames.keySet()) {
            if (g.getWatchers().contains(user)) {
                this.allGames.get(g).remove(user);
                g.removeWatcher(user);
            }
        }
    }
}
