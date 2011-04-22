package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

object StatsD extends Actor with Runnable {

  val sleepTime = 10 // seconds

  var running = true
  Runtime.getRuntime().addShutdownHook(new Thread(this))
  override def run {
    println("Caught signal; exiting.")
    running = false
  }

  val actor = this
  actor.start


  def main(args: Array[String]) {
    val s = new DatagramSocket(8125)
    val b: Array[Byte] = new Array(s.getReceiveBufferSize)
    var d = new DatagramPacket(b, b.length)
    println("Listening...")

    // Main loop
    while(running) {
      s.receive(d)
      val str = new String(d.getData, 0, d.getLength)
      actor ! str
    }
  }

  def act {
    loop {
      receive {
        case str: String =>
          // TODO handle string
          println(str)
      }
    }
  }
}
