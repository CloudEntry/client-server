import java.io.*;
import java.net.*;

/**
 * Client program for the chat room.
 * {@link ClientInstance}
 * @author jack
 * @version 1.0
 * @since 2017-12-09
 * @see ClientInstance
 */
public class Client {
	
	/**
	 * Main method. 
	 * @param args command-line arguments
	 * @throws Exception simply terminate running clientInstance when exception is thrown
	 */
	public static void main(String[] args) throws Exception {
		ClientInstance client = new ClientInstance();
		client.run();
	}
}

/**
 * Instance of each client in the chat room. 
 * @author jack
 * @version 1.0
 * @since 2017-12-09
 */
class ClientInstance {
	// the initial values below should be the same in the server program
	
	/**
	 * Integer number to represent port number. 
	 */
	private int portNumber = 5555;
	
	/**
	 * String to prompt user. 
	 */
	private String welcome = "Please type your username.";
	
	/**
	 * String to prompt user. 
	 */
	private String accepted = "Your username is accepted.";
	
	/**
	 * Endpoint of the client for the two-way communication link with the server.
	 */
	private Socket socket = null;
	
	/**
	 * Read text from character-input stream from client. 
	 */
	private BufferedReader in;
	
	/**
	 * Prints to client text output stream.
	 */
	private PrintWriter out;
	
	/**
	 * Client ability to chat initally set to false. 
	 */
	private boolean isAllowedToChat = false;
	
	/**
	 * Server connected to client intially set to false. 
	 */
	private boolean isServerConnected = false;
	
	/**
	 * String to store the username of the client. 
	 */
	private String clientName;
	
	/**
	 * Runs all the methods for each instance of the client, 
	 * established connection to the server, 
	 * handles the outgoing and incoming messages to and from the server. 
	 */
	public void run() { 
		establishConnection();
		handleOutgoingMessages();
		handleIncomingMessages();
	}
	
	/**
	 * Asks client for the ip address of the server they wish to connect to 
	 * and generates the streams between them and that server. 
	 */
	private void establishConnection() {
		
		String serverAddress = getClientInput("What is the address of the server that you wish to connect to?");
		// Socket used by client to connect to server
		try {
			socket = new Socket(serverAddress, portNumber);
			in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			isServerConnected = true;
		} 
		catch (IOException e) {
			System.err.println("Exception in handleConnection(): " + e);
		}
		handleProfileSetUp();
	} // end of handleConnection() in the class ClientInstance
		
	/**
	 * Asks user for username, when accepted 
	 * allows them to send messages to the server. 
	 */
	private void handleProfileSetUp() {
		
		String line = null;
		
		while (!isAllowedToChat) {
			try {
				line = in.readLine();
			}
			catch (IOException e) {
				System.err.println("Exception in handleProfileSetUp:" + e);
			}
			if (line.startsWith(welcome)) {
				out.println(getClientInput(welcome));
			}
			else if (line.startsWith(accepted)) {
				isAllowedToChat = true; // username ok
				System.out.println(accepted + " You can type messages.");
				System.out.println("To see a list of server commands, type \\help.");
			}
			else System.out.println(line);
		}
	}	// end of handleProfileSetUp()	in the class ClientInstance	

	/**
	 * Create thread to send messages to the server. 
	 */
	private void handleOutgoingMessages() { 
		
		Thread senderThread = new Thread(new Runnable() { //Sender thread
			public void run() {
				while(isServerConnected) {
					out.println(getClientInput(null));
				}
			}
		});
		senderThread.start();
	} // end of handleOutgoingMessages() in the class ClientInstance

	/**
	 * Get messages from the server via BufferedReader. 
	 * @param clientInstructions to ask client for server ip address they wish to connect to
	 * @return message 
	 */
	private String getClientInput(String clientInstructions) {
		
		String message = null;
		
		try {
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in));
			if (clientInstructions != null) {
				System.out.println(clientInstructions);
			}
			
			message = reader.readLine();
			
			if (!isAllowedToChat) {
				clientName = message;
			}
		}
		catch (IOException e) {
			System.err.println("Exception in getClientInput(): " + e);
		}
		return message;
	} // end of getClientInput() in the class ClientInstance

	/**
	 * Handles the messages received from the server, 
	 * creates listener thread and 
	 * informs client if server has been disconnected. 
	 */
	private void handleIncomingMessages() { // Listener thread
		Thread listenerThread = new Thread(new Runnable() {
			public void run() {
				while (isServerConnected) {
					String line = null;
					try {
						line = in.readLine();
						if (line == null) { // server isn't responsive
							isServerConnected = false;
							System.err.println("Disconnected from the server");
							closeConnection();
							break;
						}
						System.out.println(line);
					}
					catch(IOException e) {
						isServerConnected = false;
						System.err.println("IOE in handleIncomingMessages()");
						break;
					}
				}
			}
		});
		listenerThread.start();			
	} // end of handleIncomingMessages() in the class ClientInstance

	/**
	 * Closes the connection to the server. 
	 */
	void closeConnection() {
		try { 
			socket.close(); 
			System.exit(0); // finish the client program
		} 
		catch(IOException e) {
			System.err.println("Exception when closing the socket");						
			System.err.println(e.getMessage());
		}
	} // end of closeConnection() in the class ClientInstance
} // end of the class ClientInstance