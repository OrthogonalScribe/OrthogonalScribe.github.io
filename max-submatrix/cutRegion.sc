#!/usr/bin/env amm3

@main
def main(r0: Int, c0: Int, r1: Int, c1: Int) =
  val A = scala.io.Source.stdin.getLines.toArray.tail map
    {_ split " " filter {_.nonEmpty} map BigInt.apply }

  var region = (r0 to r1 map A) map {c0 to c1 map _}

  println(s"${region.size} ${region.head.size}")

  region map {_ mkString " "} foreach println
