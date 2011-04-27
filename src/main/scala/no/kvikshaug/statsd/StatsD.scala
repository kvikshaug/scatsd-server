package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

object StatsD {

  // TODO read this from config
  val sleepTime = 10 // seconds
  val inHostsAllowed = List(InetAddress.getByName("127.0.0.1"))
  val inPort = 8125
  val outHost = "localhost"
  val outPort = 2003

  var metrics = List[Metric]()

  // true when a metric is being changed
  var busy = false

  val inActor = new Incoming
  val inThread = new Thread(inActor)
  val outActor = new Outgoing
  val outThread = new Thread(outActor)

  def main(args: Array[String]) {
    // Start listening for incoming data
    inActor.start
    inThread.start

    // Start sending data to graphite
    outActor.start
    outThread.start
  }
}
