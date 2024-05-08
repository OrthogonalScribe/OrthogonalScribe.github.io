#!/usr/bin/env amm3

case class Stat(name: String, nodeCnt: Int, edgeCnt: Int, wallTimeNs: Int)

@main(doc = """Takes real_time_mean rows from a templated multi-repetition
  |Google benchmark console output table as input. Prints markdown table of
  |speedups compared to base.""".stripMargin)
def printStatsTable(base: String, column: Seq[String]) =
  val cs = column

  val stats = scala.io.Source.stdin.getLines.toSeq map {
    case s"solve<$name>/$nodeCnt/$edgeCnt/real_time_mean $wallTime ns $_" =>
      Stat(name, nodeCnt.toInt, edgeCnt.toInt, wallTime.trim.toInt) }

  println(s"""| Edges | Nodes | $base | ${cs                      mkString " | "} |""")
  println(s"""| ----: | ----: | ----: | ${Seq.fill(cs.size)("-:") mkString " | "} |""")

  for
    ((edgeCnt, nodeCnt), ss) <-
      stats.groupBy{s => (s.edgeCnt,s.nodeCnt)}.toVector.sortBy{_._1}
    timeOf = ss.map{s => s.name -> s.wallTimeNs}.toMap
    baseNs = timeOf(base)
  do
    def fmt(name: String) =
      f"${baseNs.toDouble/timeOf(name)}%.2fx"

    println(s"""| $edgeCnt | $nodeCnt | $baseNs ns | ${cs map fmt mkString " | "} |""")
