package main

import (
	"encoding/gob"
	"fmt"
	"math/rand"
	"net"
	"strconv"
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
	fmt.Println("-----------Client Setup Begin-----------\nHost: " + pingPongClient.host + "\nPort: " + strconv.Itoa(pingPongClient.port))
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
	serverConnection.Write([]byte(strconv.Itoa(myNum))) //Sends number result to other party

	var buf []byte
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
	case true:
		fmt.Println(pingPongStruct.flag + ": Coin toss terminated. I play as ping\n")
		encoder.Encode(ball) //Sends ball with result
		fmt.Println(pingPongStruct.flag + " sent ping")
		return PlayerAssignments{"ping", "pong"}
	case false:
		fmt.Println(pingPongStruct.flag + ": Coin toss terminated. I play as pong\n")
		encoder.Encode(ball) //Sends ball with result
		fmt.Println(pingPongStruct.flag + " sent pong")
		return PlayerAssignments{"pong", "ping"}
	default:
		fmt.Println("Result tied for client and server. Coin toss repeating")
		return distributedToss(serverConnection, ball, pingPongStruct)
	}
}
