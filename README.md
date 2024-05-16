## PingPong Simulation

- Author: Alex Lewtschuk
- Class: CS455 
- Semester: Spring 2024

## Overview

This PingPong project incorporates multithreading and socket connectios to make a server and client simulate a game of ping pong. When the client and server connect there is a distributed coin toss to decide who plays first. There is also a servent version in the Servernt directory.

## Compiling and Using

To run the code please use terminal and run the command `java pingpong.java <client|server> <serverHost> <port#>`. Make sure that the host is always `localhost`. 

Once the `server` has been set up open another terminal and with the same command specify `client` and the information for the server you just set up. This is how the `client` will connect.

To run the Servent version `cd` to `/Servent` and run the command: `java PingPongServent.java 1 5005 localhost 5006` then in another terminal run `java PingPongServent.java 2 5006 localhost 5005`. 

Please note the servent functionality of being both `server|client` is best demonstrated while running a third servent as there might be a bug with getting client multithreading to work I am not 100% sure. 

Please open a third terminal and run: `java pingpongservent.java 3 5007 localhost 5006`


