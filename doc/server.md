Server usage
-----------

The server is started by executing the main method of the class fri.pipt.Main with
a single argument that is the path of the game file (see game.txt). The server accepts
connections on port 5000 (this is currently hardcoded).

The left side of the server window shows the game status. On the right one can see a list of
connected clients, their assigned agents and a graph of the number of protocol messages 
sent by the client per second.

To start a game, press Play button in the top left corner.

A history visualization overlay of an individual agent can be enabled by clicking on the
list item for a client that controls the agent.

To end the game, close the server window.
