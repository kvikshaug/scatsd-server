package no.kvikshaug.scatsd

import java.io._
import java.net._
import java.util.Timer

import scala.xml._
import scala.actors.Actor
import scala.actors.Actor._

object ScatsD {

  val config = XML.load("config.xml")
  val flushInterval = (config \ "flushInterval").text.toLong
  val connectWait = (config \ "connectWait").text.toInt
  val logCountInterval = (config \ "logCountInterval").text.toInt
  val percentile = (config \ "percentile").text.toInt
  val inHostsAllowed = config \ "hosts" \ "host" map { h => InetAddress.getByName(h.text) }
  val inPort = (config \ "port").text.toInt
  val outHost = (config \ "graphite" \ "host").text
  val outPort = (config \ "graphite" \ "port").text.toInt
  val stateFile = (config \ "stateFile").text

  var metrics: List[Metric] = Nil

  // true when a metric is being changed
  var busy = false

  val timer = new Timer
  val inActor = new Incoming
  val inThread = new Thread(inActor)
  val out = new Outgoing

  def main(args: Array[String]) {
    metrics = loadState
    // Start the logger
    timer.scheduleAtFixedRate(Logger, 0, logCountInterval * 1000L)

    // Start listening for incoming data
    inActor.start
    inThread.start

    // Start sending data to graphite
    out.start
    timer.scheduleAtFixedRate(out, 0, flushInterval * 1000L)

    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run {
        println(format("Received exit signal, saving state to %s...", stateFile))
        saveState
      }
    });
  }

  def saveState {
    val out = new ObjectOutputStream(new FileOutputStream(stateFile));
    out.writeObject(metrics);
    out.close();
  }

  def loadState: List[Metric] = {
    if(!new File(stateFile).exists) {
      return Nil
    }
    val in = new ObjectInputStream(new FileInputStream(stateFile))
    val metrics = in.readObject.asInstanceOf[List[Metric]]
    Logger.log(format("Pre-loaded %s metrics from %s", metrics.size, stateFile))
    in.close()
    return metrics
  }
}
