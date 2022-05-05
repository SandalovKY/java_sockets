package com.game_proj;

import javax.swing.SwingUtilities;

// Messages interface
enum Messages
{
    EXIT,
    INIT_USER,
    MOVE,
    START_GAME,
    CONNECTION_REJECTED
}

enum FieldStatus
{
    EMPTY,
    ALIVE_X,
    ALIVE_O,
    DEAD_X,
    DEAD_O
}

public class Client {

    public static void main(String[] args) {
        Player player = null;
        Game game = new Game();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                new VirusGameGUI(game, player);
            }
            
        });
    }
}
