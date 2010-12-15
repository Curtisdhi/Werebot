package org.Werebot;


/**
 * Player.java - deals with player data
 * 
 * @author Pickle
 * @version 0.2
 */
public class Players {
    private Main main;
    private WereBot wb;
    
    protected boolean alive = false;
    protected String nick;
    protected String role;
    protected int roleNumber;
    protected Players vote;
    
    protected int roundDeath = 0;
    
    public Players() { }
    public Players(Main main, WereBot wb) {
        this.alive = true;
        this.wb=wb;
        this.main=main;
    }
    protected void setNick(String nick) { this.nick=nick; }
    protected void setRole(String role) { this.role=role; } 
    protected void setRoleNumber(int roleNumber) { this.roleNumber=roleNumber; }   
    protected void setVote(Players vote) { this.vote = vote; }
    protected void clearVote() { this.vote = null; }
    protected void kill() { 
        this.alive = false; 
        wb.sendMessage(main.CHAN,this.nick +" was a "+ this.role);
    }
    protected void kill(int round) { 
        this.kill(); 
        this.roundDeath = round; 
    }
    protected void revive() { this.alive = true; this.roundDeath = 0; }

    protected String getNick() { return this.nick; }
    protected String getRole() { return this.role; }
    protected int getRoleNumber() { return this.roleNumber; }
    protected Players getVote() { return this.vote; }
    protected int getRoundDeath() { return this.roundDeath; }
    protected boolean isAlive() { return this.alive; }

}
