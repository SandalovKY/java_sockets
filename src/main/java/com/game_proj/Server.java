package com.game_proj;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread
{
    private ServerSocket sSocket;
    private Game game;
    private SimpleSubject subj;

    public Server(int port) throws IOException
    {
        subj = new SimpleSubject();
        game = new Game(subj);
        sSocket = new ServerSocket(port);
    }

    public void run()
    {
        while(true)
        {
            try
            {
                System.out.println("Ожидание подключения клиента к порту: " + sSocket.getLocalPort());
                Socket server = sSocket.accept();
                System.out.println("Произошло подключение к: " + server.getRemoteSocketAddress());
                DataInputStream diStream = new DataInputStream(server.getInputStream());
                DataOutputStream doStream = new DataOutputStream(server.getOutputStream());
                ClientHandler client = new ClientHandler(server, diStream, doStream, game);
                subj.registerObserver(client);
                client.start();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
}
