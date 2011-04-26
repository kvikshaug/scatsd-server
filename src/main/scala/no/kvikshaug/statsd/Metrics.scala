package no.kvikshaug.statsd

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

