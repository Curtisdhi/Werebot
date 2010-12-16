package org.Werebot;
 
/**
 * Sets up the bot and connects...
 * 
 * @author Pickle
 * @version 0.3
 */
public class Main {
    protected final double VERSION = 0.4;
    protected String SERVER = "irc.coldfront.net";
    protected String NICK = "WereBot[ALPHA]";
    protected String CHAN = "#pickle";
    protected String IDENTIFY_PASS = "";
    
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        //Now start our bot up.
        Config conf = new Config("config.conf");
        String SS = conf.getParameter("server");
        if (SS != null) { main.SERVER = SS; } 
        SS = conf.getParameter("nick");
        if (SS != null) { main.NICK = SS; } 
        SS = conf.getParameter("chan");
        if (SS != null) { main.CHAN = SS; } 
        SS = conf.getParameter("identify_pass");
        if (SS != null) { main.IDENTIFY_PASS = SS; } 
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