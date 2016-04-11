# AKGraph
Calculates graph centrality with Scala, Akka and WebSocket

Use the regular sbt commands to download the appropriated libs:

in the root folder

    sbt
    > run

After the dependencies are downloadedm the server will start on port 8080.

The webpage allows the user to insert a graph, based on the edge relations (presented below), draw the graph and calculate centralities measures.

Example of graph

    1 2
    2 3
    1 3
