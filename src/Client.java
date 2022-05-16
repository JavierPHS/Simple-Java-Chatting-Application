import java.io.*;
import java.net.*;

public class Client {

	private String ip;
	private int port;
	private Socket socket;
	
	public Client(String clientIP, int clientPort) {
		ip = clientIP;
		port = clientPort;
		
		try {
			socket = new Socket(ip, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//call to return Client IP
	public String getIP() {
		return ip;
	}
	
	//call to return Client Port
	public int getPort() {
		return port;
	}
	
	//call to return Client Socket
	public Socket getSocket() {
		return socket;
		
	}
}
