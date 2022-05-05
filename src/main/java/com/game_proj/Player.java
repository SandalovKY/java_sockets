package com.game_proj;

import java.util.Stack;

public class Player
{
    Stack<Move> moveList;
    Game game;
    String name;
    boolean readyToMove;
    boolean readyToPlay;
    int playerNum;


    public Player()
    {
        this.readyToPlay = false;
        this.playerNum = -1;
    }
    public Player(Game game, String name, int playerNum)
    {
        this();
        this.name = name;
        this.game = game;
        this.playerNum = playerNum;
    }
    public Player(Game game, String name)
    {
        this();
        this.name = name;
        this.game = game;
        this.playerNum = -1;
    }
    public void move(Move move)
    {
        this.game.move(this, move);
    }

    public String getName()
    {
        return this.name;
    }
    public int getNum()
    {
        return this.playerNum;
    }

    public void setNum(int num)
    {
        this.playerNum = num;
    }
    public void setReadyToPlay()
    {
        this.readyToPlay = true;
        this.game.startGame();
    }
    public boolean readyToPlay()
    {
        return this.readyToPlay;
    }

}
