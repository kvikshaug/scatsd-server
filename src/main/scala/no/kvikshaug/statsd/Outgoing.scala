package no.kvikshaug.statsd

import java.net._

import scala.actors.Actor
import scala.actors.Actor._

class Outgoing extends Actor with Runnable {
  def run {
    while(true) {
      while(StatsD.busy) {
        Thread.sleep(10)
        // TODO log
      }
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

