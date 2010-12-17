package org.Werebot;

import org.jibble.pircbot.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The Main Engine
 * 
 * @author Pickle
 * @version 0.4
 */

public class WereBot extends PircBot {
    private Main main;
    private WereBotTimer wbtimer = new WereBotTimer(this,1000);

    protected boolean GameStarted = false,GameRunning = false, Day = false;
    protected List<Players> players = new ArrayList<Players>();
    protected Roles role = null;
    protected Votes vote = null;

    protected int STARTSECONDS = 30;
    protected int DAYSECONDS = 30;
    protected int MINPLAYERS = 3;

    protected User me;

    protected int round = 0;
    //overriding PircBot
    public WereBot(Main main) {
        this.main = main;
        this.setName(main.NICK);
        Config conf = new Config("config.conf");
        String SS = conf.getParameter("startseconds");
        if (SS != null) { STARTSECONDS = Integer.parseInt(SS); } 
        SS = conf.getParameter("dayseconds");
        if (SS != null) { DAYSECONDS = Integer.parseInt(SS); } 
        SS = conf.getParameter("minplayers");
        if (SS != null) { MINPLAYERS = Integer.parseInt(SS); } 
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
            	gameStart();
                Players p = new Players(main,this);
                p.setNick(sender);
                this.players.add(p);
                sendMessage(main.CHAN,sender +" has started a new game!");
                sendMessage(main.CHAN, "The game will start in "+ STARTSECONDS +" seconds!");
            }
            else  if (Token[0].equalsIgnoreCase("!join") && this.GameStarted && !this.GameRunning) {
                if (!isPlayerInGame(getPlayersObject(sender))) {
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
                if (this.isPlayerInGame(getPlayersObject(sender))) {
                    for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
                        Players player = iter.next();
                        if (player.getNick().equalsIgnoreCase(sender)) { 
                            if (this.GameRunning) { player.kill(); }
                            else { iter.remove(); }
                            sendMessage(main.CHAN,sender +" has Left");
                            setMode(main.CHAN, "-v "+ player.getNick());
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
                if (this.isPlayerInGame(getPlayersObject(sender))) {
                    if (this.isPlayerInGame(getPlayersObject(Token[1]))) {
                        Players PVoter = this.getPlayersObject(sender), PVotee = this.getPlayersObject(Token[1]);
                        if (PVoter.isAlive()&& PVotee.isAlive()) {
                            vote.setVote(PVoter, PVotee);
                            sendMessage(main.CHAN, sender +" just voted for: "+ Token[1]);
                        }
                    }
                    else if (Token[1].equals("0")) { sendMessage(main.CHAN, sender +" votes noone"); }
                }   
            }
            else if (Token[0].equalsIgnoreCase("!alive") && this.GameRunning) {
                sendMessage(main.CHAN,"Alive Players: "+ this.getPlayerList(true));
            }   
            else if (Token[0].equalsIgnoreCase("!dead") && this.GameRunning) {
                sendMessage(main.CHAN,"Dead Players: "+ this.getPlayerList(false));
            } 
            else if (Token[0].equalsIgnoreCase("!list") && this.GameStarted) {
                sendMessage(main.CHAN,"All Players: "+ this.getPlayerList());
            }             
        }
    }

	protected  void	onPrivateMessage(String sender, String login, String hostname, String message) {
        String[] Token = message.split(" ");
        if (Token[0].equalsIgnoreCase("kill") && Token.length > 1) {
        	if (isOn(getIRCUser(main.CHAN, Token[1]))) {
        		Players voter = getPlayersObject(sender), votee = getPlayersObject(Token[1]);
        		if (isWolf(voter) && votee.isAlive()) {
        			sendMessage(main.CHAN,"You has choosen to kill "+ Token[1]);
        			voter.setVote(votee);
        		}
        	}
        }
        else if (Token[0].equalsIgnoreCase("see") && Token.length > 1 ) {
        	if (isOn(getIRCUser(main.CHAN, Token[1]))) {
        		Players voter = getPlayersObject(sender), votee = getPlayersObject(Token[1]);
        		if (isSeer(voter) && votee.isAlive()) {
        			sendMessage(main.CHAN,"You has choosen to see "+ Token[1]);
        			voter.setVote(votee);
        		}
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
     * this "kills" the player onQuit 
     * also if user rejoins back within the round he will be revived :D
     */
    protected void onQuit(String sourceNick,String sourceLogin,
    String sourceHostname,String reason) {
        if (sourceNick.equalsIgnoreCase(getNick())) { 
            if (this.GameStarted) { this.gameEnd(); }
        }
        else if (this.GameStarted && this.isPlayerInGame(getPlayersObject(sourceNick))) {
            sendMessage(main.CHAN,sourceNick +" has fled, but the wolf caught him and was killed");
            if (this.GameStarted) {
                sendRole(this.getPlayersObject(sourceNick));
                this.getPlayersObject(sourceNick).kill(this.round);
                setMode(main.CHAN, "-v "+ sourceNick);
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
        else if (this.GameStarted && this.isPlayerInGame(getPlayersObject(sender))) {
            sendMessage(main.CHAN,sender +" has fled, but the wolf caught him and was killed");
            if (this.GameStarted) {
                sendRole(this.getPlayersObject(sender)); 
                this.getPlayersObject(sender).kill(this.round);
                setMode(main.CHAN, "-v "+ sender);
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
        else if (this.isPlayerInGame(getPlayersObject(recipientNick))) {
            sendMessage(main.CHAN,recipientNick +" has fled, but the wolf caught him and was killed");
            if (this.GameStarted) { 
                sendRole(this.getPlayersObject(recipientNick));
                this.getPlayersObject(recipientNick).kill(this.round);
                setMode(main.CHAN, "-v "+ recipientNick);
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
            if (this.isPlayerInGame(getPlayersObject(sender))) {
                Players player = this.getPlayersObject(sender);
                if (player.getRoundDeath() == this.round) { 
                    player.revive(); 
                    sendMessage(main.CHAN,sender +" has been revived!");
                    this.sendPrivateRole(getPlayersObject(sender));
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
     * @return User
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
     * @return boolean
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
        } catch (Exception e) { 
        	e.printStackTrace();
        }
    }

    //My methods

    /**
     * gets the player list
     * @return String
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
     * @return String
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
     * @return Players
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
            if (player.equals(p)) {
                iter.remove();
                break;
            }
        }
    }

    /**
     * checks for player to see if is in game
     * @return boolean
     */
    protected boolean isPlayerInGame(Players player) {
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            if (iter.next().equals(player)) {
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
            	Players p = iter.next();
            	if (p.isAlive()) {
            		setMode(main.CHAN, mode +" "+ p.getNick());
            	}
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
                this.sendPrivateRole(pArray[i]);
            }
        }
    }

    /**
     * sends role as a notice  to player
     */
    protected void sendPrivateRole(Players player) {
        sendNotice(player.getNick(), "Your role is: "+ player.getRole());
    }

    /*
     * sends role to channel
     */
    protected void sendRole(Players p) {
        sendMessage(main.CHAN, p.getNick() +"\'s role is: "+ p.getRole());
    }
    protected void clearVotes() {
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            iter.next().clearVote();
        }
	}
    /**
     * this counts the wolves
     * @return int
     */
    protected int countAliveWolves() {
        int wolf = 0;
        for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (isWolf(p) && p.isAlive()) {
                wolf++;
            }
        }
        return wolf;
    }
    /**
     * gets the wolflist
     * @return Players[]
     */
    protected Players[] getWolves() {
    	List<Players> wolfTemp = new ArrayList<Players>();
    	for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
    		Players player = iter.next();
    		if (player.getRoleNumber() == 0 && player.isAlive()) {
    			wolfTemp.add(player);
    		}
    	}
        return (Players[]) wolfTemp.toArray();
    }
    /**
     * checks is player is wolf
     * @return boolean
     */
    protected boolean isWolf(Players player) {
        if (player.getRoleNumber() == 0) {
            return true;
        }
        return false;
    }
    /**
     * checks is player is seer
     * @return boolean
     */
    protected boolean isSeer(Players player) {
        if (player.getRoleNumber() == 1) {
            return true;
        }
        return false;
    }
    /**
     * checks is player is villager
     * @return boolean
     */
    protected boolean isVillager(Players player) {
        if (player.getRoleNumber() == 3) {
           	return true;
        }
        return false;
    }
    /**
     * this counts the players
     * @return int
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
     * initiate a new game
     */
    protected void gameStart() {
        this.GameStarted = true;
        wbtimer.start();
	}
    /**
     * this is called by the timer and starts the game 
     * @return boolean
     */
    protected boolean gameTimedStart() {
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
            givePlayersMode("-v");
            setMode(main.CHAN,"+m");
            sendMessage(main.CHAN, "As the Sunsets...the votes is counted....");
            Players p = vote.countVotes(players);
            clearVotes();
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
            givePlayersMode("+v");
            setMode(main.CHAN,"-m");
            sendMessage(main.CHAN, "As the sun rises....the villagers expects the worst...");
            for (Iterator<Players> iter = this.players.iterator(); iter.hasNext();) {
            	Players p = iter.next();
            	if (p.isAlive() && p.getRoleNumber() == 1 && p.getVote() != null) { 
            		sendNotice(p.getNick(), "You has gather enough info about "+ p.getVote().getNick() +".");      	
            		sendNotice(p.getNick(), p.getVote().getNick()+" is a "+ p.getVote().getRole());
            		p.clearVote();
            		break;
            	}           	      	
            }
            Players p = vote.countVotes(players);
            clearVotes();
        	if (p.isAlive() && p != null) { 
        		sendMessage(main.CHAN, p.getNick() +" was mauled by a wolf!! :O");    
        		p.kill();
        	}
            sendMessage(main.CHAN, "There is "+ countAliveWolves() +" wolves still alive..");
            sendMessage(main.CHAN, "You has "+ DAYSECONDS +" seconds to vote.");
        }
        round++;
    }


	/**
     * checks to see if enoughPlayers...if not end game
     */
    protected void enoughPlayers() {
        if (players.size() <= 1 
        || (countAlive() - countAliveWolves()) <= 1
        || countAliveWolves() < 1
        ) { 
        	if (GameRunning) { 
        		if (countAliveWolves() < 1) {
	        		Players[] wolves = getWolves();
	        		StringBuilder sb = new StringBuilder(10);
	        		String comma = " ";
	        		for (int i=0; i > wolves.length; i++) {
	        			if (i == wolves.length) { comma = ""; }
	        			if (i > wolves.length) { comma = "and, "; }
	        			else { comma = ", "; }
	        			sb.append(wolves[i].getNick() + comma);
	        		}
	        		
	        		sendMessage(main.CHAN, sb.toString() +"was wolf");
	        		
	        	}
        	}
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
        private int seconds = 0;

        public WereBotTimer(WereBot wb, int waitTime) {
            super(waitTime);
            this.wb=wb;
        }

        public void process()  {
            if (GameStarted && !GameRunning) {
                if (++seconds == STARTSECONDS) { 
                    if (wb.gameTimedStart()) {
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