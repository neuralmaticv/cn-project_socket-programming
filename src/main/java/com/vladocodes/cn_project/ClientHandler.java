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
    private BufferedReader fromUser;
    private PrintWriter toUser;
    private String username;
    private boolean isPlaying;
    private boolean hasInvite;
    private String symbol;
    private ClientHandler opponent;
    private Game game;

    public ClientHandler(Socket client, Server server) throws IOException {
        this.socket = client;
        this.server = server;
        this.isPlaying = false;
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
                toUser.println("Username is used");
            } else {
                toUser.println("OK");
                this.server.users.add(this);
                this.sendMessage("Igraci koji ne igraju: " + this.server.getFreeUsers(this));
                this.sendMessage("Igraci koji igraju: " + this.server.getUsersPlaying());
                this.server.broadcastToAll(this, "[OBAVJESTENJE]: ukljucio se novi igrac " + this.username);

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
                    if (userInput.startsWith("igraj-")) {
                        String info[] = userInput.split("-");

                        opponent = this.server.getUser(info[1]);
                        opponent.opponent = this;

                        String msg = "[" + this.username + "]: Igraj sa mnom? da/ne";
                        this.opponent.sendMessage(msg);
                        this.opponent.hasInvite = true;
                    }

                    if (userInput.startsWith("posmatraj-")) {
                        String info[] = userInput.split("-");
                        int gameID = Integer.parseInt(info[1]);
                        for (Game g: this.server.allGames.keySet()) {
                            if (g.getID() == gameID) {
                                this.server.allGames.get(g).add(this);
                                g.addWatcher(this);
                            }
                        }
                    }

                    if (userInput.equals("da") && hasInvite) {
                        this.symbol = "X";
                        this.opponent.symbol = "O";

                        this.isPlaying = true;
                        this.opponent.isPlaying = true;

                        this.game = new Game(this, this.opponent, this.server.allGames.size());
                        this.opponent.game = game;
                        this.server.allGames.put(this.game, new HashSet<>(Arrays.asList(this, this.opponent)));

                        this.sendMessage(this.game.getInfo());
                        this.opponent.sendMessage(this.game.getInfo());

                        String notification = this.username + " i " + this.opponent.username + " zapocinju igru!";
                        this.server.broadcastToAllFreePlayers(notification);
                        this.server.broadcastToAllFreePlayers("Igraci koji ne igraju: " + this.server.getFreeUsers(this));
                        this.server.broadcastToAllFreePlayers("Igraci koji igraju: " + this.server.getUsersPlaying());
                    } else if (userInput.equals("ne") && hasInvite) {
                        String msg = "[OBAVJESTENJE]:" + this.username + " je odbio poziv za igru.";
                        this.opponent.sendMessage(msg);
                    } else if (userInput.matches("[1-9]") && isPlaying) {
                        int positionIndex = Integer.parseInt(userInput);
                        skipFlag = true;

                        if (this.symbol.equals(this.game.getActivePlayerSymbol()) || (this.symbol.equals("X") && this.game.getMoveCount() == 0)) {
                            this.sendMessage("[UPOZORENJE]: Na redu je drugi igrac!");
                        } else if (this.game.isPositionAvailable(positionIndex)) {
                            userInput =  "[" + this.username + "]: Biram poziciju " + userInput;
                            this.server.broadcast(this, userInput);
                            this.game.placeSymbol(this, positionIndex);
                        } else {
                            this.sendMessage("[UPOZORENJE]: Pozicija je vec markirana, pokusajte ponovo.");
                        }
                    }

                    if (this.isPlaying() && !skipFlag) {
                        String msg = "[" + this.username + "]:" + userInput;
                        this.server.broadcast(this, msg);
                    }
                } while (!userInput.equals("exit"));

                // Broadcast that user has disconnected
                this.server.broadcastToAll(this, this.username + " has left the game.");
            }
        } catch (IOException e) {
            System.out.println("Error in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (this.game != null) {
                this.server.allGames.remove(this.getGameID());
                this.opponent.game = null;
                this.opponent.isPlaying = false;
            }
            this.server.remove(this);
            try {
                this.socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void sendMessage(String message) {
        if (this.toUser != null)
            this.toUser.println(message);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    String getUsername() {
        return this.username;
    }

    public String getSymbol() {
        return symbol;
    }

    public Game getGame() {
        return game;
    }

    public int getGameID() {
        return this.game.getID();
    }

    public Server getServer() {
        return server;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}
