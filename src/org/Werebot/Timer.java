package org.Werebot;

/**
 * Executes a timed while loop
 * 
 * @author Pickle
 * @version 0.1
 */
public abstract class Timer implements Runnable {
    private int waitTime;
    private Thread thread;
    
    public Timer(int waitTime) {
    	super();
        this.waitTime=waitTime;
    }
    
    public void start() {
        thread = new Thread(this);
        thread.start();
    }
    public void run() {
        Thread myThread = Thread.currentThread();
         while (myThread == thread) {
            try {
                process();
                second();
                Thread.sleep(waitTime);
            }catch(Exception e) { e.printStackTrace(); }
         
        }
    }
    public void stop() {
        thread = null;
    }
    public void sleep(int waitTime) { }
    abstract void process(); 
    
    abstract void second();
}
