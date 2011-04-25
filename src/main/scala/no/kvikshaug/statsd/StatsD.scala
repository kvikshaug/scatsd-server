package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

object StatsD {

  val sleepTime = 10 // seconds

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
      var d = new DatagramPacket(b, b.length)
      println("Listening...")

      while(true) {
        s.receive(d)
        val str = new String(d.getData, 0, d.getLength)
        inActor ! str
      }
    }

    def act {
      loop {
        receive {
          case str: String =>
            // TODO handle string
            println("Receiving: " + str)
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
