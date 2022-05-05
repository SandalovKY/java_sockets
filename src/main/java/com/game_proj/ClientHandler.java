package com.game_proj;

import java.io.DataOutputStream;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Messages interface
// enum Messages
// {
//     EXIT,
//     INIT_USER,
//     MOVE,
//     START_GAME,
//     CONNECTION_REJECTED
// }

public class ClientHandler extends Thread implements Observer{
    final DataInputStream dis;
    final DataOutputStream dos;
    final Socket s;
    final Game game;
    boolean move;
    JsonObject localObj;

    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, Game game)
    {
        this.dis = dis;
        this.dos = dos;
        this.s = s;
        this.game = game;

        this.move = false;
        this.localObj = new JsonObject();
    }

    @Override
    public void run()
    {
        Player player = null;
        boolean cntn = true;
        while(cntn)
        {
            try
            {
                String received = dis.readUTF();
                JsonObject obj = (JsonObject)JsonParser.parseString(received);
                System.out.println("Message: " + received);
                if(obj != null)
                {
                    JsonElement idxElem = obj.get("idx");
                    Messages idxNum = Messages.values()[idxElem.getAsInt()];

                    switch (idxNum)
                    {
                        case EXIT:
                            System.out.println("Client " + this.s + " sends exit...");
                            if (player != null)
                            {
                                this.game.removePlayer(player.name);
                                System.out.println("Player was disconnected");
                            }
                            this.s.close();
                            System.out.println("Connection closed");
                            break;
                        case INIT_USER:
                            String playerName = obj.get("name").getAsString();
                            System.out.println("Player " + playerName + " connected");
                            player = game.addPlayer(playerName);
                            JsonObject jObj = new JsonObject();
                            int messageIdx = Messages.EXIT.ordinal();
                            if (player != null)
                            {
                                messageIdx = Messages.INIT_USER.ordinal();
                            }
                            else
                            {
                                messageIdx = Messages.CONNECTION_REJECTED.ordinal();
                            }
                            jObj.addProperty("idx", messageIdx);
                            dos.writeUTF(jObj.toString());
                            break;
                        case MOVE:
                            Move mv = new Move(obj.get("x").getAsInt(), obj.get("y").getAsInt());
                            player.move(mv);
                            break;
                        case START_GAME:
                            if (player != null)
                            {
                                player.setReadyToPlay();
                            }
                            break;
                        default:
                            this.s.close();
                            cntn = false;
                            break;
                            
                    }
                }
            }
            catch(IOException e)
            {}
        }
        try
        {
            this.dis.close();
            this.dos.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void update(String message) {
        try
        {
            dos.writeUTF(message);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
