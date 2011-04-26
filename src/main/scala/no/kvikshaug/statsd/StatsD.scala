package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

case class Metric(val name: String, val value: Double, val kind: String)

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
      Some(Metric(name, value, kind))
    } catch {
      // TODO log instead of println
      case e => println("Couldn't parse string '" + str + "' because: " + e.toString); None
    }
  }
}

object StatsD {

  val sleepTime = 10 // seconds

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
      val s = new DatagramSocket(8125)
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
            if(metrics.exists(_.name == metric.name)) {
              // The metric exists, let's add to it based on what kind of metric it is
              // TODO log
            } else {
              // The metric doesn't exist, add it to the list
              // TODO log
              metrics = metric :: metrics
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
