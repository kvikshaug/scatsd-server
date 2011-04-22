package no.kvikshaug.statsd

import java.net._

object StatsD extends Thread {

  val sleepTime = 10 // seconds

  var continue = true
  Runtime.getRuntime().addShutdownHook(this)
  override def run {
    println("Caught signal; exiting.")
    continue = false
  }

  def main(args: Array[String]) {
    val s = new DatagramSocket(8125)
    val b: Array[Byte] = new Array(s.getReceiveBufferSize)
    var d = new DatagramPacket(b, b.length)
    println("Listening...")

    // Main loop
    while(continue) {
      s.receive(d)
      handleString(new String(d.getData, 0, d.getLength))
      Thread.sleep(sleepTime * 1000)
    }
  }

  def handleString(str: String) {
    // TODO handle string
    println(str)
  }
}
