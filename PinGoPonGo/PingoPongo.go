package main

import (
	"encoding/gob"
	"flag"
	"fmt"
	"math/rand"
	"net"
	"os"
	"strconv"
	"time"
)

const netType string = "tcp"

type PingPong struct {
	netType, host, port, address, flag string
}

type Ball struct{}

type PlayerAssignments struct {
	playerAssignment, otherAssignment string
}

func startClient(pingPongClient PingPong) {
	fmt.Println("-----------Client Setup Begin-----------\nHost: " + pingPongClient.host + "\nPort: " + pingPongClient.port)
	fmt.Println("Creating ball...")
	ball := Ball{}
	fmt.Println("Ball created...")

	//playerAssignment, otherAssignment := "",""

	serverConnection, err := net.Dial(pingPongClient.netType, pingPongClient.address)
	if err != nil {
		fmt.Println(err)
		fmt.Println("Error in startClient()\nGame Terminated\nThanks for playing!")
	}
	defer serverConnection.Close() //Close connection when done

	for true {
		var assignments PlayerAssignments = distributedToss(serverConnection, ball, pingPongClient)
		// playerAssignment, otherAssignment := assignments.playerAssignment, assignments.otherAssignment
		play(serverConnection, ball, assignments, pingPongClient)
	}
}

func distributedToss(serverConnection net.Conn, ball Ball, pingPongStruct PingPong) PlayerAssignments {
	// var randNum int = rand.Int()
	var myNum int = rand.Intn(2)
	fmt.Println("My number is: " + strconv.Itoa(myNum))
	serverConnection.Write([]byte(strconv.Itoa(myNum))) //Sends number result to other party

	var buf []byte = make([]byte, 1) //Buffer to read from server
	fmt.Println("Length of exampleSlice:", len(buf))
	_, err := serverConnection.Read(buf) //Reads number result from other party
	if err != nil {
		fmt.Println(err)
		fmt.Println("Error in distributedToss()\nGame Terminated\nThanks for playing!")
	}
	theirNum := int(buf[0])
	fmt.Println(pingPongStruct.flag + ": My result is: " + strconv.Itoa(myNum) + "\nTheir result is: " + strconv.Itoa(theirNum))

	gob.Register(Ball{})                        // Register Ball type for encoding
	encoder := gob.NewEncoder(serverConnection) // Create encoder

	switch myNum > theirNum {
	case true: //Play as ping case
		fmt.Println(pingPongStruct.flag + ": Coin toss terminated. I play as ping\n")
		encoder.Encode(ball) //Sends ball with result
		fmt.Println(pingPongStruct.flag + " sent ping")
		return PlayerAssignments{"ping", "pong"}
	case false: //Play as pong case
		fmt.Println(pingPongStruct.flag + ": Coin toss terminated. I play as pong\n")
		encoder.Encode(ball) //Sends ball with result
		fmt.Println(pingPongStruct.flag + " sent pong")
		return PlayerAssignments{"pong", "ping"}
	default: //Tied case, retoss coin
		fmt.Println("Result tied for client and server. Coin toss repeating")
		return distributedToss(serverConnection, ball, pingPongStruct)
	}
}

func play(serverConnection net.Conn, ball Ball, assignments PlayerAssignments, pingPongStruct PingPong) {
	gob.Register(Ball{})                        // Register Ball type for encoding
	encoder := gob.NewEncoder(serverConnection) // Create encoder

	for true {
		fmt.Println(pingPongStruct.flag + " recieved: " + assignments.otherAssignment)
		switch assignments.playerAssignment {
		case "ping":
			encoder.Encode(ball)
			time.Sleep(2 * time.Second)
			fmt.Println(pingPongStruct.flag + ": " + assignments.playerAssignment)
		case "pong":
			encoder.Encode(ball)
			time.Sleep(2 * time.Second)
			fmt.Println(pingPongStruct.flag + ": " + assignments.playerAssignment)
		default:
			fmt.Println("Error in play()\nGame Terminated\nThanks for playing!")
		}
	}
}

func startServer(pingPongServer PingPong) {
	serverSocket, err := net.Listen(pingPongServer.netType, pingPongServer.address)
	if err != nil {
		fmt.Println(err)
		fmt.Println("Error in startServer()\nGame Terminated\nThanks for playing!")
		return
	}
	defer serverSocket.Close() // Close connection when done

	gameNum := 0

	for {
		serverConnection, err := serverSocket.Accept()
		if err != nil {
			fmt.Println(err)
			fmt.Println("Error accepting client connection\nGame Terminated\nThanks for playing!")
			continue
		}
		gameNum++
		fmt.Println("Game " + strconv.Itoa(gameNum) + " started")

		go run(serverConnection, pingPongServer, gameNum)
		if run != 0 {

		}
	}
}

func run(serverConnection net.Conn, pingPongServer PingPong, gameNum int) int {
	defer serverConnection.Close() // Ensure the connection is closed when done
	fmt.Println("Running game", gameNum)

	var assignments PlayerAssignments = distributedToss(serverConnection, Ball{}, pingPongServer)
	fmt.Println("Assignments for game", gameNum, ":", assignments)

	play(serverConnection, Ball{}, assignments, pingPongServer)
	fmt.Println("Game", gameNum, "finished")
	return 1
}

func main() {
	client := flag.Bool("client", false, "Starts program in client mode")
	server := flag.Bool("server", false, "Starts program in server mode")
	serverHost := flag.String("host", "localhost", "Host to connect to")
	serverPort := flag.Int("port", 8080, "Port to connect to")

	flag.Parse()

	if len(os.Args) == 0 {
		fmt.Println("Usage: ./PingoPongo <-client|-server> <-host host> <-port port>")
		os.Exit(1)
	}

	if *client {
		pingPongClient := PingPong{
			netType: netType,
			host:    *serverHost,
			port:    strconv.Itoa(*serverPort),
			address: *serverHost + ":" + strconv.Itoa(*serverPort),
			flag:    "Client",
		}
		startClient(pingPongClient)
	} else if *server {
		//*serverHost = "localhost"
		pingPongServer := PingPong{
			netType: netType,
			host:    *serverHost,
			port:    strconv.Itoa(*serverPort),
			address: *serverHost + ":" + strconv.Itoa(*serverPort),
			flag:    "Server",
		}
		startServer(pingPongServer)
	} else {
		fmt.Println("Valid input not detected\nUsage: ./PingoPongo <-client|-server> <-host host> <-port port>")
		os.Exit(1)
	}
}
