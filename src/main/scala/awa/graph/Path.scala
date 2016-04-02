package awa.graph

import scala.collection.mutable.ListBuffer

/**
  * Path class to hold the cost and the path so far
  */
// path information and cost
case class Path(cost: Int, path: ListBuffer[Int])