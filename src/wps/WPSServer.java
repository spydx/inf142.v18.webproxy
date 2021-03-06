package wps;

// Network imports
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
//List support
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

//IOExceptions and Readers/Writers
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

//Exception and multithread
import java.lang.IllegalArgumentException;
import java.lang.Runnable;

/**
 * WPSServer 
 * 
 * @author nix007
 *
 */
public class WPSServer implements Runnable{

	private DatagramPacket packet;
	private int id;
	private static int datasize = 1024;
	private static byte[] sendData = new byte[datasize];
	private DatagramSocket serverSocket;
	private Socket clientSocket = null;
	private SSLSocket sslsocket = null;
	private String HTTP_VERSION = "HTTP/1.1";
	private String USER_AGENT = "Java Homebrew";
	private final int HTTP_PORT = 80; 
	private final int HTTPS_PORT = 443;
	private boolean HTTPS_MODE = false;
	
	/**
	 * Creating the WPS Server with a packet.
	 *  
	 * @param receivePacket communication to process.
	 * @param id Thread ID, coming from counter in Main.
	 */
	public WPSServer(DatagramPacket receivePacket, int id) {
		
		this.packet = receivePacket;
		this.id = id;
		System.out.println(getThreadID() + HTTP_VERSION + " User-Agent: " + USER_AGENT);
	}
	/**
	 * ThreadID
	 * 
	 * @return thread id, from counter in Main.
	 */
	private String getThreadID() {
		return "[Thread " + this.id +"] ";
	}
	/**
	 * 
	 * Implemented method from Runnable
	 * must be here or Threading is not working.
	 * 
	 */
	@Override
	public void run(){
	    /*
	     * Stripping incoming packet for details to return.
	     * and ACKing it
	     */
		String command = new String(packet.getData());
	    InetAddress IPAddress = packet.getAddress();
	    int port = packet.getPort();
	      
	    System.out.println(getThreadID() + "Packet received form: " + IPAddress.toString());
	    System.out.println(getThreadID() + "Data received: " + command);
	    
	    try {
	    		ArrayList<String> urlstring = processURL(command);
	    		if(urlstring.isEmpty()) {
	    			ArrayList<String> packetdata = new ArrayList<>();
	    			packetdata.add(command.toUpperCase().replace("GET", "RST"));
	    			clientControlMessages(packetdata, IPAddress, port);
	    		} else {
	    			ArrayList<String> ackdata = new ArrayList<>();
	    			ackdata.add(command.toUpperCase().replace("GET", "ACK"));
	    			clientControlMessages(ackdata, IPAddress, port);
	    			try {
						ArrayList<String> data = getHttpHeaders(urlstring);
						if(data != null)
							clientControlMessages(data, IPAddress, port);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    } catch (IllegalArgumentException e) {
	    		;
	    } catch (IOException e) {
	    		;
	    }
	 
		serverSocket.close();
		System.out.println(getThreadID() + "Request done!");
	}
	
	/**
	 * ackData is client communication control messages.
	 *  
	 * @param data What kind of data is to be sent to client, ACK, RST or the HEADER info.
	 * @param IPAddress client IPAddress.
	 * @param port client port
	 * @throws IOException if we either cant send the packet or writeUTF it will throw exception.
	 * 
	 */
	private void clientControlMessages(ArrayList<String> data, InetAddress IPAddress, int port) throws IOException{
		String command = data.get(0).trim();
		if(command.startsWith("RST") || command.startsWith("ACK")) {
			System.out.println(getThreadID() + "Responding data: " + command);
			sendData = command.getBytes();
		} else {
			// Process header data to return
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream o = new DataOutputStream(b);
			for(String i: data) {
				o.writeUTF(i);
			}
			sendData = b.toByteArray();
		}
	    
	    DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddress, port);
		
	    serverSocket = new DatagramSocket();
	    System.out.println(getThreadID() + "Sending to client:");
	    System.out.println(getThreadID() + data); 
	  	serverSocket.send(sendPacket); 	
	}
	
	/**
	 * processURL is responisble for controlling the get request comming from the client.
	 * 
	 * @param content is the «get www.uib.no» request that is processed here.
	 * @return ArrayList<String> that is used for getHttpHeaders(), if errors are found null i returned.
	 */
	private ArrayList<String> processURL(String content) {
		String[] strArr;
		ArrayList<String> returnUrl = new ArrayList<>();
		
		if(!content.isEmpty()) {
			 strArr = content.split(" ");
			 if(strArr.length < 2) {
				 // returns null if just the GET command arrives.
				 System.out.println(getThreadID() + "Missing argument, returning null");
				 return returnUrl;
			 } else if(strArr[1].length() <= 4) {
				 // returns null if the request URI is less the 5 char.
				 System.out.println(getThreadID() + "To short URL, returning null");
				 return returnUrl;
			 } else {
				 String address = strArr[1];
				 
				 if(strArr[1].toUpperCase().startsWith("HTTPS://")) {
					 HTTPS_MODE = true;
					 int i = address.toUpperCase().indexOf("HTTPS://");
					 address = address.substring(i+8);
					
				 } else if (strArr[1].toUpperCase().startsWith("HTTP://")){
					 HTTPS_MODE = false;
					 int i = address.toUpperCase().indexOf("HTTP://");
					 address = address.substring(i+7);	 
				 } 
				 System.out.printf("%sUsing %s %s\n", getThreadID(), HTTPS_MODE ? "[HTTPS]" : "[HTTP]", address);
				 if(address.contains("/")) {
					 if(address.indexOf("/") == address.length()-1) {
						 // return just address with tailing /
						 returnUrl.add(address);
						 return returnUrl;
					 } else {
						 // Return address and path.
						 String[] string = address.split("/");
						 int index = address.indexOf("/");
						 // adds the address
						 returnUrl.add(string[0]);
						 // adds the path
						 returnUrl.add(address.substring(index));
						 return returnUrl;
					 }
				 } else {
					 returnUrl.add(address);
					 return returnUrl;
				 }
			 }
		} 
		return returnUrl;
	}

	/**
	 * Responsible for opening and closing all the TCP connections to the webservers.
	 * And is getting all the headers.
	 * 
	 * @param urlstring webside with path to connect to 
	 * @return all the headers from the requested webpage.
	 * @throws Exception
	 */
	private ArrayList<String> getHttpHeaders(ArrayList<String> urlstring) throws Exception {

		String hostname = urlstring.get(0).toString().trim();
		ArrayList<String> data = new ArrayList<>();
		BufferedReader tcpIn;
		boolean retriving = true;
		PrintWriter writeTcpToServer;
		
		try {
			
			if(HTTPS_MODE) {
				SSLSocketFactory sslfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				sslsocket = (SSLSocket) sslfactory.createSocket(hostname, HTTPS_PORT);
				writeTcpToServer = new PrintWriter(new OutputStreamWriter(sslsocket.getOutputStream()));
			} else {
				clientSocket = new Socket(hostname, HTTP_PORT);
				writeTcpToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			}
			
			if(urlstring.size() > 1) {
				String path = urlstring.get(1).trim();
				System.out.println(getThreadID() + "Sending a TCP Request for " + hostname + path);
				if(HTTPS_MODE) {
					writeTcpToServer.print("HEAD https://"+ hostname + path + " " + HTTP_VERSION + "\r\n");
				} else { 
					writeTcpToServer.print("HEAD http://"+ hostname + path + " " + HTTP_VERSION + "\r\n");
				}
			} else {
				System.out.println(getThreadID() + "Sending a TCP Request for " + hostname);
				writeTcpToServer.print("HEAD / " + HTTP_VERSION + "\r\n");
			}
			writeTcpToServer.print("User-Agent: " + USER_AGENT+"\r\n");
			writeTcpToServer.print("Host: " + hostname +"\r\n");
			writeTcpToServer.print("Connection: close\r\n");
			writeTcpToServer.print("\r\n");
			writeTcpToServer.flush();
		}
		catch (UnknownHostException e) {
			System.out.println(getThreadID() + "Unknown hostname");
			data.clear();
			data.add("RST");
			clientControlMessages(data, packet.getAddress(), packet.getPort());
			return null;
		}  

		if(HTTPS_MODE) {
			 tcpIn = new BufferedReader(new InputStreamReader(sslsocket.getInputStream()));
		} else {
			 tcpIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		}

		try {
		    while(retriving) {
		    		String header = tcpIn.readLine();
		    		data.add(header);
		    		if(header.isEmpty())
		    			retriving = false;
		    }
		} catch (NullPointerException e) {
			data.clear();
			data.add("RST");
			clientControlMessages(data, packet.getAddress(), packet.getPort());
			return null;
		}
		if(clientSocket != null)
			clientSocket.close();
		if(sslsocket != null) 
			sslsocket.close();
	    return data;
		
	} 
}
