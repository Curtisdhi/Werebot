package org.Werebot;

import org.jibble.pircbot.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class WereBot  extends PircBot {
    private Main main;
    private WereBotTimer wbtimer;

    protected boolean GameStarted = false,GameRunning = false, Day = false;
    protected List<Players> players = new ArrayList<Players>();
    protected Roles role = null;
    protected Votes vote = null;

    protected final int STARTSECONDS = 30;
    protected final int DAYSECONDS = 30;
    protected final int MINPLAYERS = 3;

    protected User me;

    protected int round = 0;
    //overriding PircBot
    public WereBot(Main main) {
        this.main = main;
        this.setName(main.NICK);
    }

    protected  void onConnect() {
        setAutoNickChange(true);
        if (main.IDENTIFY_PASS != "") {
            sendMessage("NICKSERV", "Identify "+ main.IDENTIFY_PASS);
        }
    }

    protected  void  onMessage(String channel, String sender, 
    String login, String hostname, String message) {
        if (main.CHAN.equalsIgnoreCase(channel)) {
            String[] Token = message.split(" ");
            if (Token[0].equalsIgnoreCase("!start") && !this.GameStarted) {
                this.GameStarted = true;
                Players p = new Players(main,this);
                p.setNick(sender);
                this.players.add(p);
                sendMessage(main.CHAN,sender +" has started a new game!");
                sendMessage(main.CHAN, "The game will start in "+ STARTSECONDS +" seconds!");
                wbtimer = new WereBotTimer(this,1000);
                wbtimer.start();
            }
            else  if (Token[0].equalsIgnoreCase("!join") && this.GameStarted && !this.GameRunning) {
                if (!isPlayerInGame(sender)) {
                    Players p = new Players(main,this);                
                    p.setNick(sender);
                    this.players.add(p);
                    sendMessage(main.CHAN,sender +" has Joined");
                }
                else { 
                    sendMessage(main.CHAN,sender +", you're already in the game.");
                }
            }   
            else if (Token[0].equalsIgnoreCase("!leave") && this.GameStarted) {
                if (this.isPlayerInGame(sender)) {
                    for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
                        Players player = iter.next();
                        if (player.getNick().equalsIgnoreCase(sender)) { 
                            if (this.GameRunning) { player.kill(); }
                            else { iter.remove(); }
                            sendMessage(main.CHAN,sender +" has Left");
                            break;
                        }
                    }
                }
                else { 
                    sendMessage(main.CHAN,sender +", you aren't in the game.");
                }
                this.enoughPlayers();
            }
            else if (Token[0].equalsIgnoreCase("!vote") 
            && this.GameStarted 
            && this.GameRunning 
            && Token.length > 1
            && Day) {
                if (this.isPlayerInGame(sender)) {
                    if (this.isPlayerInGame(Token[1])) {
                        Players PVoter = this.getPlayersObject(sender), PVotee = this.getPlayersObject(Token[1]);
                        if (PVoter.isAlive()&& PVotee.isAlive()) {
                            vote.setVote(PVoter, PVotee);
                            sendMessage(main.CHAN, sender +" just voted for: "+ Token[1]);
                        }
                    }
                    else if (Token[1].equals("0")) { sendMessage(main.CHAN, sender +" votes noone"); }
                }   
            }
            else if (Token[0].equalsIgnoreCase("!alive") && this.GameStarted) {
                sendMessage(main.CHAN,"Alive Players: "+ this.getPlayerList(true));
            }   
            else if (Token[0].equalsIgnoreCase("!dead") && this.GameStarted) {
                sendMessage(main.CHAN,"Dead Players: "+ this.getPlayerList(false));
            } 
            else if (Token[0].equalsIgnoreCase("!list") && this.GameStarted) {
                sendMessage(main.CHAN,"All Players: "+ this.getPlayerList());
            }             
        }
    }

    /**
     * change the nick in the arraylist if player changed his nick 
     */
    protected void onNickChange(String oldNick, String login,
    String hostname, String newNick) {
        if (this.GameStarted && newNick != getNick()) {
            for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
                Players  player = iter.next();
                if (oldNick.equalsIgnoreCase(player.getNick())) {
                    player.setNick(newNick);
                    break;
                }
            }
        }
        else if (newNick == getNick()) {
            me = this.getIRCUser(main.CHAN, getNick());
        }
    }

    /**
     * this "kills" the player onQuit or onPart or onKick
     * also if user rejoins back within the round he will be revived :D
     */
    protected void onQuit(String sourceNick,String sourceLogin,
    String sourceHostname,String reason) {
        if (sourceNick.equalsIgnoreCase(getNick())) { 
            if (this.GameStarted) { this.gameEnd(); }
        }
        else if (this.GameStarted && this.isPlayerInGame(sourceNick)) {
            sendMessage(main.CHAN,sourceNick +" has fled, but the wolf caught him and was killed");
            if (this.GameRunning) {
                sendRole(this.getPlayersObject(sourceNick));
                this.getPlayersObject(sourceNick).kill(this.round);
            }
            else {
                this.destroyPlayer(this.getPlayersObject(sourceNick));
            }
            this.enoughPlayers();
        }
    }

    protected void onPart(String channel, String sender, String login, String hostname) {
        if (sender.equalsIgnoreCase(getNick())) { 
            if (this.GameStarted) { this.gameEnd(); }
        }
        else if (this.GameStarted && this.isPlayerInGame(sender)) {
            sendMessage(main.CHAN,sender +" has fled, but the wolf caught him and was killed");
            if (this.GameRunning) {
                sendRole(this.getPlayersObject(sender)); 
                this.getPlayersObject(sender).kill(this.round);
            }
            else {
                this.destroyPlayer(this.getPlayersObject(sender));
            }             
            this.enoughPlayers();
        }
    }

    /** we will also rejoin once if we was kicked :o and the game will end */
    protected void onKick(String channel, String kickerNick, String kickerLogin,
    String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) { 
            if (this.GameStarted) { this.gameEnd(); }
            joinChannel(channel); 
        }
        else if (this.isPlayerInGame(recipientNick)) {
            sendMessage(main.CHAN,recipientNick +" has fled, but the wolf caught him and was killed");
            if (this.GameRunning) { 
                sendRole(this.getPlayersObject(recipientNick));
                this.getPlayersObject(recipientNick).kill(this.round);
            }
            else {
                this.destroyPlayer(this.getPlayersObject(recipientNick));
            }
            this.enoughPlayers();      
        }
    }

    /**
     * lets see if user was in game and if the round is the same when he died....revive him :D
     */
    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (this.GameStarted == true && main.CHAN.equalsIgnoreCase(channel)) {
            if (this.isPlayerInGame(sender)) {
                Players player = this.getPlayersObject(sender);
                if (player.getRoundDeath() == this.round) { 
                    player.revive(); 
                    sendMessage(main.CHAN,sender +" has been revived!");
                    this.sendRole(sender);
                    if (me.isOp()) {
                        voice(main.CHAN,sender);  
                    }
                }
            }
        }
        if (sender.equalsIgnoreCase(getNick())) {
            me = this.getIRCUser(main.CHAN, getNick());
        }
    }

    /**
     * This is for when I need the IRC user object...
     * @returns User
     */
    protected User getIRCUser(String channel, String user) {
        User[] users = getUsers(channel);
        for (int i = 0; i < users.length; i++) {
            if (users[i].equals(user)) { 
                return users[i]; 
            }
        }
        return null;
    }

    /**
     * this checks to see if the user is in channel
     * use getIRCUser to get the User object...
     * @Returns boolean
     */
    protected  boolean isOn(User user) {
        if (user != null) { return true; }
        else { return false; }
    }

    /**
     * Try and reconnect once
     */

    protected void onDisconnect() {
        try {
            reconnect();
        } catch (Exception e) {}
    }

    //My methods

    /**
     * gets the player list
     * @ returns String
     */
    protected String getPlayerList() {
        StringBuilder playerList = new StringBuilder(10);
        String comma = " ";
        if (players.size() > 1) { comma = ", "; }
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            String nick = iter.next().getNick();
            if (!iter.hasNext()) { comma = ""; }
            playerList.append(nick + comma);
        }
        return playerList.toString();
    }

    /**
     * Gets alive status playerlist
     * @returns String
     */
    protected String getPlayerList(boolean alive) {
        StringBuilder playerList = new StringBuilder(10);
        String comma = " ";
        boolean anyPlayers = false;
        if (players.size() > 1) { comma = ", "; }
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            String role = "";
            Players p = iter.next();
            if (p.isAlive() == alive) {
                anyPlayers = true;
                if (!iter.hasNext()) { comma = ""; }
                if (alive == true) { role = "["+ p.getRole() +"]"; } 
                playerList.append(p.getNick() + role + comma);
            }
            if (!anyPlayers) { playerList.append("N/A"); }
        }
        return playerList.toString();
    }

    /**
     * get players object
     * @ returns Players
     */
    protected Players getPlayersObject(String player) {
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (player.equalsIgnoreCase(p.getNick())) {
                return p;
            }
        }
        return null;
    }

    /**
     * This destroys the Players object in the ArrayList
     */
    protected void destroyPlayer(Players p) {
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            Players player = iter.next();
            if (player == p) {
                iter.remove();
                break;
            }
        }
    }

    /**
     * checks for player to see if is in game
     * @ returns boolean
     */
    protected boolean isPlayerInGame(String player) {
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            if (player.equalsIgnoreCase(iter.next().getNick())) {
                return true;
            }
        }
        return false;
    }

    /**
     * this gives all players a mode
     */
    protected void givePlayersMode(String mode) {
        if (me.isOp()) {
            for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
                setMode(main.CHAN, mode +" "+ iter.next().getNick());
            }
        }
    }

    /**
     * this gives players roles
     */
    protected void givePlayersRoles() {
        Players[] pArray = new Players[players.size()];
        pArray = players.toArray(pArray);
        this.role = new Roles(pArray.length);
        role.giveRoles(pArray);
        for (int i = 0; i < pArray.length; i++) {
            if (pArray[i] != null) {
                sendNotice(pArray[i].getNick(),"Your role has been choosen!");
                this.sendRole(pArray[i].getNick());
            }
        }
    }

    /**
     * sends role as a notice  to player
     */
    protected void sendRole(String player) {
        Players p = this.getPlayersObject(player);
        sendNotice(p.getNick(), "Your role is: "+ p.getRole());
    }

    /*
     * sends role to channel
     */
    protected void sendRole(Players p) {
        sendMessage(main.CHAN, p.getNick() +"\'s role is: "+ p.getRole());
    }
    /**
     * this counts the wolves
     * returns int
     */
    protected int countAliveWolves() {
            int wolf = 0;
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (p.getRoleNumber() == 0 
            && p.isAlive()) {
                wolf++;
            }
        }
        return wolf;
    }
        /**
     * this counts the players
     * returns int
     */
    protected int countAlive() {
            int players = 0;
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (p.isAlive()) {
                players++;
            }
        }
        return players;
    }
    /**
     * this is called by the timer and starts the game 
     * @returns boolean
     */
    public boolean gameStart() {
        if (players.size() >= MINPLAYERS) { 
            sendMessage(main.CHAN,"The game has started!");
            this.GameRunning = true;
            this.vote = new Votes();
            this.givePlayersRoles();
            if (me.isOp()) { 
                this.givePlayersMode("+v");
                setMode(main.CHAN,"+m"); 
            }
            return true;
        }
        else {
            sendMessage(main.CHAN,"Not enough Players!");
            this.gameEnd();
            return false;
        }
    }

    /**
     * this changes day to night and vice versa
     */
    protected void Day() {
        //End of day
        if (Day) {
            Day = false; 
            sendMessage(main.CHAN, "As the Sunsets...the votes is counted....");
            Players p = vote.countVotes(players);
            if (p == null) { sendMessage(main.CHAN,"No player was voted to die today..."); }
            else { 
                sendMessage(main.CHAN, p.getNick() +" Was Voted to die....");
                sendAction(main.CHAN,"Watches as the angry mob beats "+ p.getNick() +" with sticks.");
                p.kill();
                this.enoughPlayers();
            }
        }
        //End of night
        else { 
            Day = true; 
            sendMessage(main.CHAN, "As the sun rises....the villagers expects the worst...");
            //kill();
            sendMessage(main.CHAN, "There is "+ countAliveWolves() +" wolves still alive..");
            sendMessage(main.CHAN, "You has "+ DAYSECONDS +" seconds to vote.");
        }
    }

    /**
     * checks to see if enoughPlayers...if not end game
     */
    protected void enoughPlayers() {
        if (players.size() <= 1 
        || (countAlive() - this.countAliveWolves()) <= 1
        ) { 
            sendMessage(main.CHAN,"Theres no players left...GameOver! D:");
            this.gameEnd();
        }        
    }

    /**
     * this cleans up when the game ends
     */
    protected void gameEnd() {
        if (me.isOp()) {
            this.givePlayersMode("-v");
            setMode(main.CHAN,"-m");
        }
        this.GameStarted = false;
        this.GameRunning = false;
        this.players.clear();
        this.role = null;
        this.vote = null;
        this.wbtimer.stop();
    }

    /**
     * the game starter timer class
     */
    private class WereBotTimer extends Timer {
        private WereBot wb;
        private  int seconds = 0;

        public WereBotTimer(WereBot wb, int waitTime) {
            super(waitTime);
            this.wb=wb;
        }

        public void process()  {
            if (GameStarted && !GameRunning) {
                if (++seconds == STARTSECONDS) { 
                    if (wb.gameStart()) {
                        wb.Day();
                    }
                    this.seconds=0;
                }
            }
            else if (GameRunning) {
                if (++seconds == DAYSECONDS) {
                    wb.Day();
                    this.seconds = 0;   
                }
            }
        }

        public void second()  {

        }
    }

}