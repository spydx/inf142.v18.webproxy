/*
 * Client to communicate with WPS Server.
 * 
 * @author Kenneth Fossen
 * @version 0.1
 *  
 */
//package client; //uncommented for javac compiling.

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * WPSClient
 * 
 * @author nix007
 *
 */
class WPSClient {
	private static int portnumber = 8080;
    private static byte[] sendData = new byte[1024];
    private static byte[] receiveData = new byte[1024];
    private static InetAddress IPAddress;
    private static DatagramSocket clientSocket;
    private static DatagramPacket receivePacket;
    private static double version = 1.0;
    private static String author = "nix007"; 
    private static boolean running = true;
    
    /**
     * Start the client and serves you the menu
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
	    
		System.out.printf("\nWPSClient v%.2f %s - Ready\n\n", version, author);
		
		getHelpMenu();
		try {
			IPAddress = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			System.out.println("Unable to find server or invalid servername");
		}
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("Port taken, cannot start client");
		}
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	    
	    do {
	    		System.out.printf("[k] %s > ",
	    				clientSocket.isBound() ? "[Connected]" : "[Disconnected]");
	    		String command = inFromUser.readLine();
	    		
	    		if(!command.isEmpty()) {
		    		switch(command.substring(0, 1).toUpperCase()) {
			    		case "G":
			    			connectServer(command);
			    			break;
			    		case "?":
			    			getHelpMenu();
			    			break;
			    		case "Q":
			    			running = false;
			    			break;
			    		default :
			    			System.out.println("Invalid command!");
			    			System.out.println("Enter one of the following commands");
			    			getHelpMenu();
			    			break;
		    		}
	    		}
	    } while(running);
	    
	    clientSocket.close();
	}
    /**
     * Sending UDP Request to WPS Server. 
     * 
     * @param command is the «get www.uib.no» 
     * @throws IOException will fail if not able to get IO for datastream.
     */
    private static void connectServer(String command) throws IOException{
    		if(sendRequest(command)) {
	  		try {
					reciveData("ACK");
					String message = new String(receivePacket.getData());
					
					if(checkMessage(message)) {
						reciveData("HEADER");
						ArrayList<String> headerlist = new ArrayList<>();
						byte[] bytes = receivePacket.getData();
						ByteArrayInputStream b = new ByteArrayInputStream(bytes);
						DataInputStream i = new DataInputStream(b);
						while(i.available() > 0) {
							try {
								String header = i.readUTF();
								if(!header.isEmpty())
									headerlist.add(header);
							} catch(IOException e) {
								;
							}	
						}
						if(headerlist.isEmpty()) {
							System.out.println("Something went wrong try again!");
						} else {
							System.out.println("[Server] HEADER dump:");
							System.out.println("");
							for(String header : headerlist) {
								if(!header.isEmpty())
								System.out.println(header);
							}
							System.out.println("");
						}
					}
			} catch (IOException e) {
					System.out.println("[k] Unable to recive packet, try again facking UDP");
			}
    		}
    } 
    /**
     * Checking the message if the ACK message has been received
     * @param message Message to be checked.
     * @return true if ACK is receivced, false if otherwise
     */
    private static boolean checkMessage(String message) {
    		String[] strArr = message.split(" ");
    		System.out.println("[Server] " + Arrays.toString(strArr));
    		if(strArr[0].startsWith("ACK")) {
    			return true;
    		}
    		
    		return false;
    }
    /**
     *  Generic Sending packets to server
     * @param data Data we are sending 
     * @return returns true if packet is sendt, false otherwise.
     * @throws IOException
     */
    private static boolean sendRequest(String data) throws IOException{
    		try {
	    		sendData = data.getBytes();
		    
		    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portnumber);
		
		    System.out.println("[k] Sending packet to: " + IPAddress.toString());
		    clientSocket.send(sendPacket);
	
		    sendData = new byte[1024];
		    return true; 
	    } catch (IOException e) {
	    		System.out.println("[k] Unable to send packet, try again");
	    		return false;
	    }
    }
    /**
     * Generic receive data method
     * 
     * @param packet packet to be received
     * @throws IOException 
     */
    private static void reciveData(String packet) throws IOException {
    		receiveData = new byte[1024];
	    receivePacket = new DatagramPacket(receiveData, receiveData.length);
	    System.out.println("[k] Waiting for server " + packet);
	    clientSocket.receive(receivePacket);
    }
    
    /**
     * Prints help menu
     * 
     */
    private static void getHelpMenu() {
    		System.out.println("Help menu:\n");
    		System.out.println("GET hostname - request a webpage e.g: GET www.example.com");
    		System.out.println("GET hostname/path - request a subpage e.g GET www.uib.no/matnat/");
    		System.out.println("GET http://hostname - request a webpage e.g GET http://www.kefo.no");
    		System.out.println("GET https://hostname - request a secure webpage e.g GET https://www.kefo.no");
    		System.out.println();
    		System.out.println("? - this menu");
    		System.out.println("Q - end application");
    		System.out.println("");		
    }
 }