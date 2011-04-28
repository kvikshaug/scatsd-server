package no.kvikshaug.statsd

import java.net._
import java.io.PrintWriter

import scala.actors.Actor
import scala.actors.Actor._

class Outgoing extends java.util.TimerTask with Actor {

  val sb = new StringBuilder

  def run {
    while(StatsD.busy) {
      Logger.addCount("Out-queue wait for incoming data to get updated (ms)", 10)
      Thread.sleep(10)
    }
    val ts = new java.util.Date().getTime / 1000
    StatsD.metrics.foreach {
      m => m.kind match {
        case "retain" =>
          sb.append(m.name + " " + m.values(0) + " " + ts + "\n")
          Logger.addCount("Metrics sent to graphite", 1)
        case "count" =>
          sb.append(m.name + " " + m.values(0) + " " + ts + "\n")
          m.values = List(0)
          Logger.addCount("Metrics sent to graphite", 1)
        case "time" if(m.values.size > 0) =>
          val sorted = m.values.sortWith { _ < _ }
          m.values = List()
          val mean = sorted.sum / sorted.size.toDouble
          val upperPct = sorted((math.round((StatsD.percentile / 100.0) * sorted.size) - 1).toInt)

          sb.append(m.name + ".mean " + mean + " " + ts + "\n")
          sb.append(m.name + ".median " + median(sorted) + " " + ts + "\n")
          sb.append(m.name + ".upper " + sorted.last + " " + ts + "\n")
          sb.append(m.name + ".upper_" + StatsD.percentile + " " + upperPct + " " + ts + "\n")
          sb.append(m.name + ".lower " + sorted.head + " " + ts + "\n")
          sb.append(m.name + ".count " + sorted.size + " " + ts + "\n")
          Logger.addCount("Metrics sent to graphite", 6)
        case _ =>
      }
    }
    if(!sb.isEmpty) {
      StatsD.out ! sb.toString
      sb.clear
    }
  }

  def act {
    loop {
      receive {
        case str: String =>
          val socket = connect()
          if(failed) {
            Logger.log("Graphite connection is back up.")
            failed = false
          }
          val pw = new PrintWriter(socket.getOutputStream, true)
          pw.println(str)
          pw.close
          socket.close
      }
    }
  }

  var failed = false

  def connect(): Socket = {
    try {
      return new Socket(StatsD.outHost, StatsD.outPort)
    } catch {
      case e => if(!failed) {
        Logger.log("Failed to connect to graphite: " + e.getMessage + " - will retry every " + StatsD.connectWait + " ms.")
        failed = true
      }
    }
    Thread.sleep(StatsD.connectWait)
    connect() // tail recursion
  }

  def median(l: List[Double]) = if(l.size % 2 == 0) {
    ((l(l.size / 2)) + (l((l.size / 2) - 1))) / 2
  } else {
    l((l.size - 1) / 2)
  }
}

