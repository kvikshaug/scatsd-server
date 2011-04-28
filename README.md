# StatsD

A (slightly mutated) clone of [StatsD][1] written in [Scala][2].

StatsD accumulates and forwards statistic data (metrics) to [Graphite][3].

## Overview

StatsD will:

1. Receive metrics in StatsD-format over UDP at random intervals from its clients
2. Save them, sum them up, calculate arithmetic means and more
2. Forward them at regular intervals in graphite-format to graphite

The StatsD metric format looks like this:

    name|amount|interval|kind

Where:

1.  **name** is a graphite-compatible name of the metric (e.g. `foo.bar`)
2.  **amount** is the current value of the metric (what we want to record)
3.  **interval** specifies a per-metric level flush interval value in seconds. Higher values will give smoother graphs, but lower granularity. If 0, the default value (specified in the configfile) will be used. Setting this to something else only makes sense for *count* and *time* metrics.
4.  **kind** can be one of these text strings:
    * *count* - The value will be accumulated. At each flush interval the accumulated value is sent to graphite and reset to 0.
    * *retain* - The value will be set, regardless of its previous value. At each flush interval the current value is sent to graphite, and kept.
    * *time* - Each value will be stored separately. At each flush interval, calulations are made on these values, sent to graphite, and the stored values are cleared.

### Time calculations

When StatsD receives one or more *time* metrics within a flush interval, the following values are calculated, appended to the metric name and sent to graphite:

* The mean of all values
* The median of all values
* The 90th percentile (configurable to higher/lower percentile)
* The highest value
* The lowest value
* The number of values

### Graphite database schema

I prefer Etsys recommendation of saving:

* 6 hours of 10 second data
* 1 week of 1 minute data
* 5 years of 10 minute data

The schema looks like this:

    [stats]
    priority = 110
    pattern = .*
    retentions = 10:2160,60:10080,600:262974

## Why clone StatsD?

* StatsD does not calculate the median of timings.
* StatsD crashes when the connection to graphite is broken.
* StatsD puts a prefix on the metric names before sending them to Graphite, I don't see the point of that at all.
* StatsD cannot retain values. Etsy [commented][4] that they cheat and use the time mean + a graphite function to achieve this.
* StatsD divides the accumulated counts by the flush interval. This means that the display will show per second-values, but 

## So why not just contribute to StatsD instead?

* I'm not very well versed in Javascript.
* I get to change the protocol to my likings, they probably wouldn't want that.
* I wanted to learn about how it works, and writing a clone forces me to get to know *all* the implementation details.

[1]: https://github.com/etsy/statsd "Etsy"
[2]: http://www.scala-lang.org/
[3]: http://graphite.wikidot.com/
[4]: http://codeascraft.etsy.com/2011/02/15/measure-anything-measure-everything/#comment-1087

