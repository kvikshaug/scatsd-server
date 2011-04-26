package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

case class Metric(var name: String, var values: List[Double], var kind: String) {
  def update(other: Metric) {
    kind = other.kind // in case the sender changed their mind
    kind match {
      case "retain" => values = other.values
      case "count"  => values = List(values(0) + other.values(0))
      case "time"   => values = other.values(0) :: values
    }
  }
}

object Parseable {
  val validKinds = List("retain", "count", "time")

  def unapply(str: String) = {
    try {
      val fields = str.split('|').toList
      val name = fields(0)
      val value = fields(1).toDouble
      var kind = fields(2)
      if(!validKinds.contains(kind)) {
        throw new IllegalArgumentException("Unrecognized metric type: " + kind)
      }
      Some(Metric(name, List(value), kind))
    } catch {
      // TODO log instead of println
      case e => println("Couldn't parse string '" + str + "' because: " + e.toString); None
    }
  }
}

object StatsD {

  // TODO read this from config
  val sleepTime = 10 // seconds
  val inHostsAllowed = List(InetAddress.getByName("127.0.0.1"))
  val inPort = 8125
  val outHost = "localhost"
  val outPort = 2003

  var metrics = List[Metric]()

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

  class Incoming extends Actor with Runnable {
    def run {
      val s = new DatagramSocket(inPort)
      val b: Array[Byte] = new Array(s.getReceiveBufferSize)
      val d = new DatagramPacket(b, b.length)
      println("Listening...")

      while(true) {
        s.receive(d)
        inActor ! new String(d.getData, 0, d.getLength)
      }
    }

    def act {
      loop {
        receive {
          case Parseable(metric) =>
            val existing = metrics.find(_.name == metric.name)
            if(existing.isEmpty) {
              // The metric doesn't exist, add it to the list
              // TODO log
              metrics = metric :: metrics
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

  class Outgoing extends Actor with Runnable {
    def run {
      while(true) {
        Thread.sleep(sleepTime * 1000)
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
}
