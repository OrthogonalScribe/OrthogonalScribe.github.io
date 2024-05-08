#!/usr/bin/env amm3

import scala.util.Random

def sample[A](xs: IndexedSeq[A])(using r: Random): A = xs(r nextInt xs.size)

def genEulerianMultiDiGraph[A](nodes: IndexedSeq[A], edgeCnt: Int)(using Random): Seq[(A,A)] =
  require(nodes == nodes.distinct, "nodes must be unique")
  require(nodes.size > 1, "there must be more than one node")

  val cycleGraph =
    (nodes, nodes.tail).zipped.map{(_,_)} :+ (nodes.last, nodes.head)

  require(edgeCnt >= cycleGraph.size,
    "edge count must be at least as large as the node count")

  def randomRoundTrip[A](v: A) =
    val u = sample(nodes filter {_ != v})
    Seq((v, u), (u, v))

  val edgesToGen = edgeCnt - cycleGraph.size

  val extraEdges = (1 to (edgesToGen/2))
    .flatMap {_ => randomRoundTrip(sample(nodes))}

  if (extraEdges.size == edgesToGen) // Eulerian
    cycleGraph ++ extraEdges
  else                               // semi-Eulerian
    cycleGraph ++ extraEdges :+ randomRoundTrip(sample(nodes)).head

def genRandomAirports(existing: Set[String], n: Int)(using Random): Seq[String] =
  require(n >= existing.size, "requested fewer than the existing airports")

  val res = collection.mutable.Set from existing
  while (res.size < n) // takes longer the closer n is to 26^3
    res += (1 to 3).map{_ => sample('A' to 'Z')}.mkString
  res.toSeq

@main(doc = """Prints an edge list representing a simple (semi-)Eulerian
  |MultiDiGraph: the cycle graph of the airports and enough random roundtrip
  |edges to have edgeCnt or edgeCnt-1 edges. Adds an extra edge to reach
  |edgeCnt when a semi-Eulerian graph is requested, i.e. edgeCnt-airportCnt
  |is odd.""".stripMargin)
def genAirportEulerianMultiDiGraph(airportCnt: Int, edgeCnt: Int,
  seed: Long = 2024L, shuffle: Boolean = true
) =
  require(airportCnt <= math.pow(26, 3),
    "can't generate more than 26^3 unique airports")

  given r: Random = Random(seed)

  val edges =
    val es = genEulerianMultiDiGraph(
      genRandomAirports(Set("JFK"), airportCnt).toIndexedSeq,
      edgeCnt)

    if (shuffle)
      r shuffle es
    else
      es

  edges map {(v, u) => s"$v $u"} foreach println
