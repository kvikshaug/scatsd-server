package no.kvikshaug.statsd

object StatsD extends Thread {

  val sleepTime = 10 // seconds

  var continue = true
  Runtime.getRuntime().addShutdownHook(this)
  override def run {
    println("Caught signal; exiting.")
    continue = false
  }

  def main(args: Array[String]) {
    while(continue) {
      Thread.sleep(sleepTime * 1000)
    }
  }
}
