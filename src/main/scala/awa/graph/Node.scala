package awa.graph

import scala.collection.mutable.ListBuffer
/** A person who uses our application.
  *
  * @constructor Creates a new node of the graph
  * @param name: the nodes name. A int
  */
class Node(val name: Int) {

  // hold the neighbors
  var edges: ListBuffer[Node] = ListBuffer()

  // add a neighbor to this node
  def addNeighbor(nNode: Node) = {
    // check if the node already on the list
    if (!edges.contains(nNode)) {
      edges += nNode
    }
  }

  // print utility
  override def toString() = {
    "Number: " + this.name.toString()
  }

  // the amount of nodes
  def neihgborCount(): Int = this.edges.length
}
