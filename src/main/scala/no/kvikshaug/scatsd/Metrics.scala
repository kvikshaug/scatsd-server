package no.kvikshaug.scatsd

import java.util.Date

case class Interval(var interval: Double, var lastUpdate: Long, var skips: List[Long])
case class Metric(var name: String, var values: List[Double], val interval: Interval, var kind: String) {

  def intervalPassed = (new Date().getTime / 1000) - interval.interval >= interval.lastUpdate

  def update(other: Metric) {
    ScatsD.busy = true
    kind = other.kind // in case the sender changed their mind
    kind match {
      case "retain" => values = other.values
      case "count"  => values = List(values(0) + other.values(0))
      case "time"   => values = other.values(0) :: values
    }
    ScatsD.busy = false
  }
}

object Parseable {
  val validKinds = List("retain", "count", "time")

  def unapply(str: String) = {
    try {
      val fields = str.split('|').toList
      val name = fields(0)
      val value = fields(1).toDouble
      val interval = Interval(fields(2).toDouble, new Date().getTime / 1000, List[Long]())
      var kind = fields(3)
      if(!validKinds.contains(kind)) {
        throw new IllegalArgumentException("Unrecognized metric type: " + kind)
      }
      Some(Metric(name, List(value), interval, kind))
    } catch {
      case e => Logger.log("ERROR: Incoming string '" + str + "' was unparseable because: " + e.toString); None
    }
  }
}

