package com.vladocodes.cn_project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Game {
    private ClientHandler player1, player2;
    private Set<ClientHandler> viewers;
    private int moveCount = 0;
    private String[][] board = new String[3][3];

    public Game(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.viewers = new HashSet<>();
        this.viewers.add(player1);
        this.viewers.add(player2);

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = "-";
            }
        }
    }

    public void placeSymbol(ClientHandler player, int position) {
        switch (position) {
            case 1:
                board[0][0] = player.getSymbol();
                break;
            case 2:
                board[0][1] = player.getSymbol();
                break;
            case 3:
                board[0][2] = player.getSymbol();
                break;
            case 4:
                board[1][0] = player.getSymbol();
                break;
            case 5:
                board[1][1] = player.getSymbol();
                break;
            case 6:
                board[1][2] = player.getSymbol();
                break;
            case 7:
                board[2][0] = player.getSymbol();
                break;
            case 8:
                board[2][1] = player.getSymbol();
                break;
            case 9:
                board[2][2] = player.getSymbol();
                break;
        }

        moveCount++;
        displayBoard();

        if (moveCount >= 5) {
            checkWin();
        }
    }

    private void checkWin() {
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
                this.player1.sendMessage("Igrac sa simbolom X je pobijedio");
            } else if (countO == 3) {
                this.player1.sendMessage("Igrac sa simbolom O je pobijedio");
            }
        }
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

        for (ClientHandler v : viewers) {
            v.sendMessage(sb.toString());
        }
    }

    public void addViewer(ClientHandler user) {
        this.viewers.add(user);
    }

    public ArrayList<ClientHandler> getPlayers() {
        ArrayList<ClientHandler> list = new ArrayList<>();
        list.add(player1);
        list.add(player2);

        return list;
    }
}
