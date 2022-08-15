package com.vladocodes.cn_project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;

public class ClientHandler extends Thread {
    private final Server server;
    private final Socket socket;
    private final BufferedReader fromUser;
    private final PrintWriter toUser;
    private String username;
    private boolean isPlaying;
    private boolean isWatching;
    private boolean hasInvite;
    private String symbol;
    private ClientHandler opponent;
    private Game game;

    public ClientHandler(Socket client, Server server) throws IOException {
        this.socket = client;
        this.server = server;
        this.isPlaying = false;
        this.isWatching = false;
        this.hasInvite = false;

        this.fromUser = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.toUser = new PrintWriter(this.socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // Upon connecting, read username and send connected users list
            this.username = fromUser.readLine();

            if (!server.isUsernameValid(username)) {
                toUser.println("Korisničko ime je zauzeto.");
            } else {
                toUser.println("OK");
                this.server.users.add(this);
                this.sendMessage(this.server.getStatusInfo(this));
                this.server.broadcastToAll(this, "[OBAVJEŠTENJE]: Pridružio se novi igrač " + this.username);

                // Process the user (until he leaves the game)
                String userInput;
                boolean skipFlag;
                do {
                    skipFlag = false;

                    userInput = fromUser.readLine();
                    if (userInput == null) {
                        break;
                    }

                    // analyze input
                    if (userInput.startsWith("igraj-") && !this.isPlaying) {
                        String info[] = userInput.split("-");

                        opponent = this.server.getUser(info[1]);
                        opponent.opponent = this;

                        String msg = "[" + this.username + "]: Igraj sa mnom? da/ne";
                        this.opponent.sendMessage(msg);
                        this.opponent.hasInvite = true;
                    } else if (userInput.startsWith("posmatraj-") && !this.isPlaying) {
                        String[] info = userInput.split("-");
                        int gameID = Integer.parseInt(info[1]);

                        this.isWatching = true;
                        this.server.addWatcher(this, gameID);
                    } else if (userInput.equals("info")) {
                        skipFlag = true;
                        this.sendMessage(this.server.getStatusInfo(this));
                    } else if (userInput.equals("rezultati")) {
                        skipFlag = true;
                        this.sendMessage(this.server.getResults());
                    } else if (userInput.equals("da") && hasInvite) {
                        if (this.isWatching) {
                            this.server.removeWatcher(this);
                            this.isWatching = false;
                        }
                        this.symbol = "X";
                        this.opponent.symbol = "O";

                        this.isPlaying = true;
                        this.opponent.isPlaying = true;

                        this.game = new Game(this.server, this, this.opponent, this.server.allGames.size());
                        this.opponent.game = game;
                        this.server.allGames.put(this.game, new HashSet<>(Arrays.asList(this, this.opponent)));

                        this.sendMessage(this.game.getInfo());
                        this.opponent.sendMessage(this.game.getInfo());

                        String notification = this.username + " i " + this.opponent.username + " počinju igru!";
                        this.server.broadcastToAllFreePlayers(notification);
                        this.server.broadcastToAllFreePlayers(this.server.getStatusInfo(this));
                    } else if (userInput.equals("ne") && hasInvite) {
                        String msg = "[OBAVJEŠTENJE]: " + this.username + " je odbio poziv za igru.";
                        this.opponent.sendMessage(msg);
                    } else if (userInput.matches("[1-9]") && isPlaying) {
                        int positionIndex = Integer.parseInt(userInput);
                        skipFlag = true;

                        if (this.symbol.equals(this.game.getActivePlayerSymbol()) || (this.symbol.equals("X") && this.game.getMoveCount() == 0)) {
                            this.sendMessage("[UPOZORENJE]: Na redu je drugi igrač, sačekajte!");
                        } else if (this.game.isPositionAvailable(positionIndex)) {
                            userInput =  "[" + this.username + "]: Biram poziciju " + userInput;
                            this.server.broadcast(this, userInput);
                            this.game.placeSymbol(this, positionIndex);
                        } else {
                            this.sendMessage("[UPOZORENJE]: Pozicija je već markirana, pokušajte ponovo.");
                        }
                    }

                    if (this.isPlaying() && !skipFlag) {
                        String msg = "[" + this.username + "]: " + userInput;
                        this.server.broadcast(this, msg);
                    }
                } while (!userInput.equals("exit"));

                // Broadcast that user has disconnected
                this.server.broadcastToAll(this, this.username + " je napustio igru.");
            }
        } catch (IOException e) {
            System.out.println("Error in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (this.game != null) {
                this.opponent.resetStates();
                this.server.allGames.remove(this.game);
            }

            this.server.remove(this);

            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void sendMessage(String message) {
        if (this.toUser != null)
            this.toUser.println(message);
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getSymbol() {
        return symbol;
    }

    public Game getGame() {
        return game;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public void resetStates() {
        this.isPlaying = false;
        this.isWatching = false;
        this.hasInvite = false;
        this.symbol = null;
        this.opponent = null;
        this.game = null;
    }
}
