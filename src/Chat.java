import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.Map.Entry;


public class Chat {
	private int port;
	private String myIP;
	private ServerSocket serverSocket;
	private BufferedReader br;
	private List<Client> clientList;
	private Map<Client, DataOutputStream> clientMap;
	
	public Chat (int userPort) throws IOException {
        port = userPort;
        myIP = InetAddress.getLocalHost().getHostAddress();
        clientList = new ArrayList<Client>();
        br = new BufferedReader(new InputStreamReader(System.in));
        clientMap = new HashMap<Client, DataOutputStream>();
        serverSocket = new ServerSocket(port);
        
    }
	
	public void print(String e) {
		System.out.println(e);
	}
	
	public static void main(String[] args) {
        try {
        	//Accept listening port number from command line argument, "java Chat <port #>"
        	int userPort = Integer.parseInt(args[0]);
        	//check for valid port #
        	if (userPort < 1024 || userPort > 65535) {
        		System.out.println("Please use a valid port #");
        	}
        	else {
	        	//Boot up server with given port #
	            Chat chat = new Chat(userPort);
	            System.out.println("Welcome to the Chatting App\n");
	            chat.bootup();
	            chat.menu();
        	}
        }
        catch (Exception e) {
        }
    } //end main
	
	public void bootup() throws IOException {
		
		//Allow the program to handle multithreading for multiple Clients
		new Thread(() -> {
			while(true) {
				try {
					//Accept any Client trying to connect to us
					Socket clientSocket = serverSocket.accept();
					
					//Start new thread for each individual Client
					new Thread(new Connection(clientSocket)).start();
					
				} catch (IOException e) {
					
				}
			}
		}).start();
	}
	
	//Display all messages sent by various Clients
	private class Connection implements Runnable {
		private Socket clientSocket;
		
		public Connection(Socket socket) {
			clientSocket = socket;
		}
		
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				//Read all messages intercepted from Clients
				while(true) {
					String line = br.readLine();
					if (line == null) {
						return;
					}
					String[] arg = line.split(" ");
					//Handle case when Client tries to connect to you
					if (line.startsWith("connect")) {
						displayConnect(line);
					}
					//Handle case when Client wants to send you a message
					else if (line.startsWith("Message")) {
						displayMessage(line);
					}
					//Handle case when Client wants to terminate your connection
					else if (line.startsWith("terminate")) {
						displayTerminate(arg[1], arg[2]);
						terminateConnection(searchClient(arg[1], Integer.parseInt(arg[2])));
						removeClient(searchClient(arg[1], Integer.parseInt(arg[2])));
						br.close();
						return;
					}
				}
			} catch (IOException e) {
				System.out.println("Connection has been dropped");
			}
		}
	}
	
	public void displayConnect (String line) throws IOException {
		String[] arg = line.split(" ");
		String cIP = arg[1];
		int cPort = Integer.parseInt(arg[2]);
		System.out.println("New Client with IP: " + cIP + ", port#: " + cPort + " has connected\n");
		Client newClient = new Client(cIP, cPort);
		clientList.add(newClient);
		clientMap.put(newClient,  new DataOutputStream(newClient.getSocket().getOutputStream()));
		
	}
	
	
	public void menu() {
		Scanner scanner = new Scanner(System.in);
    	String line;
    	try {
    	while ((line = scanner.nextLine()) != null) {
    		if (line.equals("help")) {
    			helpCMD();
    		}
    		else if (line.equals("myip")) {
    			myipCMD();
    		}
    		else if (line.equals("myport")) {
    			myportCMD();
    		}
    		else if (line.startsWith("connect")) {
    			connectCMD(line);
    		}
    		else if (line.equals("list")) {
    			listCMD();
    		}
    		else if (line.startsWith("terminate")) {
    			terminateCMD(line);
    		}
    		else if (line.startsWith("send")) {
    			sendCMD(line);
    		}
    		else if (line.equals("exit")) {
    			exitCMD();
    			System.exit(0);
    			return;
    		}
    		else {
    			System.out.println("Unrecognized command. Type in 'help' for list of commands\n");
    		}
    	} //end while
    	} catch (UnknownHostException e) {
    		
    	} catch (IOException e) {
    		
    	}
	}
	
	public void helpCMD() {
    	print("\nhelp\t Displays information about the available user interface options or command manual.");
    	print("\nmyip\t Display the IP Address of this process");
    	print("\nmyport\t Display the port on which this process is listening for incoming connections");
    	print("\nconnect <destination> <port no>\t This command establishes a new TCP connection to the specified <destination> at the specified <port no>" +
    	"The <destination> is the IP Address of the computer.");
    	print("\nlist\t Display a numbered list of all the connections this process is a part of.");
    	print("\nterminate <connection id>\t This command will terminate the connection listed under the specified number when LIST is used to display all connections");
    	print("\nsend <connection id> <message>\t This will send a message to the host on the connection that is designated by the connection id when the 'list' command is used");
    	print("\nexit\t Close all connections and terminates this process\n");
    }
    
    public void myipCMD() throws UnknownHostException {
    	print("Your IP is: " + myIP + "\n");
    }
    
    public void myportCMD() {
    	print("Your port # is: " + port + "\n");
    }
    
    public void connectCMD(String line) throws IOException {
    	String[] arg = line.split(" ");
    	String clientIP;
    	int clientPort;
    	Socket clientSocket = null;
    	String message = null;
    	
    	//check for valid connect command format
    	if (arg.length != 3) {
    		print("Invalid connect command format, please type 'help' if you need assistance.\n");
    		return;
    	}
    	//check for valid IP address
    	if (!isValidIP(arg[1])) {
    		print("Invalid IP Address. Please try again.\n");
    		return;
    	}
    	//check for valid port #
    	if (!isValidPort(arg[2])) {
    		print("Invalid Port #. Please try again.\n");
    		return;
    	}
    	clientIP = arg[1];
    	clientPort = Integer.valueOf(arg[2]);
    	//check if the connection is unique
    	if (!isUniqueConnection(clientIP, clientPort)) {
    		print("Connection failed, the requested connection already exists/is your own connection\n");
    		return;
    	}
    	try {
    		clientSocket = new Socket(clientIP, clientPort);
    	} catch (IOException e) {
    		print("Connection failed\n");
    	}
    	
    	System.out.println("Connected to " + clientIP + ", " + clientPort + "\n");
    	Client client = new Client(clientIP, clientPort);
    	clientList.add(client);
    	clientMap.put(client,  new DataOutputStream(clientSocket.getOutputStream()));
    	message = "connect " + myIP + " " + port;
    	sendMessage(client, message);
    	
    }
    
    public void listCMD() {
    	//check if list is empty
    	if (clientList.isEmpty()) {
    		System.out.println("No clients connected.\n");
    	}
    	else {
    		System.out.println("id:   IP Address     Port No.");
    		for (int i = 0; i < clientList.size(); i++) {
    			Client client = clientList.get(i);
    			System.out.println((i + 1) + ":   " + client.getIP() + "     " + client.getPort());
    		}
    		System.out.println();
    	}
    }
    
    public void terminateCMD(String line) {
    	String[] arg = line.split(" ");
    	try {
    		//check for valid terminate command format
    		if(arg.length != 2) {
    			print("Invalid terminate command format, please type 'help' if you need assistance\n");
    			return;
    		}
    		int id = Integer.valueOf(arg[1]) - 1;
    		//check if the selected ID exists in our available connections, then terminate if it does
    		if (id >= 0 && id < clientList.size()) {
	    		Client client = clientList.get(id);
	    		String msg = "terminate " + myIP + " " + port;
	    		sendMessage(client, msg);
	    		System.out.println("You have terminated the connection with Client IP: " + client.getIP() + " Port: " + client.getPort() + "\n");
	    		terminateConnection(client);
	    		removeClient(client);
    		}
    		else {
    			print("Invalid Client ID. Use the 'list' command to see what connections are available\n");
    			return;
    		}
    		
    	} catch (NumberFormatException e) {
    		System.out.println("The connection ID you entered does not exist\n");
    	}
    }
    
    public void sendCMD(String line) {
    	String[] args = line.split(" ");
    	System.out.println();
    	//check for valid send command format
    	if (args.length < 3) {
    		print("Invalid send command format, please type 'help' if you need assistance\n");
    		return;
    	}
    	try {
    		int id = Integer.valueOf(args[1]) - 1;
    		//check if the selected ID exists in our available connections, then send message if it does
    		if (id >= 0 && id < clientList.size()) {
	    		String msg = "";
	    		for (int i = 2; i < args.length; i++) {
	    			msg += args[i] + " ";
	    		}
	    		Client client = clientList.get(id);
	    		clientMap.get(client).writeBytes("Message " + myIP + " " + port + " " + msg + "\r\n");
    		}
    		else {
    			print("Invalid Client ID. Use the 'list' command to see what connections are available\n");
    		}
    	} catch (NumberFormatException e) {
    		System.out.println("Invalid send command format, please ensure that the second value is valid connection ID\n");
    	} catch (IOException e) {
    		System.out.println(e);
    	}
    }
    
    public void exitCMD() {
    	try {
    		//Send every Client connection that you are terminating your connection with them
    		for (Client client : clientList) {
    			String msg = "terminate " + myIP + " " + port;
    			sendMessage(client, msg);
    			terminateConnection(client);
    		}
    		
    		//Close each Client output stream
    		for (Entry<Client, DataOutputStream> e : clientMap.entrySet()) {
    			e.getValue().close();
    		}
    		
    		serverSocket.close();
    		System.out.println("The application will now close. Goodbye.\n");
    	} catch (IOException e) {
    		System.out.println(e);
    	}
    }
    
    public void terminateConnection(Client client) {
    	try {
    		client.getSocket().close();
    		clientMap.get(client).close();
    	} catch (IOException e) {
    		System.out.println(e);
    	}
    }
    
    public void removeClient(Client client) {
    	clientList.remove(client);
    	clientMap.remove(client);
    }
    
    public Client searchClient(String cIP, int cPort) {
    	for (Client client : clientList) {
	    	if (client.getIP().equals(cIP) && client.getPort() == cPort) {
	    		return client;
			}
    	}
    	return null;
    } //end searchClient
	
    public void sendMessage(Client client, String message) {
    	try {
    		clientMap.get(client).writeBytes(message + "\r\n");
    	} catch (Exception e) {
    		System.out.println(e);
    	}
    } //end sendMessage
    
    public void displayMessage(String line) {
    	String[] arg = line.split(" ");
    	String cIP = arg[1];
		int cPort = Integer.parseInt(arg[2]);
		System.out.println("Message received from " + cIP);
		System.out.println("Sender's Port: " + cPort);
		System.out.print("Message: ");
		for(int i = 3; i < arg.length; i++) {
			System.out.print(arg[i] + " ");
		}
		System.out.println("\n");
    }
    
    public void displayTerminate(String cIP, String cPort) {
    	System.out.println("Client IP: " + cIP + " Port: " + cPort + " has terminated the connection\n");
    }
    
    public boolean isValidIP(String ip) {
    	try {
	    	String[] arg = ip.split("\\.");
	    	if (arg.length != 4) {
	    		return false;
	    	}
	    	for(int i = 0; i < 4; i++) {
	    		if(Integer.parseInt(arg[i]) < 1 || Integer.parseInt(arg[i]) > 255) {
	    			return false;
	    		}
	    	}
	    	return true;
    	} catch (NumberFormatException e) {
    		System.out.println(e);
    		return false;
    	}
    } //end isValidIP
    
    public boolean isValidPort(String prt) {
    	try {
    		if (Integer.parseInt(prt) < 1024 || Integer.parseInt(prt) > 65535) {
    			return false;
    		}
    		return true;
    	} catch (NumberFormatException e) {
    		return false;
    	}
    } //end isValidPort
    
    public boolean isUniqueConnection(String cIP, int cPort) {
    	return !isSelf(cIP, cPort) && isUniqueClient(cIP, cPort);
    } //end isUniqueConnection

	public boolean isUniqueClient(String cIP, int cPort) {
		return searchClient(cIP, cPort) == null;
	} //end isUniqueClient
	
	public boolean isSelf(String cIP, int cPort) {
		return myIP.equals(cIP) && port == cPort;
	} //end isSelf
	
}
