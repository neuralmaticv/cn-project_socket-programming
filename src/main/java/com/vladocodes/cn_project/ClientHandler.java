package com.vladocodes.cn_project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Server server;
    private final Socket socket;
    private BufferedReader fromUser;
    private PrintWriter toUser;
    private String username;
    private boolean playing;
    private String symbol;
    private ClientHandler opponent;
    private Game game;

    public ClientHandler(Socket client, Server server) throws IOException {
        this.socket = client;
        this.server = server;
        this.playing = false;

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
                do {

                    userInput = fromUser.readLine();
                    if (userInput == null) {
                        break;
                    }

                    // analyze input
                    if (userInput.startsWith("igraj-")) {
                        String info[] = userInput.split("-");

                        opponent = this.server.getUser(info[1]);
                        opponent.opponent = this;
                        this.symbol = "X";
                        this.opponent.symbol = "O";

                        String msg = "[" + this.username + "]: Igraj sa mnom? da/ne";
                        this.opponent.sendMessage(msg);
                    }

                    if (userInput.startsWith("posmatraj-")) {
                        String info[] = userInput.split("-");
                        int gameID = Integer.parseInt(info[1]);
                        this.server.allGames.get(gameID).addWatcher(this);
                    }

                    if (userInput.equals("da")) {
                        this.playing = true;
                        this.opponent.playing = true;

                        this.game = new Game(this, this.opponent, this.server.allGames.size());
                        this.opponent.game = game;
                        this.server.allGames.put(this.server.allGames.size(), this.game);

                        this.sendMessage(this.game.getInfo());
                        this.opponent.sendMessage(this.game.getInfo());

                        String notification = this.username + " i " + this.opponent.username + " zapocinju igru!";
                        this.server.broadcastToAllFreePlayers(notification);
                        this.server.broadcastToAllFreePlayers("Igraci koji ne igraju: " + this.server.getFreeUsers(this));
                        this.server.broadcastToAllFreePlayers("Igraci koji igraju: " + this.server.getUsersPlaying());
                    } else if (userInput.equals("ne")) {
                        String msg = "[OBAVJESTENJE]:" + this.opponent.username + " je odbio poziv za igru.";
                        this.sendMessage(msg);
                    } else if (userInput.matches("[1-9]")) {
                        int positionIndex = Integer.parseInt(userInput);

                        if (this.symbol.equals(this.game.getActivePlayerSymbol())) {
                            this.sendMessage("[UPOZORENJE]: Na redu je drugi igrac!");
                        } else if (this.game.isPositionAvailable(positionIndex)) {
                            this.game.placeSymbol(this, positionIndex);
                        } else {
                            this.sendMessage("[UPOZORENJE]: Pozicija je vec markirana, pokusajte ponovo.");
                        }

                        userInput = "Biram poziciju " + userInput;
                    }

                    if (this.isPlaying()) {
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
        return playing;
    }

    String getUsername() {
        return this.username;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getGameID() {
        return this.game.getID();
    }

    public Server getServer() {
        return server;
    }
}
