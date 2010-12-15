package org.Werebot;

import java.util.List;
//import java.util.ArrayList;
//import java.util.Iterator;

/**
 * Records vote data
 * 
 * @author Pickle
 * @version 0.1
 */
public class Votes {
    protected int totalVotes = 0;
    
    public Votes() { }
    
    protected void setVote(Players player, Players vote) {
        player.setVote(vote);
        totalVotes++;
    }
    /*
     *Count votes returns a Player object if the votes is vaild
     * @returns Players
     */
    protected Players countVotes(List<Players> PList) {
        Players[] pArray = new Players[PList.size()];
        pArray = PList.toArray(pArray);
        int[] votes = new int[pArray.length + 1];
        for (int i = 0; i < pArray.length; i++) {
            if (pArray[i].isAlive()) {
                for (int ii = 0; ii < pArray.length; ii++) {
                    if (pArray[i].equals(pArray[ii].getVote())) {
                          votes[i]++;
                    }
                 }
                 if (pArray[i].getVote() == null) { votes[votes.length -1]++; }
              }
          }
          // loop to find the highest vote count.
          int highVote = 0;
          int index = 0;
          for (int i = 0; i < votes.length; i++) {
               if (votes[i] > highVote) { 
                   highVote = votes[i]; 
                   index = i;
                }
            }
            if (index == votes.length || totalVotes <= 0) { return null; }
        return pArray[index];
    }

}
