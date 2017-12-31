import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

/**
 * Server program for the chat room, 
 * data structures in the outer class contain information on all client sessions 
 * currently running. 
 * {@link HandleSession}
 * @author jack
 * @version 1.0
 * @since 2017-12-09
 * @see Server.HandleSession
 */
public class Server {
	// the initial values below should be the same in the client program
	
	/**
	 * Integer number to represent port number. 
	 */
	private int portNumber = 5555;
	
	/**
	 * The number of clients, initialised. 
	 */
	private int numClients = 0;
	
	/**
	 * String to prompt user. 
	 */
	private String welcome = "Please type your username.";
	
	/**
	 * String to prompt user. 
	 */
	private  String accepted = "Your username is accepted.";
	
	/**
	 * ServerSocket for the method {@link Server#shutDown()}
	 * bound to port number to connect client and server. 
	 */
	private ServerSocket ss;
	
	/**
	 * Hash set to store client names, collection of unique elements order not guaranteed.
	 */
	private HashSet<String> clientNames = new HashSet<String>();
	
	/**
	 * Hash set to store printWriters, , collection of unique elements order not guaranteed.
	 */
	private HashSet<PrintWriter> clientWriters = new HashSet<PrintWriter>();
	
	/**
	 * Array of helpful commands for user to use, including to get a list of commands, log off and see how many clients are in the chat room. 
	 */
	private String[] commands = {"\\help - list of commands","\\quit - sign out","\\numberclients - how many clients in chat room",
			"\\servertime - how long has server been running","\\clienttime - how long have you been logged in", "\\ipaddress - ip address of server", 
			"\\clientnames - list of client names signed in", "\\afk - notify clients you are away from keyboard", "\\back - notify clients you are back after being afk", 
			"@'username' - for the 'username' of the person you wish to private message followed by message"};
	
	/**
	 * Long integer to store time server started. 
	 */
	private long serverStartTime;
	
	/**
	 * HashMap to assign client names to their outputStreams, allowing for private messaging.
	 */
	private HashMap<String, PrintWriter> clientWriterMap = new HashMap<String, PrintWriter>();
	
	/**
	 * HashMap to assign client names to their start time, for getting the clienttime.
	 */
	private HashMap<String, Long> clientTimeMap = new HashMap<String, Long>();

	/**
	 * HashMap to assign client names to their away status, for the afk and back commands.
	 */
	private HashMap<String, Boolean> clientAFKMap = new HashMap<String, Boolean>();
	
	/** 
	 * Main method calls {@link Server#start()}. 
	 * @param args command-line arguments
	 * @throws IOException simply terminate running server if input or output exception occured
	 */
	public static void main(String[] args) throws IOException {	
		Server server = new Server(); 
		server.start();
	}
	
	/**
	 * Initiates the server, 
	 * when this is run, server is ready for connections. 
	 * @throws IOException simply terminate running server if input or output exception occured
	 */
	void start() throws IOException {	
		
		// As soon as server starts, current time is captured for servertime command
		serverStartTime = System.currentTimeMillis();
		
		ss = new ServerSocket(portNumber);
		
		System.out.println("Echo server at "
			+ InetAddress.getLocalHost()+ " is waiting for connections ..." );
		
		Socket socket;
		Thread thread;
		
		try {
			while(true) {
				socket = ss.accept(); // listen and accept connection from client
				thread = new Thread(new HandleSession(socket));
				thread.start();
			}
		} 
		catch (Exception e)  {
			System.out.println(e.getMessage());
		}
		finally {
			shutDown(); 
		}
	}
	
	/**
	 * Closes socket and shuts down server. 
	 */
	public void shutDown() {
		
		try { 
			ss.close(); 
			System.out.println("The server is shut down.");	
		} 
		catch (Exception e) {
			System.err.println("Problem shutting down the server.");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Inner class to handle each client session. 
	 * @author jack
	 * @version 1.0
	 * @since 2017-12-09
	 */
	class HandleSession implements Runnable {
		
		/**
		 * Endpoint of the server for the two-way communication link with the client. 
		 */
		private Socket socket; // for a client
		
		/**
		 * Username of current client. 
		 */
		String name;
		
		/**
		 * Read text from character-input stream from client. 
		 */
		BufferedReader in = null;
		
		/**
		 * Prints to client text output stream. 
		 */
		PrintWriter out = null;
		
		/**
		 * To display time for each message. 
		 */
		private SimpleDateFormat sdf;
		
		/**
		 * Constructor method for {@link Server.HandleSession}. 
		 * @param socket client end-point of communication
		 */
		HandleSession(Socket socket) {
			this.socket = socket;
			sdf = new SimpleDateFormat("HH:mm:ss"); // format to display time
		}

		/**
		 * Runs all the methods for each client session, 
		 * creates the stream between server and client, 
		 * gets client's username,
		 * listens for messages from the client, 
		 * closes that client's connection if there is an error or 
		 * client logs out. 
		 */
		public void run() {
			try {
				createStreams();
				getClientUserName();
				listenForClientMessages();
			} 
			catch (IOException e) {
				System.out.println(e);
			}
			finally { // executes when try block exits, even for exceptions
				closeConnection();
			}
		} // end of run() in the class HandleSession
	
		/**
		 * Method to establish connections to clients 
		 * instance in of BufferedReader to be used for receiving messages from clients 
		 * and out instance of PrintWriter to send messages to clients. 
		 */
		private void createStreams() {
			
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				// have this in a for loop for all clientWriters
				clientWriters.add(out); // add to a HashSet
				System.out.println("Connection has been established");
				numClients++; // Increment number of clients to display on command
			} 
			catch (IOException e) {
				System.err.println("Exception in createStreams(): " + e);
			}		
		} // end of createStreams() in the class HandleSession

		/**
		 * Infinite loop to add Clients to hash set, 
		 * when username entered which is valid and does not already exist 
		 * will break out of loop, synchronised to protect against more than one user 
		 * choosing the same username at the same time.  
		 */
		private synchronized void getClientUserName() {
			
			while(true) {
				out.println(welcome); 
				out.flush(); // from server
				
				try { 
					name = in.readLine(); 
				} catch (IOException e) { // first input
					System.err.println("Exception in getClientUserName: " + e);
				}			
				
				if (name == null) return; // no response
				
				// Better idea would be to import string utils package from apache commons to 
				// check for all-whitespace usernames but we are only allowed to use the java packages for this assignment
				synchronized(clientNames) {
					if (!clientNames.contains(name)) { // makes sure name is not already taken
						// makes sure name is not an empty string or null and between 1-19 characters
						if(name != null && !name.isEmpty() && name.length() < 20) { 
							clientNames.add(name); // new username
							clientWriterMap.put(name, out); // assign name key to printwriter for that client
							break; // from the while loop reading lines
						}
					}
				}
				out.println("Sorry, this username is unavailable"); 
				out.flush();
				// continue the loop
			}
			out.println(accepted + "Please type messages."); 
			out.flush(); // otherwise the client may not see the message
			System.out.println(name + " has entered the chat.");
			clientTimeMap.put(name, System.currentTimeMillis());
		}	// end of getClientUserName() in the class HandleSession
		
		/**
		 * Method to handle messages from client, 
		 * if client types '\quit' will log out, 
		 * if client types anything else starting with '\',
		 * will see array of commands,  
		 * else it will broadcast message. 
		 * @throws IOException simply terminate running server if input or output exception occured
		 */
		private void listenForClientMessages() throws IOException {
			
			String line; // input from a remote client
			
			while(in != null) {
				
				line = in.readLine(); // from the client
				
				if((line == null) || (line.equals("\\quit"))) break; // no response
				
				if(line.startsWith("\\")) { //a command returns false only if client quits
					if (!processClientRequest(line)) return;
				}
				
				// Private messaging
				if(line.startsWith("@")) {
					try {
						String[] lineArray = line.split(" ", 2); // split line in two at the first space
						String recipient = lineArray[0].substring(1); // recipient name is the first part of the line minus the @ symbol
						String message = lineArray[1]; // message is the rest of the line
				    	privateMessage(message, recipient);
					} catch (ArrayIndexOutOfBoundsException e) {
						System.err.println("Invalid private messaging request");
						System.err.println(e.getMessage());
					}
				}
				
				else if (!line.startsWith("\\") && !line.isEmpty()){ // Don't broadcast when a user types a command or when they don't input any text
					broadcast(line);
				}
			}
		} // end of listenForClientMessages() in the class HandleSession
		
		/**
		 * Removes connections with clients.
		 */
		void closeConnection() {
			
			if(name != null) {
				broadcast(name + " has left the chat.");
				clientNames.remove(name);
				}
				if (out != null) {
				clientWriters.remove(out);
			}
			try { 
				socket.close(); 
				System.out.println("Connection has been closed.");
				numClients--; // decrement number of clients to display on command
			} 
			catch (IOException e) {
				System.err.println("Exception when closing the socket");						
				System.err.println(e.getMessage());
			}
		} // end of closeConnection() in the class HandleSession
		
		/**
		 * Send the message out to all the clients in the chat room and print to server console, 
		 * synchronised to protect from interference from messages broadcasted at same time. 
		 * @param message message to be broadcast to all clients
		 */
		private synchronized void broadcast(String message) {
			
			for(PrintWriter writer : clientWriters) {
				
				if(writer != null) {

					// Prints "You said to the console of the user who sent the message"
					// And the user's name + "said:" to everybody else
					if(writer == clientWriterMap.get(name)) {
						String time = sdf.format(new Date());
						writer = clientWriterMap.get(name);
						writer.println(time + " You: " + message);
					} else {
						String time = sdf.format(new Date());
						writer.println(time + " " + name + ": " + message);
					}
					
					writer.flush();
				}
			}
			String time = sdf.format(new Date());
			System.out.println(time + " " + name + ": " + message); //server's screen
		}
		
		/**
		 * Method to send private messages between clients 
		 * will notify if recipient is currently AFK. 
		 * @param message private message to be sent
		 * @param recipient client who receives private message
		 */
		private synchronized void privateMessage(String message, String recipient) {

			if(clientAFKMap.get(recipient) != null && clientAFKMap.get(recipient) == true) { // notify sender if recipient is afk
				out.println("Recipient is currently away from keyboard, they will see the message when they return");
				out.flush();
			}

			String time = sdf.format(new Date()); // set the time
			out.println(time + " (PM >> " + recipient + ") You: " + message);
			out.flush();
			
			for(String client : clientNames) {
				
				if (client.equals(recipient)) {

					// Print message to this client
					System.out.println(time + "Private message sent from " + name + " to " + recipient + ": " + message); // notify server of private message (can comment out/remove)
					
					PrintWriter privateOut = clientWriterMap.get(recipient); // set the printwriter to the one associated with the recipient's name
					privateOut.println(time + " " + "(private)" + name + ": " + message); // print message to one client
					privateOut.flush();
				}
			}
		}
		
		/**
		 * Method to answer client queries:  
		 * log client out, print list of commands, how many clients in the chat, how long the server/client has been runnning, ip address of server, afk and back. 
		 * @param command command typed by client beginning in "\"
		 * @return true once command client has requested has been processed
		 * @throws UnknownHostException thrown if IP address of server could not be determined
		 */
		boolean processClientRequest(String command) throws UnknownHostException { // to get InetAddress.getLocalHost()
			
			long endTime; // Long integer to represent the current time
			
			if(command.equals("\\quit")) return false; // Client logs out
			
			if(command.equals("\\help")) {
				for(String c : commands) {
					out.println("Command " + c); 
					out.flush();
				}
			} // Prints list of commands
			
			if(command.equals("\\numberclients")) {
				out.println("Number of clients: " + numClients); 
				out.flush();
				}//how many clients currently online
			
			if(command.equals("\\servertime")) { 
				endTime = System.currentTimeMillis(); // set the end time when command called
				out.println("Server has been running for: " + getTime(serverStartTime, endTime));
				out.flush();
			} // how long has the server been running for, current time - start time
			
			if(command.equals("\\clienttime")) {
				endTime = System.currentTimeMillis(); // set the end time when command called
				out.println("You have been logged in for: " + getTime(clientTimeMap.get(name), endTime));
				out.flush();
			} // how long the client has been in the chat room for, current time - start time
			
			if(command.equals("\\ipaddress")) {
				out.println("IP address of server: " + InetAddress.getLocalHost()); 
				out.flush();
			}// the IP address of the server
			
			if(command.equals("\\clientnames")) { // print usernames of all clients
				for(String client : clientNames) {
					out.println(client);
					out.flush();
				}
			}

			if(command.equals("\\afk")) {

				if(clientAFKMap.get(name) == null || !clientAFKMap.get(name)) { //only notify once while away

					for(PrintWriter writer : clientWriters) {

						if(writer != clientWriterMap.get(name)) { 

							clientAFKMap.put(name, true);
							writer.println(name + " is away from keyboard");
							writer.flush();
						}	
					}
					System.out.println(name + " is away from keyboard");
				}
			} // notify everyone that you are away from keyboard

			if(command.equals("\\back")) {

				if(clientAFKMap.get(name)) { // only notify that they are back if they are afk

					for(PrintWriter writer : clientWriters) {

						if(writer != clientWriterMap.get(name)) {

							clientAFKMap.put(name, false);
							writer.println(name + " is back");
							writer.flush();
						}	
					}
					System.out.println(name + " is back");
				} 
			} // notify everyone that you are back

			return true;
		}
		
		/**
		 * Print time in a clear, easy to read format.
		 * @param startTime the time to be subtracted from endTime
		 * @param endTime the current time, subtract startTime to get duration
		 * @return String
		 */
		String getTime(long startTime, long endTime) {

			long time = endTime - startTime;
			
			long days = TimeUnit.MILLISECONDS.toDays(time);
			time -= TimeUnit.DAYS.toMillis(days);

			long hours = TimeUnit.MILLISECONDS.toHours(time);
			time -= TimeUnit.HOURS.toMillis(hours);

			long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
			time -= TimeUnit.MINUTES.toMillis(minutes);

			long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
				
			if (days > 0) return days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds";
			else if (hours > 0) return hours + " hours, " + minutes + " minutes, " + seconds + " seconds";
			else if (minutes > 0) return minutes + " minutes, " + seconds + " seconds";
			else return seconds + " seconds";
		}
	} // end of the class HandleSession
} // end of the class ServerWithClient