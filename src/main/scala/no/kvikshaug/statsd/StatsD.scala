package no.kvikshaug.statsd

import java.net._
import java.util.Timer

import scala.xml._
import scala.actors.Actor
import scala.actors.Actor._

object StatsD {

  val config = XML.load("config.xml")
  val flushInterval = (config \ "flushInterval").text.toLong
  val connectWait = (config \ "connectWait").text.toInt
  val logCountInterval = (config \ "logCountInterval").text.toInt
  val percentile = (config \ "percentile").text.toInt
  val inHostsAllowed = config \ "hosts" \ "host" map { h => InetAddress.getByName(h.text) }
  val inPort = (config \ "port").text.toInt
  val outHost = (config \ "graphite" \ "host").text
  val outPort = (config \ "graphite" \ "port").text.toInt

  var metrics = List[Metric]()

  // true when a metric is being changed
  var busy = false

  val inActor = new Incoming
  val inThread = new Thread(inActor)
  val out = new Outgoing
  val logger = new Thread(Logger)

  def main(args: Array[String]) {
    // Start the logger
    logger.start

    // Start listening for incoming data
    inActor.start
    inThread.start

    // Start sending data to graphite
    out.start
    new Timer().scheduleAtFixedRate(out, 0, flushInterval * 1000L)
  }
}
