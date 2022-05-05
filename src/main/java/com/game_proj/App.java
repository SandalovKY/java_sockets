package com.game_proj;

import java.io.IOException;


public class App 
{
    public static void main( String[] args )
    {
        try
        {
            Thread gameServer = new Server(3333);
            gameServer.start();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
