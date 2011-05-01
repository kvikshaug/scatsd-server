package no.kvikshaug.scatsd

import java.util.{Date, TimerTask}

object Logger extends TimerTask {

  var counts = Map[String, Double]()

  def addCount(name: String, amount: Double) {
    if(counts.get(name).isEmpty) {
      counts = counts + (name -> amount)
    } else {
      counts = counts.updated(name, counts(name) + amount)
    }
  }

  def run {
    if(counts.size == 0) {
      println(time + "No logged counts yet.")
    } else {
      println(time + "Counts since last count:")
    }
    counts foreach { e => println(time + e._1 + ": " + e._2) }
    counts = counts map { e => (e._1, 0.0) }
  }

  def log(message: String) {
    // For now, we just print the message with a timestamp.
    // Maybe it'd be useful with a more sophisticated logging system in the future?
    println(time + message)
  }

  def time = new Date().getTime + " "
}
