package com.game_proj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


class Move
{
    public Move(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        return this.x == ((Move)obj).x && this.y == ((Move)obj).y;
    }

    int x;
    int y;
}

class Cell
{
    public Cell(FieldStatus stat)
    {
        this.status = stat;
        this.set = null;
        isActive = false;
    }
    public FieldStatus getStatus()
    {
        return this.status;
    }
    public void setStatus(FieldStatus status)
    {
        this.status = status;
    }
    public void addToSet(DeadSet set)
    {
        this.set = set;
    }
    public boolean getActiveStatus()
    {
        return isActive;
    }
    public void setActiveStatus(boolean status)
    {
        isActive = status;
    }
    public DeadSet getSet()
    {
        return set;
    }

    FieldStatus status;
    DeadSet set;
    boolean isActive;
}

class DeadSet
{
    public DeadSet(FieldStatus status)
    {
        this.status = status;
        deadList = new ArrayList<>();
    }
    public void addCell(Cell cell)
    {
        deadList.add(cell);
        cell.addToSet(this);
    }
    public void reduceActiveCells() throws RuntimeException
    {
        --numActiveCells;
        if (numActiveCells < 0)
        {
            throw new RuntimeException("Invalid active cells count");
        }
    }
    public void addActiveCell()
    {
        ++numActiveCells;
    }
    public FieldStatus getStatus() {
        return status;
    }
    public boolean isSetActive() {
        return numActiveCells > 0;
    }
    public List<Cell> getCells() {
        return deadList;
    }
    public int getNumOfActiveItems() {
        return numActiveCells;
    }
    public void mergeSet(DeadSet other) {
        for (Cell el: other.getCells())
        {
            deadList.add(el);
            el.addToSet(this);
        }
        this.numActiveCells += other.getNumOfActiveItems();
        other.getCells().clear();
    }
    List<Cell> deadList;
    int numActiveCells;
    FieldStatus status;
}

public class Game
{
    private static int playersCount = 2;

    private static int firstPlayer = 1;
    private static int secondPlayer = 2;

    private static int numAddSteps = 2;

    private static HashMap<Integer, FieldStatus> playerToAliveFieldMap = new HashMap<>();
    static
    {
        playerToAliveFieldMap.put(firstPlayer, FieldStatus.ALIVE_X);
        playerToAliveFieldMap.put(secondPlayer, FieldStatus.ALIVE_O);
    }

    private static HashMap<Integer, FieldStatus> playerToDeadFieldMap = new HashMap<>();
    static
    {
        playerToDeadFieldMap.put(firstPlayer, FieldStatus.DEAD_O);
        playerToDeadFieldMap.put(secondPlayer, FieldStatus.DEAD_X);
    }

    private static Move firstPlayerStartPos = new Move(0, 0);
    private static Move secondPlayerStartPos = new Move(9, 9);

    private HashMap<String, Player> playersMap;
    private SimpleSubject subjPlayers;
    private boolean gameStarted;
    private int currMove;
    private Cell [][] field;

    private int currStep;

    public Game()
    {
        currStep = 0;
        currMove = -1;
        gameStarted = false;
        subjPlayers = null;
        playersMap = new HashMap<>();
        field = new Cell[10][10];
        for (int i = 0; i < 10; ++i)
        for (int j = 0; j < 10; ++j)
        {
            field[i][j] = new Cell(FieldStatus.EMPTY);
        }
    }
    public Game(SimpleSubject subj)
    {
        this();
        subjPlayers = subj;
    }
    public Player addPlayer(String name)
    {
        Player playerObj = null;
        synchronized (this)
        {
            if (!playersMap.containsKey(name) && playersMap.size() < playersCount)
            {
                playerObj = new Player(this, name, playersMap.size() + 1);
                playersMap.put(name, playerObj);
            }
        }
        return playerObj;
    }

    private void updateActivePlayer()
    {
        if (currStep < numAddSteps)
        {
            ++currStep;
        }
        else
        {
            if (this.currMove == firstPlayer) this.currMove = secondPlayer;
            else this.currMove = firstPlayer;
            currStep = 0;
        }
    }

    public boolean removePlayer(String name)
    {
        boolean playerWasDeleted = false;
        synchronized (this)
        {
            if (playersMap.containsKey(name))
            {
                playersMap.remove(name);
                playerWasDeleted = true;
            }
        }
        return playerWasDeleted;
    }

    private boolean findAliveNeighbour(int x, int y, int currPlayer)
    {
        boolean nFound = false;
        for (int i = x - 1; i <= x + 1; ++i)
        for (int j = y - 1; j <= y + 1; ++j)
        {
            if (i >= 0 && i < 10 &&
                j >= 0 && j < 10 &&
                !(i == x && j == y))
            {
                if (field[j][i].getStatus() == playerToAliveFieldMap.get(currPlayer)) nFound = true;
            }
        }
        return nFound;
    }

    private boolean findActiveDeadCells(int x, int y, int currPlayer)
    {
        FieldStatus status = playerToDeadFieldMap.get(currPlayer);
        boolean nFound = false;
        for (int i = x - 1; i <= x + 1; ++i)
        for (int j = y - 1; j <= y + 1; ++j)
        {
            if (i >= 0 && i < 10 &&
                j >= 0 && j < 10 &&
                !(i == x && j == y))
            {
                DeadSet set = field[j][i].getSet();
                if (set != null && set.getStatus() == status)
                {
                    nFound = set.isSetActive();
                    if (nFound) break;
                }
            }
        }
        return nFound;
    }

    private int getEnemyNum(int currPlayerNum)
    {
        if (currPlayerNum == firstPlayer) return secondPlayer;
        else return firstPlayer;
    }

    private void setCellToDead(int x, int y, int playerNum)
    {
        FieldStatus status = playerToDeadFieldMap.get(playerNum);
        field[y][x].setStatus(status);

        // Validate enemy sets
        int enemyNum = getEnemyNum(playerNum);
        FieldStatus enemyStatus = playerToDeadFieldMap.get(enemyNum);
        for (int i = x - 1; i <= x + 1; ++i)
        for (int j = y - 1; j <= y + 1; ++j)
        {
            if (i >= 0 && i < 10 &&
                j >= 0 && j < 10 &&
                !(i == x && j == y))
            {
                DeadSet nbrSet = field[j][i].getSet();
                if (nbrSet != null && nbrSet.getStatus() == enemyStatus)
                {
                    if (!findAliveNeighbour(i, j, enemyNum))
                    {
                        nbrSet.reduceActiveCells();
                        field[j][i].setActiveStatus(false);
                    }
                }
            }
        }

        DeadSet set = null;
        for (int i = x - 1; i <= x + 1; ++i)
        for (int j = y - 1; j <= y + 1; ++j)
        {
            if (i >= 0 && i < 10 &&
                j >= 0 && j < 10 &&
                !(i == x && j == y))
            {
                if (set == null)
                {
                    set = field[j][i].getSet();
                    if (set != null && set.getStatus() != status) set = null;
                }
                else
                {
                    DeadSet otherSet = field[j][i].getSet();
                    if (otherSet != null && otherSet.getStatus() == status && otherSet != set)
                    {
                        set.mergeSet(otherSet);
                    }
                }
            }
        }
        if (set != null)
        {
            set.addCell(field[y][x]);
        }
        else
        {
            set = new DeadSet(status);
            set.addCell(field[y][x]);
        }
        if (findAliveNeighbour(x, y, playerNum))
        {
            set.addActiveCell();
            field[y][x].setActiveStatus(true);
        }

    }

    private boolean onPlayersStartPosition(int playerOrder, Move move)
    {
        if (move == null) return false;
        return playerOrder == firstPlayer && move.equals(firstPlayerStartPos) ||
            playerOrder == secondPlayer && move.equals(secondPlayerStartPos);
    }

    private boolean actionOnAliveCell(Move move, int currPlayerOrder, JsonObject jObj)
    {
        boolean moveWasMade = false;
        Cell currCell = field[move.y][move.x];
        FieldStatus status = playerToDeadFieldMap.get(currPlayerOrder);
        if (findAliveNeighbour(move.x, move.y, currPlayerOrder) || findActiveDeadCells(move.x, move.y, currPlayerOrder))
        {
            setCellToDead(move.x, move.y, currPlayerOrder);
            currCell.setStatus(status);
            jObj.addProperty("status", status.ordinal());
            moveWasMade = true;
        }
        return moveWasMade;
    }

    public void updateNearDeadCells(int x, int y, int playerNum)
    {
        FieldStatus status = playerToDeadFieldMap.get(playerNum);
        for (int i = x - 1; i <= x + 1; ++i)
        for (int j = y - 1; j <= y + 1; ++j)
        {
            if (i >= 0 && i < 10 &&
                j >= 0 && j < 10 &&
                !(i == x && j == y))
            {
                DeadSet set = field[j][i].getSet();
                if (set != null && set.getStatus() == status &&
                    !field[j][i].getActiveStatus())
                {
                    field[j][i].setActiveStatus(true);
                    set.addActiveCell();
                }
            }
        }
    }

    public boolean move(Player player, Move move)
    {
        if (gameStarted && playersMap.containsKey(player.name))
        {
            int currPlayerOrder = player.getNum();
            if (currMove == currPlayerOrder &&
                move.x >= 0 && move.x < 10 &&
                move.y >= 0 && move.y < 10)
            {
                Boolean moveWasMade = false;

                JsonObject jObj = new JsonObject();
                jObj.addProperty("idx", Messages.MOVE.ordinal());
                jObj.addProperty("x", move.x);
                jObj.addProperty("y", move.y);

                Cell currCell = field[move.y][move.x];

                switch (currCell.getStatus())
                {
                    case EMPTY:
                        FieldStatus status = playerToAliveFieldMap.get(currPlayerOrder);

                        if (onPlayersStartPosition(currPlayerOrder, move) ||
                            findAliveNeighbour(move.x, move.y, currPlayerOrder) ||
                            findActiveDeadCells(move.x, move.y, currPlayerOrder))
                        {
                            updateNearDeadCells(move.x, move.y, currPlayerOrder);
                            currCell.setStatus(status);
                            jObj.addProperty("status", status.ordinal());
                            moveWasMade = true;
                        }
                        break;
                    case ALIVE_X:
                        if (currPlayerOrder == secondPlayer)
                        {
                            moveWasMade = actionOnAliveCell(move, currPlayerOrder, jObj);
                        }
                        break;
                    case ALIVE_O:
                        if (currPlayerOrder == firstPlayer)
                        {
                            moveWasMade = actionOnAliveCell(move, currPlayerOrder, jObj);
                        }
                        break;
                    case DEAD_X:
                        break;
                    case DEAD_O:
                        break;
                    default:
                        return false;
                }
                if (moveWasMade)
                {
                    updateActivePlayer();
                    subjPlayers.setVal(jObj.toString());
                }
                return moveWasMade;
            }
        }
        return false;
    }
    public void startGame()
    {
        synchronized (this)
        {
            if (!gameStarted && playersMap.size() == 2)
            {
                gameStarted = true;
                for(Player player: playersMap.values())
                {
                    if (!player.readyToPlay())
                    {
                        gameStarted = false;
                    }
                }
                if (gameStarted)
                {
                    int cnt = 1;
                    System.out.println("GAME STARTED");
                    JsonObject messageObj = new JsonObject();
                    messageObj.addProperty("idx", Messages.START_GAME.ordinal());
                    JsonArray arr = new JsonArray();
                    for (Player player: playersMap.values())
                    {
                        player.setNum(cnt++);
                        JsonObject obj = new JsonObject();
                        obj.addProperty("name", player.getName());
                        obj.addProperty("num", player.getNum());
                        arr.add(obj);
                    }
                    messageObj.add("players", arr);
                    if (subjPlayers != null)
                        subjPlayers.setVal(messageObj.toString());
                    currMove = 1;
                }
            }
        }
    }
}