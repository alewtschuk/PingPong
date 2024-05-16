package hw1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

public class PingPong implements Serializable {
    private static int port;
    private static String host;
    private static String flag; //Is it client or server

    /**
     * Empty object to simulate ping pong ball
     */
    public static class Ball extends Object implements Serializable {}

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
     * Starts the client for each new client instance
     * @param host hostname to connect to
     * @param port port to connect to
     */
    private static void startClient(String host, int port){
        System.out.println("-----------Client Setup Begin-----------\n" + "Host: " + host + "\nPort: " + port);
        
        
        System.out.println("Creating ball...");
        Ball ball = new Ball();
        System.out.println("Ball created!");


        String playerAssignment = null;
        String otherAssignement = null;

        try(Socket server = new Socket(host, port);) { //Settup the client socket with try resource to ensure leak closed
            ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(server.getInputStream());

            while(true){ //Keep client running till termination
                PlayerAssignments assignments = distributedToss(out, in, ball, flag); //Assign ping or pong
                playerAssignment = assignments.playerAssignment;
                otherAssignement = assignments.otherAssignment;
                play(out, in, ball, playerAssignment, otherAssignement, flag); //Play game
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in startClient()\nGame Terminated.\nThanks for playing!");
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
            System.out.println(flag + ": My result is: " + myNum + "\n" + " Their result is: " + theirNum);

            if (myNum > theirNum){ //Plays as ping
                System.out.println(flag + ": Coin toss terminated. I play as ping\n");
                out.writeObject(ball); //Sends ball with result
                out.flush();
                System.out.println(flag + " sent ping");
                return new PlayerAssignments("ping", "pong");
            } else if (myNum < theirNum ){ //Plays as pong
                System.out.println(flag + ": Coin toss terminated. I play as pong\n");
                out.writeObject(ball); //Sends ball with result
                out.flush();
                System.out.println(flag + " sent pong");
                return new PlayerAssignments("pong", "ping");
            } else {
                System.out.println("Result tied for client and server. Coin toss repeating");
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
    private static void play(ObjectOutputStream out, ObjectInputStream in, Ball ball, String playerAssignment, String otherAssignment, String flag) throws IOException, ClassNotFoundException, InterruptedException{
        while(true){
            System.out.println(flag + " recieved: " + otherAssignment);
            switch (playerAssignment) {
                case "ping":
                    out.writeObject(ball);
                    out.flush();
                    //Add delay if too fast
                    Thread.sleep(1500);
                    System.out.println(flag + ": " + playerAssignment);
                    break;

                case "pong":
                    out.writeObject(ball);
                    out.flush();
                    //Add delay if too fast
                    Thread.sleep(1500);
                    System.out.println(flag + ": " + playerAssignment);
                        break;

                default:
                    System.err.println("Error in play()\nGame Terminated.\nThanks for playing!");
                    break;
            }
        }
    }

    /**
     * Method to start the server using the host and port number to setup the connection. Runs till server termination
     * @param host hostname to connect to 
     * @param port port to connect to
     * @throws IOException
     */
    private static void startServer(String host, int port) throws IOException{
        System.out.println("-----------Server Setup Begin-----------\n"  + "Host: " + host + "\nPort: " + port);

        int gameNum = 1;

        try {
            @SuppressWarnings("resource")
		    ServerSocket serverSocket = new ServerSocket(port);
		    System.out.println("Server: ready");

		    while (true) {
			    new ServerConnection(serverSocket.accept(), gameNum).start();
			    System.out.println("Server: Started game #" + gameNum);
			    gameNum++;
		    }
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Server: Error in startServer()\nGame Terminated.\nThanks for playing!");
        }
    }

	/**
	 * A thread that handles one client.
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

			try{
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
    
                while(true){
                    PlayerAssignments assignments = distributedToss(out, in, ball, flag);
                    playerAssignment = assignments.playerAssignment;
                    otherAssignement = assignments.otherAssignment;
                    play(out, in, ball, playerAssignment, otherAssignement, flag);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Server: Game " + gameNum + "\nGame Terminated.\nThanks for playing!");
            }
		
		}
	}

	public static void main(String argv[]) throws IOException
	{
        if (argv.length < 3) {
			System.err.println("Usage:  java PingPong <client|server> <serverHost> <port#>");
			System.exit(1);
		} else if (argv[0].equalsIgnoreCase("client") ){
            flag = argv[0];
            host = argv[1];
            port = Integer.parseInt(argv[2]);
            startClient(host, port);
		} else if (argv[0].equalsIgnoreCase("server")){
            flag = argv[0];
			host = "localhost";
            port = Integer.parseInt(argv[2]);
            startServer(host, port);
		} else {
            System.err.println("Valid input not detected");
            System.err.println("Usage:  java PingPong <client|server> <serverHost> <port#>");
            System.exit(1);
        }
	}
}
