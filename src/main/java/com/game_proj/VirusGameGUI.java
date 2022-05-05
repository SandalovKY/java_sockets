package com.game_proj;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;

class ConnectionManager extends Thread
{
    private Socket client;
    private DataOutputStream dos;
    private DataInputStream dis;
    private VirusGameGUI frame;

    public ConnectionManager(VirusGameGUI frame)
    {
        this.frame = frame;
        // Read configuration JSON file with information about connection
        FileReader reader = null;
        try
        {
            reader = new FileReader("info.json");
            System.out.println("-- Configuration was loaded --");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.exit(0);
        }

        if (reader != null)
        {
            String ipString = null;
            JsonObject obj = (JsonObject)JsonParser.parseReader(reader);
            JsonElement ipElem = obj.get("ip");
            if(ipElem != null)
            {
                ipString = ipElem.getAsString();
                System.out.println("Server IP: " + ipString);
            }
            else
            {
                System.out.println("Can't get server IP");
                System.exit(0);
            }
            Integer port = null;
            JsonElement portElem = obj.get("port");
            if(portElem != null)
            {
                port = portElem.getAsInt();
                System.out.println("Server port: " + port);
            }
            else
            {
                System.out.println("Can't get server port");
                System.exit(0);
            }

            if (ipString != null && port != null)
            // Create connection with server
            try
            {
                client = new Socket(ipString, port);
                System.out.println("-- Connection to server --");

                dos = new DataOutputStream(client.getOutputStream());
                dis = new DataInputStream(client.getInputStream());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
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
                            break;
                        case INIT_USER:
                            System.out.println("-- Successfully connected to server --");
                            JOptionPane.showMessageDialog(this.frame, "Successfully connected to server");
                            break;
                        case MOVE:
                            switch (FieldStatus.values()[obj.get("status").getAsInt()])
                            {
                                case ALIVE_O:
                                    frame.getBoard()[obj.get("y").getAsInt()][obj.get("x").getAsInt()].setText("O");
                                    break;
                                case ALIVE_X:
                                    frame.getBoard()[obj.get("y").getAsInt()][obj.get("x").getAsInt()].setText("X");
                                    break;
                                case DEAD_O:
                                    frame.getBoard()[obj.get("y").getAsInt()][obj.get("x").getAsInt()].setBackground(Color.GREEN);
                                    break;
                                case DEAD_X:
                                    frame.getBoard()[obj.get("y").getAsInt()][obj.get("x").getAsInt()].setBackground(Color.BLUE);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case START_GAME:
                            System.out.println("-- All players ready game started --");
                            JsonElement players = obj.get("players");
                            JsonArray array = players.getAsJsonArray();

                            String playersName = frame.getPlayer().getName();

                            array.forEach(item -> {
                                JsonObject objSearch = (JsonObject)item;
                                if (objSearch.get("name").getAsString().equals(playersName))
                                {
                                    frame.getPlayer().setNum(objSearch.get("num").getAsInt());
                                }
                            });
                            JOptionPane.showMessageDialog(this.frame, "All players connected, game started. Your move: " + frame.getPlayer().getNum());
                            break;
                        case CONNECTION_REJECTED:
                            System.out.println("-- Connection rejected --");
                            JOptionPane.showMessageDialog(this.frame, "Connection rejected");
                        default:
                            cntn = false;
                            break;
                            
                    }
                }
            }
            catch (IOException e) {}
        }
        try
        {
            this.dis.close();
            this.dos.close();
            this.client.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message)
    {
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

public class VirusGameGUI extends JFrame
{
    // private Game baseGame;
    private Player basePlayer;
    private Container pane;
    private JButton [][] board;
    private JMenuBar menuBar;
    private JMenu menu;
    private JMenuItem newGame;
    private JMenuItem endGame;

    private ConnectionManager connection;

    public Player getPlayer()
    {
        return this.basePlayer;
    }

    public JButton[][] getBoard()
    {
        return board;
    }

    private void addPlayer(Game baseGame)
    {
        String name = JOptionPane.showInputDialog(this, "Write player's name: ");
        System.out.println(name);
        this.basePlayer = new Player(baseGame, name);

        JsonObject jObj = new JsonObject();
        jObj.addProperty("idx", Messages.INIT_USER.ordinal());
        jObj.addProperty("name", name);
        this.connection.sendMessage(jObj.toString());
    }

    public VirusGameGUI(Game baseGame, Player basePlayer)
    {
        super();

        this.connection = new ConnectionManager(this);
        connection.start();
        this.pane = getContentPane();
        this.pane.setLayout(new GridLayout(10, 10));
        this.setTitle("Virus Vars");
        this.setSize(600, 600);
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);

        this.board = new JButton[10][10];
        this.initMenuBar();
        this.initBoard();
        this.addPlayer(baseGame);
    }

    private void initMenuBar()
    {
        this.menuBar = new JMenuBar();
        this.menu = new JMenu("File");
        this.newGame = new JMenuItem("New Game");
        this.newGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                resetBoard();
                JsonObject jObj = new JsonObject();
                jObj.addProperty("idx", Messages.START_GAME.ordinal());
                connection.sendMessage(jObj.toString());
            }
        });
        this.endGame = new JMenuItem("End Session");
        this.endGame.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                JsonObject jObj = new JsonObject();
                jObj.addProperty("idx", Messages.EXIT.ordinal());
                connection.sendMessage(jObj.toString());
                System.exit(0);
            }
        });
        this.menu.add(this.newGame);
        this.menu.add(this.endGame);
        this.menuBar.add(this.menu);
        setJMenuBar(menuBar);
    }
    private void resetBoard()
    {

    }
    private void initBoard()
    {
        for (int i = 0; i < 10; ++i)
        {
            for (int j = 0; j < 10; ++j)
            {
                int rowNum = i;
                int colNum = j;
                JButton btn = new JButton();
                board[i][j] = btn;
                btn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent event) {

                        JsonObject jObj = new JsonObject();
                        jObj.addProperty("idx", Messages.MOVE.ordinal());
                        jObj.addProperty("x", colNum);
                        jObj.addProperty("y", rowNum);
                        connection.sendMessage(jObj.toString());
                    }

                });
                pane.add(btn);
            }
        }
    }

}
