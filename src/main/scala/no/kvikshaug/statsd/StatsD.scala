package no.kvikshaug.statsd

import java.net._
import java.util.Timer

import scala.actors.Actor
import scala.actors.Actor._

object StatsD {

  // TODO read this from config
  val sleepTime = 10 // seconds
  val percentile = 90
  val inHostsAllowed = List(InetAddress.getByName("127.0.0.1"))
  val inPort = 8125
  val outHost = "localhost"
  val outPort = 2003

  var metrics = List[Metric]()

  // true when a metric is being changed
  var busy = false

  val inActor = new Incoming
  val inThread = new Thread(inActor)
  val out = new Outgoing

  def main(args: Array[String]) {
    // Start listening for incoming data
    inActor.start
    inThread.start

    // Start sending data to graphite
    out.start
    new Timer().scheduleAtFixedRate(out, 0, sleepTime * 1000L)
  }
}
