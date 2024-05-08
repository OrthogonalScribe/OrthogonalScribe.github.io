#!/usr/bin/env amm3

val A = scala.io.Source.stdin.getLines.toArray.tail map
  {_ split " " filter {_.nonEmpty} map BigInt.apply}

case class Max(sum: BigInt, uli: Int, ulj: Int, width: Int)

var m = Max(-1,-1,-1,-1)

for
  i <- A   .indices
  j <- A(i).indices
  width <- 1 to ((i min j) + 1)

  uli = i - width + 1
  ulj = j - width + 1
  region = (uli to i map A) map {ulj to j map _}

  if region forall {_ forall 0.!=}
  sum = region.map{_.sum}.sum
  if sum > m.sum
do
  m = Max(sum, uli, ulj, width)

println(s"Sum=${m.sum} in ${m.width}-wide square starting at (${m.uli},${m.ulj})")
