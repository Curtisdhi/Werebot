package org.Werebot;
 
/**
 * Sets up the bot and connects...
 * 
 * @author Pickle
 * @version 0.2
 */
public class Main {
    protected final double VERSION = 0.2;
    protected final String SERVER = "irc.coldfront.net";
    protected final String CHAN = "#okandjo";
    protected final String NICK = "WereBot[ALPHA]";
    protected final String IDENTIFY_PASS = "";
    
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        //Now start our bot up.
        WereBot bot = new WereBot(main);
        
        //Enable debugging output.
        bot.setVerbose(true);
        
        //Connect to the IRC server.
        bot.connect(main.SERVER);
    
        //Join the channel.
        bot.joinChannel(main.CHAN);
        bot.setAutoNickChange(true);
    }
    
}