package org.Werebot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Reads a Config file
 * 
 * @author Pickle
 * @version 0.1
 */
public class Config {
    
    protected String CONFIG = "config.conf";
    
    protected BufferedReader inputStream;
    protected BufferedWriter writerStream;
    
    public Config(String CONFIG) {
    	if (CONFIG != null) { this.CONFIG = CONFIG; } 
    	try {
			inputStream = new BufferedReader(new FileReader(this.CONFIG));
			writerStream = new BufferedWriter(new FileWriter(this.CONFIG));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    /**
     * gets the parameter in said item
     * @param item
     * @return String
     */
	protected String getParameter(String item) {
    	try {		
    		String readLine;
			while ((readLine = inputStream.readLine()) != null) {
				String[] Token = readLine.split("=");
				System.out.println(readLine);
				if (Token[0].equalsIgnoreCase(item)) {
					return Token[1];
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * creates a item and value pair inside a config file
	 * @param item
	 * @param value
	 */
	protected void writeItem(String item,String value) {
		try {
		writerStream.write(item +" = "+ value);
		writerStream.newLine();
		writerStream.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
	} 
	protected void close() {
		try {
			inputStream.close();
			writerStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
