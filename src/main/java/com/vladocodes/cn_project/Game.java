package com.vladocodes.cn_project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Game {
    private ClientHandler player1, player2;
    private String activePlayerSymbol;
    private Set<ClientHandler> watchers;
    private int moveCount = 0;
    private String[][] board = new String[3][3];
    private Set<Integer> markedPositions;
    private int id;

    public Game(ClientHandler player1, ClientHandler player2, int ID) {
        this.player1 = player1;
        this.player2 = player2;
        this.watchers = new HashSet<>();
        this.watchers.add(player1);
        this.watchers.add(player2);
        this.id = ID;
        this.markedPositions = new HashSet<>();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = "-";
            }
        }
    }

    public void placeSymbol(ClientHandler player, int position) {
        activePlayerSymbol = player.getSymbol();

        switch (position) {
            case 1:
                board[0][0] = activePlayerSymbol;
                break;
            case 2:
                board[0][1] = activePlayerSymbol;
                break;
            case 3:
                board[0][2] = activePlayerSymbol;
                break;
            case 4:
                board[1][0] = activePlayerSymbol;
                break;
            case 5:
                board[1][1] = activePlayerSymbol;
                break;
            case 6:
                board[1][2] = activePlayerSymbol;
                break;
            case 7:
                board[2][0] = activePlayerSymbol;
                break;
            case 8:
                board[2][1] = activePlayerSymbol;
                break;
            case 9:
                board[2][2] = activePlayerSymbol;
                break;
        }

        markedPositions.add(position);
        displayBoard();
        moveCount++;

        if (moveCount >= 5 && checkWin()) {
            ;
        } else if (moveCount == 9 && !checkWin()) {
            this.player1.getServer().broadcast(player1, "[OBAVJESTENJE]: Nerijeseno!");
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
                String playerName = this.player1.getSymbol().equals("X") ? this.player1.getUsername() : this.player2.getUsername();
                this.player1.getServer().broadcast(player1, "[OBAVJESTENJE]: pobijedio je " + playerName);
                return true;
            } else if (countO == 3) {
                String playerName = this.player1.getSymbol().equals("O") ? this.player1.getUsername() : this.player2.getUsername();
                this.player1.getServer().broadcast(player1, "[OBAVJESTENJE]: pobijedio je " + playerName);
                return true;
            }
        }

        return false;
    }

    public void displayBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            sb.append("| ");
            for (int j = 0; j < board[i].length; j++) {
                sb.append(board[i][j]).append(" | ");
            }
            sb.append("\n");
        }

        for (ClientHandler v : watchers) {
            v.sendMessage(sb.toString());
        }
    }

    public void addWatcher(ClientHandler user) {
        this.watchers.add(user);
    }

    public ArrayList<ClientHandler> getPlayers() {
        ArrayList<ClientHandler> list = new ArrayList<>();
        list.add(player1);
        list.add(player2);

        return list;
    }

    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(">>> POCINJE IGRA <<<").append("\n");
        sb.append("Upute i pravila:").append("\n");
        sb.append("1. Poziciju za znak birate iz opsega [1-9];").append("\n");
        sb.append("2. Pobjednik je onaj koji prvi spoji tri znaka vodoravno, uspravno ili dijagonalno.").append("\n");
        sb.append(">>>    SRECNO    <<<").append("\n");

        return sb.toString();
    }

    public int getID() {
        return id;
    }

    public String getActivePlayerSymbol() {
        return activePlayerSymbol;
    }

    public boolean isPositionAvailable(int index) {
        return !markedPositions.contains(index);
    }
}
