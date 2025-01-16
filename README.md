## PingPong Simulation

- Author: Alex Lewtschuk

## Overview

This PingPong project is a simple implementation of a distributed computing excercise written and implemented accross several languages. The program utilizes multithreading and socket connections to make a server and client simulate a game of ping pong by sending and recieveing an object over network. When the client and server connect there is a distributed coin toss to decide who plays first between the two parties. There is also a servent version in the Servernt directories.

## Compiling and Using

To run the code please see the readmes for each language's version of the program. General usagage requires using the terminal and running the command `<language specific call> PingPongProgram.language <client|server> <serverHost> <port#>`. Make sure that the host is always `localhost` when running locally. 

Once the `server` has been set up open another terminal and with the same command specify `client` and the information for the server you just set up. This is how the `client` will connect.

To run the Servent version `cd` to `/Servent` and run the command: `<language specific call> PingPongProgram.language 1 <port#> <serverHost> <port#>` then in another terminal run `<language specific call> PingPongProgram.language 2 <port#> <serverHost> <port#>`. 

Please note the servent functionality of being both `server|client` is best demonstrated while running a third servent as there might be a bug with getting client multithreading to work. 

Please open a third terminal and run: `<language specific call> PingPongProgram.language 3 <port#> <serverHost> <port#>`