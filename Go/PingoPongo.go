package main

import (
	"encoding/gob"
	"fmt"
	"math/rand"
	"net"
	"os"
	"strconv"
	"time"
)

const netType string = "tcp"

// Global variables to hold the port and host
var port string
var host string
var mode string

// Empty ball struct to pass between client and server
type Ball struct{}

// Struct to hold the player assignments
type PlayerAssignments struct {
	playerAssignment string
	otherAssignment  string
}

// Method called on PlayerAssignments object to create filled PlayerAssignment Struct
func newPlayerAssignment(playerAssignment string, otherAssignment string) *PlayerAssignments {
	return &PlayerAssignments{
		playerAssignment: playerAssignment,
		otherAssignment:  otherAssignment,
	}
}

func startClient(host string, port string) {
	fmt.Println("-----------Client Setup Begin-----------\n" + "Host: " + host + "\nPort: " + port)
	fmt.Println("Creating ball...")
	var ball Ball = Ball{}
	fmt.Println("Ball created.")

	var assignments PlayerAssignments

	conn, err := net.Dial(netType, net.JoinHostPort(host, port))
	if err != nil {
		fmt.Println("Error connecting to server in startClient()", err)
		return
	} else {
		fmt.Println(mode + ": Connection established")
	}
	defer conn.Close()
	var encoder = gob.NewEncoder(conn)
	var decoder = gob.NewDecoder(conn)

	for {
		assignments = distributedToss(encoder, decoder, ball, mode)
		if assignments.playerAssignment == "" || assignments.otherAssignment == "" {
			fmt.Println("Error in distributedToss()")
			os.Exit(1)
		}
		err = play(encoder, ball, assignments, mode)
		if err != nil {
			fmt.Println("Server has disconnected. Game Terminated.\nThanks for playing!")
			os.Exit(0)
		}
	}
}

func distributedToss(encoder *gob.Encoder, decoder *gob.Decoder, ball Ball, mode string) PlayerAssignments {
	var myRand int = rand.Intn(2)
	fmt.Println("My random number: ", myRand)
	encoder.Encode(myRand)

	var thierNum int
	err := decoder.Decode(&thierNum)

	if err != nil {
		fmt.Println("Error decoding in distributedToss()", err)
		os.Exit(1)
	}
	fmt.Println("My number: ", myRand, "\n", "Their number: ", thierNum)
	switch myRand == thierNum {
	case true:
		fmt.Println("Restult tied for client and server. Coin toss repeadting...")
		return distributedToss(encoder, decoder, ball, mode)
	case thierNum < myRand:
		fmt.Println(mode + ": Coin toss terminated. I play as pong\n")
		encoder.Encode(ball)
		fmt.Println(mode + " sent pong")
		return *newPlayerAssignment("pong", "ping")
	case thierNum > myRand:
		fmt.Println(mode + ": Coin toss terminated. I play as ping\n")
		encoder.Encode(ball)
		fmt.Println(mode + " sent ping")
		return *newPlayerAssignment("ping", "pong")
	}
	return *newPlayerAssignment("", "")
}

func play(encoder *gob.Encoder, ball Ball, assignments PlayerAssignments, mode string) error {
	for {
		fmt.Println(mode + " recieved: " + assignments.otherAssignment)
		var err error
		switch assignments.playerAssignment {
		case "ping":
			if err = encoder.Encode(ball); err != nil {
				return err
			}
			time.Sleep(1 * time.Second)
			fmt.Println(mode + ": " + assignments.playerAssignment)
			break
		case "pong":
			if err = encoder.Encode(ball); err != nil {
				return err
			}
			time.Sleep(1 * time.Second)
			fmt.Println(mode + ": " + assignments.playerAssignment)
		default:
			fmt.Println("Error in play(). Invalid player assignment")
			break
		}
	}
}

func startServer(host string, port string) {
	fmt.Println("-----------Server Setup Begin-----------\n" + "Host: " + host + "\nPort: " + port)

	var gameNum int = 1

	server, err := net.Listen(netType, net.JoinHostPort(host, port))
	if err != nil {
		fmt.Println("Error starting server listener in startServer()", err)
		os.Exit(1)
	}
	fmt.Println("Server: READY")

	for {
		serverConnection, err := server.Accept()
		if err != nil {
			fmt.Println("Error accepting server connection in startServer()", err)
			os.Exit(1)
		}
		fmt.Println("Server: Connection accepted")
		go handleConnection(serverConnection, gameNum)
		gameNum++
	}
}

func handleConnection(serverConnection net.Conn, gameNum int) {
	fmt.Println("-----------Game " + strconv.Itoa(gameNum) + " Begin-----------")
	defer serverConnection.Close()
	var assignments PlayerAssignments
	var ball Ball = Ball{}
	var encoder = gob.NewEncoder(serverConnection)
	var decoder = gob.NewDecoder(serverConnection)
	for {
		assignments = distributedToss(encoder, decoder, ball, mode)
		var err = play(encoder, ball, assignments, mode)
		if err != nil {
			fmt.Println("Server: Game " + strconv.Itoa(gameNum) + "\nGame Terminated.\nThanks for playing!")
			break
		}
	}
}

func main() {
	if len(os.Args) < 4 {
		fmt.Println("Usage:  go run PingPong.go <client|server> <serverHost> <port#>")
		os.Exit(1)
	}

	host = os.Args[2]
	port = os.Args[3]
	mode = os.Args[1]

	if mode == "server" {
		startServer(host, port)
	} else if mode == "client" {
		startClient(host, port)
	} else {
		fmt.Println("Usage:  go run PingPong.go <client|server> <serverHost> <port#>")
		os.Exit(1)
	}
}
