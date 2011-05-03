package no.kvikshaug.scatsd.server

import java.net._
import java.io.PrintWriter

import scala.actors.Actor
import scala.actors.Actor._

class Outgoing extends java.util.TimerTask with Actor {

  val sb = new StringBuilder

  def run {
    while(ScatsD.busy) {
      Logger.addCount("Out-queue wait for incoming data to get updated (ms)", 10)
      Thread.sleep(10)
    }
    val ts = new java.util.Date().getTime / 1000
    ScatsD.metrics.foreach {
      m => m.kind match {
        case "retain" =>
          sb.append(m.name + " " + m.values(0) + " " + ts + "\n")
          Logger.addCount("Metrics sent to graphite", 1)
        case "count" =>
          if(m.intervalPassed) {
            m.interval.skips.foreach { ts => sb.append(m.name + " " + m.values(0) + " " + ts + "\n"); }
            sb.append(m.name + " " + m.values(0) + " " + ts + "\n")
            m.values = List(0)
            m.interval.skips = List[Long]()
            m.interval.lastUpdate = ts
          } else {
            m.interval.skips = ts :: m.interval.skips
          }
          Logger.addCount("Metrics sent to graphite", 1)
        case "time" if(m.values.size > 0) =>
          if(m.intervalPassed) {
            val sorted = m.values.sortWith { _ < _ }
            m.values = List()
            val mean = sorted.sum / sorted.size.toDouble
            val currentMedian = median(sorted)
            val upperPct = sorted((math.round((ScatsD.percentile / 100.0) * sorted.size) - 1).toInt)
            m.interval.skips = ts :: m.interval.skips
            m.interval.skips.foreach { ts =>
              sb.append(m.name + ".mean " + mean + " " + ts + "\n")
              sb.append(m.name + ".median " + currentMedian + " " + ts + "\n")
              sb.append(m.name + ".upper " + sorted.last + " " + ts + "\n")
              sb.append(m.name + ".upper_" + ScatsD.percentile + " " + upperPct + " " + ts + "\n")
              sb.append(m.name + ".lower " + sorted.head + " " + ts + "\n")
              sb.append(m.name + ".count " + sorted.size + " " + ts + "\n")
            }
            m.interval.skips = List[Long]()
            Logger.addCount("Metrics sent to graphite", 6)
          } else {
            m.interval.skips = ts :: m.interval.skips
          }
        case _ =>
      }
    }
    if(!sb.isEmpty) {
      ScatsD.out ! sb.toString
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
      return new Socket(ScatsD.outHost, ScatsD.outPort)
    } catch {
      case e => if(!failed) {
        Logger.log("Failed to connect to graphite: " + e.getMessage + " - will retry every " + ScatsD.connectWait + " ms.")
        failed = true
      }
    }
    Thread.sleep(ScatsD.connectWait)
    connect() // tail recursion
  }

  def median(l: List[Double]) = if(l.size % 2 == 0) {
    ((l(l.size / 2)) + (l((l.size / 2) - 1))) / 2
  } else {
    l((l.size - 1) / 2)
  }
}

