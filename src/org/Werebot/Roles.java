package org.Werebot;

import java.util.Random;

/**
 * handles the roles
 * 
 * @author Pickle
 * @version 0.2
 */
public class Roles {
    private final Random r = new Random();
    /** "0" = wolf "1" = seer "2" = villager*/
    protected final String[][] Roles = {
    {"Wolf", "Big Bad Wolf"}, 
    {"Seer", "Spai", "Secret Person"}, 
    {"Villager","The Village Pickle", "Common hobo", "Not Wolf or Seer", "Pr0ud N00b"}
    };
    /** Wolf on villager ratio e.g. 1 wolf per 5 villagers */
    protected final int WolfRatio=5;
        
    protected int wolves = 0, villager = 0, seer = 0;
    
    public Roles() { }
    public Roles(int players) {
        this.villager = players;
        this.seer = 1;
        this.wolves = (players / WolfRatio);
        if (this.wolves < WolfRatio) { this.wolves = 1; }
    }
    /**
     * give Players roles
    */
    public void giveRoles(Players[] players)  {
        boolean[] playersUsed = new boolean[players.length];
        for (int i = 0; i < this.wolves; i++) {
            setRandPlayerRole(playersUsed,players,0);
        }
            setRandPlayerRole(playersUsed,players,1);
            for (int i = 0; i < players.length; i++) {
                if (!playersUsed[i]) { 
                    players[i].setRole(Roles[2][this.r.nextInt(Roles[2].length)]);
                    players[i].setRoleNumber(2);
                }
            }
    }
    /**
     * this is so i don't have to keep copying the same loop when i need it..
     */
    @SuppressWarnings("unused")
	protected void setRandPlayerRole(boolean[] playersUsed,Players[] players
    ,int role) {
            for (int rand = this.r.nextInt(players.length); 
            !playersUsed[rand];
            rand = this.r.nextInt(players.length)) {
                players[rand].setRole(
                Roles[role][this.r.nextInt(Roles[role].length)]
                );
                players[rand].setRoleNumber(role);
                playersUsed[rand] = true;
                break;
            } 
    }
    
}
