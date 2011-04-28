package no.kvikshaug.statsd

import java.util.Date

object Logger {
  def log(message: String) {
    // For now, we just print the message with a timestamp.
    // Maybe it'd be useful with a more sophisticated logging system in the future?
    println(new Date().getTime + ' ' + message)
  }
}
