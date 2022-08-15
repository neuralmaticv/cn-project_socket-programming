package com.vladocodes.cn_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Game {
    private final Server server;
    private final ClientHandler player1;
    private final ClientHandler player2;
    private String activePlayerSymbol;
    private final Set<ClientHandler> watchers;
    private int moveCount = 0;
    private final String[][] board = new String[3][3];
    private final Set<Integer> markedPositions;
    private final int id;
    private String winnerName;
    private boolean endOfGame = false;

    public Game(Server server, ClientHandler player1, ClientHandler player2, int ID) {
        this.server = server;
        this.player1 = player1;
        this.player2 = player2;
        this.watchers = new HashSet<>();
        this.watchers.add(player1);
        this.watchers.add(player2);
        this.id = ID;
        this.markedPositions = Collections.synchronizedSet(new HashSet<>());

        for (String[] strings : board) {
            Arrays.fill(strings, "-");
        }
    }

    public void placeSymbol(ClientHandler player, int position) {
        activePlayerSymbol = player.getSymbol();

        switch (position) {
            case 1 -> board[0][0] = activePlayerSymbol;
            case 2 -> board[0][1] = activePlayerSymbol;
            case 3 -> board[0][2] = activePlayerSymbol;
            case 4 -> board[1][0] = activePlayerSymbol;
            case 5 -> board[1][1] = activePlayerSymbol;
            case 6 -> board[1][2] = activePlayerSymbol;
            case 7 -> board[2][0] = activePlayerSymbol;
            case 8 -> board[2][1] = activePlayerSymbol;
            case 9 -> board[2][2] = activePlayerSymbol;
        }

        markedPositions.add(position);
        displayBoard();
        moveCount++;

        String message = "";

        if (moveCount >= 5 && checkWin()) {
            message = "[OBAVJEŠTENJE]: Pobijednik je "  + this.getWinnerName();
        } else if (moveCount == 9 && !checkWin()) {
            message = "Neriješeno!";
            endOfGame = true;
        }

        if (this.isEndOfGame()) {
            player1.resetStates();
            player2.resetStates();

            for (ClientHandler v : watchers) {
                v.sendMessage(message);
            }

            this.recordResult(player1.getUsername(), player2.getUsername(), winnerName);
            this.server.allGames.remove(this);
        }
    }

    private boolean checkWin() {
        int[][][] winningCombinations = {
                {
                        {0, 0}, {0, 1}, {0, 2}
                },
                {
                        {1, 0}, {1, 1}, {1, 2}
                },
                {
                        {2, 0}, {2, 1}, {2, 2}
                },
                {
                        {0, 0}, {1, 0}, {2, 0}
                },
                {
                        {0, 1}, {1, 1}, {2, 1}
                },
                {
                        {0, 2}, {1, 2}, {2, 2}
                },
                {
                        {0, 0}, {1, 1}, {2, 2}
                },
                {
                        {0, 2}, {1, 1}, {2, 0}
                }
        };

        for (int n = 0; n < winningCombinations.length; n++) {
            int countX = 0;
            int countO = 0;

            for (int i = 0; i < winningCombinations[n].length; i++) {
                if (board[winningCombinations[n][i][0]][winningCombinations[n][i][1]].equals("X")) {
                    countX++;
                } else if (board[winningCombinations[n][i][0]][winningCombinations[n][i][1]].equals("O")) {
                    countO++;
                }
            }

            if (countX == 3) {
                winnerName = this.player1.getSymbol().equals("X") ? this.player1.getUsername() : this.player2.getUsername();
                endOfGame = true;
                return true;
            } else if (countO == 3) {
                winnerName = this.player1.getSymbol().equals("O") ? this.player1.getUsername() : this.player2.getUsername();
                endOfGame = true;
                return true;
            }
        }

        return false;
    }

    public void displayBoard() {
        StringBuilder sb = new StringBuilder();
        for (String[] strings : board) {
            sb.append("| ");

            for (String string : strings) {
                sb.append(string).append(" | ");
            }
            sb.append("\n");
        }
        sb.append("\t\t\t");

        for (ClientHandler v : watchers) {
            sb.setLength(sb.length() - 1);
            v.sendMessage(sb.toString());
        }
    }

    public void addWatcher(ClientHandler user) {
        this.watchers.add(user);
    }

    public void removeWatcher(ClientHandler user) {
        this.watchers.remove(user);
    }

    public Set<ClientHandler> getWatchers() {
        return watchers;
    }

    public boolean isPositionAvailable(int index) {
        return !markedPositions.contains(index);
    }

    public boolean isEndOfGame() {
        return endOfGame;
    }

    public int getID() {
        return id;
    }

    public Set<ClientHandler> getPlayers() {
        Set<ClientHandler> set = new HashSet<>();
        set.add(player1);
        set.add(player2);
        
        return set;
    }

    public String getActivePlayerSymbol() {
        return activePlayerSymbol;
    }

    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(">>> POČINJE IGRA <<<").append("\n");
        sb.append("Upute i pravila:").append("\n");
        sb.append("1. Poziciju za znak birate iz opsega [1-9];").append("\n");
        sb.append("2. Pobjednik je onaj koji prvi spoji tri znaka vodoravno, uspravno ili dijagonalno.").append("\n");
        sb.append(">>>    SREĆNO    <<<").append("\n");
        sb.append("\t\t\t");

        return sb.toString();
    }

    private String getWinnerName() {
        return winnerName;
    }

    public int getMoveCount() {
        return moveCount;
    }

    private void recordResult(String player1, String player2, String result) {
        try {
            Path filePath = Paths.get("src/main/java/com/vladocodes/cn_project/results.txt");

            if (result == null) {
                result = "neriješeno";
            } else {
                result = "pobijednik je " + result;
            }

            String record = player1 + " protiv " + player2 + " => ishod: " + result + ";\n";
            Files.writeString(filePath, record, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
