#!/usr/bin/env amm2

val t0 = scala.io.Source.stdin.getLines.next.split(',').map{_.toInt}

// patch tape
val t = Array(t0.head, 12, 2) ++ (t0 drop 3)

def run(i: Int): Unit = t.slice(i, i+4) match {
  case Array( 1,a,b,c ) => t(c) = t(a) + t(b); run(i+4)
  case Array( 2,a,b,c ) => t(c) = t(a) * t(b); run(i+4)
  case Array(99,   _* ) => ()  // halt
}

run(0)

println(t.head)
