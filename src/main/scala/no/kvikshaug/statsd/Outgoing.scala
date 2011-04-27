package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

class Outgoing extends Actor with Runnable {
  def run {
    val sb = new StringBuilder
    while(true) {
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
        StatsD.outActor ! sb.toString
        sb.clear
      }
      // TODO use java.util.Timer
      Thread.sleep(StatsD.sleepTime * 1000)
    }
  }

  def act {
    loop {
      receive {
        case str: String =>
          // TODO send to graphite
          println("Sending: " + str)
      }
    }
  }
}

