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
        setName(main.NICK);
        Config conf = new Config("config.conf");
        int SS = conf.getInt("startseconds");
        if (SS != 0) { STARTSECONDS = SS; } 
        SS = conf.getInt("dayseconds");
        if (SS != 0) { DAYSECONDS = SS; } 
        SS = conf.getInt("minplayers");
        if (SS != 0) { MINPLAYERS = SS; } 
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
            if (Token[0].equalsIgnoreCase("!start") && !GameStarted) {
            	gameStart();
                Players p = new Players(main,this);
                p.setNick(sender);
                players.add(p);
                sendMessage(main.CHAN,sender +" has started a new game!");
                sendMessage(main.CHAN, "The game will start in "+ STARTSECONDS +" seconds!");
            }
            else  if (Token[0].equalsIgnoreCase("!join") && GameStarted && !GameRunning) {
                if (!isPlayerInGame(getPlayersObject(sender))) {
                    Players p = new Players(main,this);                
                    p.setNick(sender);
                    players.add(p);
                    sendMessage(main.CHAN,sender +" has Joined");
                }
                else { 
                    sendMessage(main.CHAN,sender +", you're already in the game.");
                }
            }   
            else if (Token[0].equalsIgnoreCase("!leave") && GameStarted) {
                if (isPlayerInGame(getPlayersObject(sender))) {
                    for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
                        Players player = iter.next();
                        if (player.getNick().equalsIgnoreCase(sender)) { 
                            if (GameRunning) { player.kill(); }
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
                enoughPlayers();
            }
            else if (Token[0].equalsIgnoreCase("!vote") 
            && GameStarted 
            && GameRunning 
            && Token.length > 1
            && Day) {
                if (isPlayerInGame(getPlayersObject(sender))) {
                    if (isPlayerInGame(getPlayersObject(Token[1]))) {
                        Players PVoter = getPlayersObject(sender), PVotee = getPlayersObject(Token[1]);
                        if (PVoter.isAlive()&& PVotee.isAlive()) {
                            vote.setVote(PVoter, PVotee);
                            sendMessage(main.CHAN, sender +" just voted for: "+ Token[1]);
                        }
                    }
                    else if (Token[1].equals("0")) { sendMessage(main.CHAN, sender +" votes noone"); }
                }   
            }
            else if (Token[0].equalsIgnoreCase("!list") && GameStarted) {
                sendMessage(main.CHAN,"All Players: "+ getPlayerList());
            } 
            else if (Token[0].equalsIgnoreCase("!alive") && GameRunning) {
                sendMessage(main.CHAN,"Alive Players: "+ getPlayerList(true));
            }   
            else if (Token[0].equalsIgnoreCase("!dead") && GameRunning) {
                sendMessage(main.CHAN,"Dead Players: "+ getPlayerList(false));
            } 
            else if (Token[0].equalsIgnoreCase("!role") && GameRunning) {
            	sendPrivateRole(getPlayersObject(sender));
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
        if (GameStarted && newNick != getNick()) {
                Players  player = getPlayersObject(oldNick);
                if (oldNick.equalsIgnoreCase(player.getNick())) {
                    player.setNick(newNick);
                }
        }
        else if (newNick == getNick()) {
            me = getIRCUser(main.CHAN, getNick());
        }
    }

    /**
     * this "kills" the player onQuit 
     * also if user rejoins back within the round he will be revived :D
     */
    protected void onQuit(String sourceNick,String sourceLogin,
    String sourceHostname,String reason) {
        if (sourceNick.equalsIgnoreCase(getNick())) { 
            if (GameStarted) { gameEnd(); }
        }
        else if (GameStarted && isPlayerInGame(getPlayersObject(sourceNick))) {
            sendMessage(main.CHAN,sourceNick +" has fled, but the wolf caught him and was killed");
            if (GameRunning) {
                sendRole(getPlayersObject(sourceNick));
                getPlayersObject(sourceNick).kill();
            }
            else if (GameStarted) { 
                destroyPlayer(getPlayersObject(sourceNick));
            }
            enoughPlayers();
        }
    }

    protected void onPart(String channel, String sender, String login, String hostname) {
        if (sender.equalsIgnoreCase(getNick())) { 
            if (GameStarted) { gameEnd(); }
        }
        else if (GameStarted && isPlayerInGame(getPlayersObject(sender))) {
            sendMessage(main.CHAN,sender +" has fled, but the wolf caught him and was killed");
            if (GameRunning) {
                getPlayersObject(sender).kill(round);   
            }
            else if (GameStarted) {
                destroyPlayer(getPlayersObject(sender));
            }             
            enoughPlayers();
        }
    }

    /** we will also rejoin once if we was kicked :o and the game will end */
    protected void onKick(String channel, String kickerNick, String kickerLogin,
    String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) { 
            if (GameStarted) { gameEnd(); }
            joinChannel(channel); 
        }
        else if (isPlayerInGame(getPlayersObject(recipientNick))) {
            sendMessage(main.CHAN,recipientNick +" has fled, but the wolf caught him and was killed");
            if (GameRunning) { 
                getPlayersObject(recipientNick).kill(round);
            }
            else if (GameStarted) {
                destroyPlayer(getPlayersObject(recipientNick));
            }
            enoughPlayers();      
        }
    }

    /**
     * lets see if user was in game and if the round is the same when he died....revive him :D
     */
    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (GameStarted == true && main.CHAN.equalsIgnoreCase(channel)) {
            if (isPlayerInGame(getPlayersObject(sender))) {
                Players player = getPlayersObject(sender);
                if (player.getRoundDeath() == round) { 
                    player.revive(); 
                    sendMessage(main.CHAN,sender +" has been revived!");
                    sendPrivateRole(getPlayersObject(sender));
                    if (me.isOp()) {
                        voice(main.CHAN,sender);  
                    }
                }
            }
        }
        if (sender.equalsIgnoreCase(getNick())) {
            me = getIRCUser(main.CHAN, getNick());
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
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
            for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        role = new Roles(pArray.length);
        role.giveRoles(pArray);
        for (int i = 0; i < pArray.length; i++) {
            if (pArray[i] != null) {
                sendNotice(pArray[i].getNick(),"Your role has been choosen!");
                sendPrivateRole(pArray[i]);
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
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
            iter.next().clearVote();
        }
	}
    
    /**
     * this counts the wolves
     * @return int
     */
    protected int countAliveWolves() {
        int wolf = 0;
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (isWolf(p) && p.isAlive()) {
                wolf++;
            }
        }
        return wolf;
    }
    
    /**
     * counts the villagers
     * @return int
     */
    protected int countAliveVillagers() {
        int vill = 0;
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (isVillager(p) && p.isAlive()) {
                vill++;
            }
        }
        return vill;
    }
    
    /**
     * gets the wolflist
     * @return Players[]
     */
    protected Players[] getWolves() {
    	List<Players> wolfTemp = new ArrayList<Players>();
    	for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        int player = 0;
        for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
            Players p = iter.next();
            if (p.isAlive()) {
                player++;
            }
        }
        return player;
    }
    
    /**
     * initiate a new game
     */
    protected void gameStart() {
        GameStarted = true;
        wbtimer.start();
	}
    
    /**
     * this is called by the timer and starts the game 
     * @return boolean
     */
    protected void gameTimedStart() {
        if (countAlive() >= MINPLAYERS) { 
            sendMessage(main.CHAN,"The game has started!");
            sendMessage(main.CHAN, "There is "+ countAliveWolves() +" wolves still alive..");
            sendMessage(main.CHAN, "You has "+ DAYSECONDS +" seconds to vote.");
            GameRunning = true;
            Day = true; 
            vote = new Votes();
            givePlayersRoles();
            if (me.isOp()) { 
                givePlayersMode("+v");
                setMode(main.CHAN,"+m"); 
            }
        }
        else {
            sendMessage(main.CHAN,"Not enough Players!");
            gameEnd();
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
            sendMessage(main.CHAN, "As the Sunsets...the votes is counted....");
            Players p = vote.countVotes(players);
            clearVotes();
            if (p == null) { sendMessage(main.CHAN,"No player was voted to die today..."); }
            else { 
                sendMessage(main.CHAN, p.getNick() +" Was Voted to die....");
                sendAction(main.CHAN,"Watches as the angry mob beats "+ p.getNick() +" with sticks.");
                p.kill();
                enoughPlayers();
            }
        }
        //End of night
        else { 
            Day = true; 
            givePlayersMode("+v");
            sendMessage(main.CHAN, "As the sun rises....the villagers expects the worst...");
            for (Iterator<Players> iter = players.iterator(); iter.hasNext();) {
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
        if (countAlive() <= 1 
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
            gameEnd();
        }        
    }

    /**
     * this cleans up when the game ends
     */
    protected void gameEnd() {
        if (me.isOp()) {
            givePlayersMode("-v");
            setMode(main.CHAN,"-m");
        }
        GameStarted = false;
        GameRunning = false;
        players.clear();
        role = null;
        vote = null;
        wbtimer.stop();
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
                    wb.gameTimedStart();
                    seconds=0;
                }
            }
            else if (GameRunning) {
                if (++seconds == DAYSECONDS) {
                    wb.Day();
                    seconds = 0;   
                }
            }
            System.out.println(seconds +" SECOND :D");
        }

        public void second()  {

        }
    }

}