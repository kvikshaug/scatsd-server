package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

class Incoming extends Actor with Runnable {
  def run {
    val s = new DatagramSocket(StatsD.inPort)
    val b: Array[Byte] = new Array(s.getReceiveBufferSize)
    val d = new DatagramPacket(b, b.length)
    Logger.log("Listening for incoming data on port " + StatsD.inPort + ".")
    Logger.log("Discarding packets from any host other than: " + StatsD.inHostsAllowed)

    while(true) {
      s.receive(d)
      if(StatsD.inHostsAllowed.contains(d.getAddress)) {
        this ! new String(d.getData, 0, d.getLength)
      } else {
        Logger.log("WARNING: Received data from host not in allow list! Host: '" + d.getAddress + "', data: '" + new String(d.getData, 0, d.getLength) + "'")
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
            StatsD.metrics = metric :: StatsD.metrics
            Logger.log("Adding new '" + metric.kind + "' metric '" + metric.name + "', now handling " + StatsD.metrics.size + " metrics.")
          } else {
            // The metric exists, add to it
            existing.get.update(metric)
            Logger.addCount("Received metric updates", 1)
          }
        case _ =>
      }
    }
  }
}

