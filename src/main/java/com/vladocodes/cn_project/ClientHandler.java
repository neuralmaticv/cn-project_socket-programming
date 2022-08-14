package com.vladocodes.cn_project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler extends Thread {
    private final Server server;
    private final Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private boolean playing;
    private String symbol;
    private ClientHandler opponent;

    public ClientHandler(Socket client, Server server) throws IOException {
        this.socket = client;
        this.server = server;
        this.playing = false;

        this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.output = new PrintWriter(this.socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // Upon connecting, read username and send connected users list
            this.username = input.readLine();

            if (!server.isUsernameValid(username)) {
                output.println("Username is used");
            } else {
                output.println("OK");
                this.server.users.add(this);
                this.sendMessage("Igraci koji ne igraju: " + this.server.getFreeUsers(this));
                this.sendMessage("Igraci koji igraju: " + this.server.getUsersPlaying());
                this.server.broadcastToAll(this, "[OBAVJESTENJE]: ukljucio se novi igrac " + this.username);

                // Process the user (until he leaves the game)
                String userInput;
                do {

                    userInput = input.readLine();
                    if (userInput == null) {
                        break;
                    }

                    // analyze input
                    if (userInput.startsWith("igraj-")) {
                        String info[] = userInput.split("-");
                        opponent = this.server.getUser(info[1]);
                        opponent.opponent = this;
                        String msg = "[" + this.username + "]: Igraj sa mnom? da/ne";
                        this.server.broadcast(this, info[1], msg);
                    }
                    if (userInput.equals("da")) {
                        this.server.broadcast(this, this.opponent.username, "[OBAVJESTENJE]: Pocinje igra");
                        this.server.broadcast(this, this.username, "[OBAVJESTENJE]: Pocinje igra");
                        this.playing = true;
                        this.opponent.playing = true;
                        this.server.usersInGame.put(this.server.usersInGame.size(), new HashSet<>(Arrays.asList(this, this.opponent)));
                        String notification = this.username + " i " + this.opponent.username + " zapocinju igru!";
                        this.server.broadcastToAllFreePlayers(notification);
                        this.server.broadcastToAllFreePlayers(this.server.getUsersPlaying());
                    } else if (userInput.equals("ne")) {
                        String msg = this.opponent.username + " je odbio poziv za igru.";
                        this.server.broadcast(this, this.username, msg);
                    }
                } while (!userInput.equals("exit"));

                // Broadcast that user has disconnected
                this.server.broadcastToAll(this, this.username + " has left the game.");
            }
        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            this.server.remove(this);
            try {
                this.socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    void sendMessage(String message) {
        if (this.output != null)
            this.output.println(message);
    }

    public boolean isPlaying() {
        return playing;
    }

    String getUsername() {
        return this.username;
    }
}
