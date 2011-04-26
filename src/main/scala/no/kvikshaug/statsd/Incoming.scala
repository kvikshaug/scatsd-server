package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

class Incoming extends Actor with Runnable {
  def run {
    val s = new DatagramSocket(StatsD.inPort)
    val b: Array[Byte] = new Array(s.getReceiveBufferSize)
    val d = new DatagramPacket(b, b.length)
    println("Listening...")

    while(true) {
      s.receive(d)
      if(StatsD.inHostsAllowed.contains(d.getAddress)) {
        this ! new String(d.getData, 0, d.getLength)
      } else {
        // TODO log
      }
    }
  }

  def act {
    loop {
      receive {
        case Parseable(metric) =>
          val existing = StatsD.metrics.find(_.name == metric.name)
          if(existing.isEmpty) {
            // The metric doesn't exist, add it to the list
            // TODO log
            StatsD.metrics = metric :: StatsD.metrics
          } else {
            // The metric exists, add to it
            existing.get.update(metric)
            // TODO log
          }
        case i => // TODO log
      }
    }
  }
}

