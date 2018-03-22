package wps;

import java.lang.Thread;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class Main {
    private static double version = 1.1;
    private static String author = "nix007";
    
	private static int portnumber = 8080;
	private static int datasize = 1024;
	private static int  counter = 0;
	
	private static byte[] receiveData = new byte[datasize];

	/**
	 * Listens for UDP packets at 8080 and handles client communication in a separate thread.
	 * 
	 * @param args not used
	 * @throws Exception lazy error handling.
	 */
	public static void main(String args[]) {
		try {
			// TODO Auto-generated method stub
			@SuppressWarnings("resource")
			DatagramSocket serverSocket = new DatagramSocket(portnumber);
			System.out.printf("\nWPSServer v%.2f %s - Ready\n\n", version, author);
			System.out.printf("Ready for requests, listening on UDP: %d\n", portnumber);
			
			while(true) {
					++counter;
				    DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
					serverSocket.receive(receivePacket);
					System.out.println("[MAIN] Creating a thread for request: " + counter);
					WPSServer srv = new WPSServer(receivePacket, counter);
					new Thread(srv).start();
					receiveData = new byte[datasize];
			}
		} catch(Exception e) {
			System.out.println("Something went horrible wrong");
			System.out.println("There might be a clue in the following line");
			System.out.println(e.getMessage());
		}
	}
}
