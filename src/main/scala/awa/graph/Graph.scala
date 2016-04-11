package awa.graph

import scala.io.Source
import scala.collection.mutable.{ListBuffer, Queue}


/**
  * the graph class that holds all the others.
  */
class Graph () {

  // nodes that are part of the graph
  var nodes = Map[Int, Node]()

  // add a node into the list if it is not present
  def addNode(nNode: Node) = {
    // check if the node already on the list
    if (!nodes.contains(nNode.name)) {
      nodes += (nNode.name -> nNode)
    }
  }

  // get the node if it exists
  def getNode(number: Int):Option[Node] = {
    this.nodes.get(number)
  }

  // print utility
  override def toString() = {
    "==============================================\n" +
      "Nodes: \n"+ this.nodes.toList.map(_ + "\n")  +
      "==============================================\n"
  }

  // create the graph from a file
  def createGraphFromFile(file: String): Graph = {

    // create the nodes list
    //val info = Source.fromURL(getClass.getResource(file))
    val getCurrentDirectory = new java.io.File( "." ).getCanonicalPath()
    val info = Source.fromFile(getCurrentDirectory + file).mkString

    createGraphFromText(info)
    this
  }

  def createGraphFromText(text: String): Boolean = {

    try {
      for (e <- text.split("\n")) {

        val l = e.split(" ").map(_.toInt)

        // create or get an existing node
        val nodea = this.getNode(l(0)).getOrElse(new Node(l(0)))
        val nodeb = this.getNode(l(1)).getOrElse(new Node(l(1)))

        // add the edges and save the node
        nodea.addNeighbor(nodeb)
        nodeb.addNeighbor(nodea)
        this.addNode(nodea)
        this.addNode(nodeb)
      }
      // amount of nodes.
      println(s"Loaded graph with ${this.nodes.size} nodes")
      return true
    } catch {
        case _ => {
          println("Error reading from graph")
          return false
        }
    }
  }

  // find the shortest paths and calculate the farness
  def findShortest(nodeName: Int):Float = {
    // create the structures to hold the paths and the shortest
    val q = Queue[Path]()
    var shortest = Map[Int, Path]()
    val sNode = this.getNode(nodeName).get
    q.enqueue(Path(0, ListBuffer(sNode.name)))
    shortest += (sNode.name -> Path(0, ListBuffer(sNode.name)))
    var far = 0

    while(!q.isEmpty) {
      // explicitly
      val path = q.dequeue()
      val currNode = this.getNode(path.path.last).get
      val currCost = path.cost
      val currPath = path.path

      for( nbor <- currNode.edges) {
        // get the current shortest path to the neighbor
        if (!shortest.contains(nbor.name)) {
          // the cost to add another node is 1 only
          val newPath = Path(currCost+1, currPath :+ nbor.name)

          // add the new node to the queue to be traversed and add his cost to shortest
          q.enqueue(newPath)
          shortest += (nbor.name -> newPath)

          // add to the total farness of the node to all nodes
          far += currCost+1
        }
      }
    }

    // helper function to order the results
    //val sortedShortest = ListMap(shortest.toSeq.sortBy(_._1):_*)

    // return the inverse of farness
    1.0f/far
  }

}