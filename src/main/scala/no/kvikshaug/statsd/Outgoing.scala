package no.kvikshaug.statsd

import java.net._
import java.io.PrintWriter

import scala.actors.Actor
import scala.actors.Actor._

class Outgoing extends java.util.TimerTask with Actor {

  val sb = new StringBuilder

  def run {
    while(StatsD.busy) {
      Thread.sleep(10)
      // TODO log
    }
    val ts = new java.util.Date().getTime / 1000
    StatsD.metrics.foreach {
      m => m.kind match {
        case "retain" =>
          sb.append(m.name + " " + m.values(0) + " " + ts + "\n")
        case "count" =>
          sb.append(m.name + " " + m.values(0) + " " + ts + "\n")
          m.values = List(0)
        case "time" if(m.values.size > 0) =>
          val sorted = m.values.sortWith { _ < _ }
          m.values = List()
          val mean = sorted.sum / sorted.size.toDouble
          val upperPct = sorted((math.round((StatsD.percentile / 100.0) * sorted.size) - 1).toInt)

          sb.append(m.name + ".mean " + mean + " " + ts + "\n")
          sb.append(m.name + ".upper " + sorted.last + " " + ts + "\n")
          sb.append(m.name + ".upper_" + StatsD.percentile + " " + upperPct + " " + ts + "\n")
          sb.append(m.name + ".lower " + sorted.head + " " + ts + "\n")
          sb.append(m.name + ".count " + sorted.size + " " + ts + "\n")
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
          val pw = new PrintWriter(socket.getOutputStream, true)
          pw.println(str)
          pw.close
          socket.close
      }
    }
  }

  def connect(): Socket = {
    try {
      return new Socket(StatsD.outHost, StatsD.outPort)
    } catch {
      case e => // TODO log
    }
    Thread.sleep(StatsD.connectWait)
    connect() // tail recursion
  }
}

