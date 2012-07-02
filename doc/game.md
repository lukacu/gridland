Game creation
============

Each game is defined as a set of properties, defined in a `.game`
file that is actually an INI-like file.

Gameplay options
--------------

 * `gameplay.speed` - The speed of the gameplay (optional, default 10)
 * `gameplay.field` - The map file to load (required), see maps.txt
 * `gameplay.agents` - The maximum number of agents per team (optional, default 10)
 * `gameplay.respawn` - The number of timesteps between respawn phases (optional, default 30)
 * `gameplay.flags` - The flags mode of the game (default unique):
    * `unique` - The positions of the flags are acquired from the map file
    * `random` - Position flags randomly on the map
    * `respawn` - Position flags randomly on the map and add new flags to maintain a fixed pool of
       flags per team.
  * `gameplay.flags.pool` - The number of flags for flag modes random and respawn
  * `gameplay.flags.respawn` - The number of timesteps between respawn phases for flag mode respawn
  * `gameplay.flags.weight` - The weight of a flag (each collected flag will cause the agent to move slower)

Protocol options
--------------

 * `message.size` - Maximum personal message size in bytes (optional, default 256)
 * message.neighborhood - The size of the neighborhood scan
    For size N, the entire neighborhood has width and height (N * 2 +1)
 * `message.speed` - The personal message transfer speed. Not that this is all game emulation stuff. An integer
    number means the number of bytes per game step. The messages are queued on the sender side for the sufficient 
    number of steps and then transmitted to the receiver.

Teams
----

For each active team it is necessary so specify an id of the team as a property.
The key of the property is `team{N}`, where `N` is an integer number starting with 1.
The value of the property is the id of the team that is used to identify the clients.

Example:

> team1=foo
> team2=bar


A more sophisticated usage involves a team database that defines id, name, passphrase and
color for all teams.

Misc options
-----------

 * `title` - the title of the game, visible in server window

