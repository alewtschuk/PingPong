package Java.Servent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PingPongServent implements Serializable {
    private static String name; //Is it client or server
    private Server server;
    private Client client;
    
    

    /**
     * Empty object to simulate ping pong ball
     */
    public static class Ball extends Object implements Serializable {}

    public PingPongServent(String myName, int myPort, String serverHostname, int serverPort){
        name = myName;
        server = new Server(myPort);
        client = new Client(serverHostname, serverPort);
    }

    public void runServent(){
        server.start();
        client.runClient();
    }

    private class Server extends Thread{
        private ServerSocket serverSocket;
        private Set<String> clients = new HashSet<>();
        //private int gameNum = 1;
        int gameAsServer = 0;

        /**
         * Creates server socket on port number 
         * @param port port for server
         */
        public Server(int port){
            try {
		        serverSocket = new ServerSocket(port);
		        System.out.println("-----------Servent Setup Begin-----------\n");
                System.out.println("Servent" + name + ": up and running on port " + port + " " + InetAddress.getLocalHost());
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Servent: Error in Server()\n");
            }
        }

        public void run(){
            Socket client;
            System.out.println("Servent " + name + ": main thread " + getName());
            try{
                while(true){
                    client = serverSocket.accept();
                    String clientIdentifier = client.getInetAddress().getHostName() + client.getPort();
                    System.out.println(
                        "Servent " + name + ": Recieved connect from " + client.getInetAddress().getHostName()
                        + " [ " + client.getInetAddress().getHostAddress() + "," + client.getPort() + "] ");
                        synchronized (this) {
                            if (!clients.contains(clientIdentifier)) {
                                clients.add(clientIdentifier);
                                // Increment gameAsServer for each unique client
                                gameAsServer++;
                            }
                        }
                    System.out.println("Servent: Starting game #" + gameAsServer + " as server");
                    new ServerConnection(client, gameAsServer).start();
                }
            } catch (IOException e){
                e.printStackTrace();
                System.err.println(e);
                System.err.println("Servent: Error in run() in class Server");
            }
        }

    }

    private class Client{
        private String host;
        private int port;
        private int gameNum = 1;

        public Client(String host, int port){
            this.host = host;
            this.port = port;
        }

        public void runClient(){
            //System.out.println("Creating ball...");
            Ball ball = new Ball();
            //System.out.println("Ball created!");


            String playerAssignment = null;
            String otherAssignement = null;
            String role = "<Client>";

            try(Socket server = new Socket(host, port);) { //Settup the client socket with try resource to ensure leak closed
                ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());

                System.out.println("Servent " + name + ": Connecting to " + host + ":" + port + " as client");


                while(true){ //Keep client running till termination
                    System.out.println("Servent" + name + ": Started game #" + gameNum + " as client \n");
                    PlayerAssignments assignments = distributedToss(out, in, ball, name); //Assign ping or pong
                    playerAssignment = assignments.playerAssignment;
                    otherAssignement = assignments.otherAssignment;
                    play(out, in, ball, playerAssignment, otherAssignement, name, gameNum, role); //Play game
                    gameNum++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Servent " + name + "Error in runClient() in class Client.\nGame Terminated.\nThanks for playing!");
            }
        }
    }

    /**
     * Class to store player assignment values
     */
    public static class PlayerAssignments {
        public String playerAssignment;
        public String otherAssignment;
    
        public PlayerAssignments(String playerAssignment, String otherAssignment) {
            this.playerAssignment = playerAssignment;
            this.otherAssignment = otherAssignment;
        }
    }

    /**
     * Contains the logic for the distributed coin toss. Written to be able to be called and resused regardles of server or client call
     * @param out the ObjectOutputStream
     * @param in the InputObjectStream
     * @param ball the ball object to be written to and sent over the stream
     * @param flag the string holding "client" or "server" identifier
     * @return PlayerAssignments object holding the strings with the correct assignment
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static PlayerAssignments distributedToss(ObjectOutputStream out, ObjectInputStream in, Ball ball, String flag) throws IOException, ClassNotFoundException{
        try {
            Random rand = new Random();
            int myNum = rand.nextInt(2);
            out.writeObject(myNum); //Sends number result to other party
            out.flush();

            int theirNum = (int) in.readObject(); //Reads other party's number
            System.out.println("Servent" + flag + ": My result is: " + myNum + "\n" + " Their result is: " + theirNum);

            if (myNum > theirNum){ //Plays as ping
                System.out.println("Servent" + flag + ": Coin toss terminated. I play as ping\n");
                out.writeObject(ball); //Sends ball with result
                out.flush();
                System.out.println("Servent" + flag + " sent ping");
                return new PlayerAssignments("ping", "pong");
            } else if (myNum < theirNum ){ //Plays as pong
                System.out.println("Servent" + flag + ": Coin toss terminated. I play as pong\n");
                out.writeObject(ball); //Sends ball with result
                out.flush();
                System.out.println("Servent" + flag + " sent pong");
                return new PlayerAssignments("pong", "ping");
            } else {
                System.out.println("Result tied. Coin toss repeating");
                return distributedToss(out, in, ball, flag); //Recursive call till result is found
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error in distributedToss()\nGame Terminated.\nThanks for playing!");
            return new PlayerAssignments("error", "error");
        }
    }

    /**
     * Play function that plays the ping pong game. Written to be called by server or client
     * @param out the ObjectOutputStream
     * @param in the InputObjectStream
     * @param ball the ball object to be written to and sent over the stream
     * @param playerAssignment string with player assignment value 
     * @param otherAssignment string with opponent assingment value
     * @param flag string holding "client" or "server" identifier
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    private static void play(ObjectOutputStream out, ObjectInputStream in, Ball ball, String playerAssignment, String otherAssignment, String name, int gameNum, String role) throws IOException, ClassNotFoundException, InterruptedException{
        while(true){
            System.out.println("Game " + gameNum + " as " + role + ": " + "Servent" + name + " recieved: " + otherAssignment);
            switch (playerAssignment) {
                case "ping":
                    out.writeObject(ball);
                    out.flush();
                    //Add delay if too fast
                    Thread.sleep(2500);
                    System.out.println("Game " + gameNum + " as " + role + ": " + "Servent" + name + " sent: " + playerAssignment);
                    break;

                case "pong":
                    out.writeObject(ball);
                    out.flush();
                    //Add delay if too fast
                    Thread.sleep(2500);
                    System.out.println("Game " + gameNum + " as " + role + ": " + "Servent" + name + ": " + playerAssignment);
                        break;

                default:
                    System.err.println("Error in play()\nGame Terminated.\nThanks for playing!");
                    break;
            }
        }
    }


	/**
	 * A thread that handles one connection.
	 */
	private static class ServerConnection extends Thread {
		private Socket client;
		private int gameNum;

		ServerConnection(Socket client, int gameNum) throws SocketException {
            this.client = client;
            this.gameNum = gameNum;
		}


        /**
         * Run method for the server, executed by the thread
         */
		public void run(){
            String playerAssignment = null;
            String otherAssignement = null;
            Ball ball = new Ball();
            String role = "<Server>";

			try{
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
    
                while(true){
                    PlayerAssignments assignments = distributedToss(out, in, ball, name);
                    playerAssignment = assignments.playerAssignment;
                    otherAssignement = assignments.otherAssignment;
                    play(out, in, ball, playerAssignment, otherAssignement, name, gameNum, role);
                    System.out.println("Game " + gameNum + " as Server: Servent " + name + "...");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Servent: Game " + gameNum + "\nGame Terminated.\nThanks for playing!");
            }
		
		}
	}

	public static void main(String argv[]) throws IOException
	{
        if (argv.length < 3) {
			System.err.println("Usage:  java PingPongServent <myname> <myport> <server-hostname> <server-port#>");
			System.exit(1);
		} 

        String myName = argv[0];
        int myPort = Integer.parseInt(argv[1]);
        String serverHostname = argv[2];
        int serverPort = Integer.parseInt(argv[3]);
        PingPongServent servent = new PingPongServent(myName, myPort, serverHostname, serverPort);
        servent.runServent();

	}
}
