## PingPong Simulation

- Author: Alex Lewtschuk

## Overview

This PingPong project is a simple implementation of a distributed computing excercise written and implemented accross several languages. The program utilizes multithreading and socket connections to make a server and client simulate a game of ping pong by sending and recieveing an object over network. When the client and server connect there is a distributed coin toss to decide who plays first between the two parties. There is also a servent version in the Servernt directories.

## Compiling and Using

To run the code please see the readmes for each language's version of the program. General usagage requires using the terminal and running the command `go run PingoPongo.go <client|server> <serverHost> <port#>`. Make sure that the host is always `localhost` when running locally. 

Once the `server` has been set up open another terminal and with the same command specify `client` and the information for the server you just set up. This is how the `client` will connect.
