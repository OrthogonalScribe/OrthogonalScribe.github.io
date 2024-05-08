#!/usr/bin/env amm2

case class Op(opcode: Int, pos: Int, v: Int) {
  import math.pow
  def get(t: Array[Int]) =
    (opcode/100)%pow(10,pos).toInt/pow(10,pos-1).toInt match {
      case 0 => t(v)
      case 1 => v
    }
}

sealed trait Insn
case object Halt extends Insn
case class Add(a: Op, b: Op, c: Int) extends Insn
case class Mul(a: Op, b: Op, c: Int) extends Insn
case class In (p: Int) extends Insn
case class Out(a: Op ) extends Insn

sealed trait State
case object Halted         extends State
case object NeedInput      extends State
case class  Output(v: Int) extends State

case class Proc(tape: Seq[Int]) {
  val t = Array(tape: _*)  // copy
  var ip = 0

  def decode = (t(ip)%100, t.slice(ip,ip+4)) match {
    case ( 1, Array(o, a,b,c )) => Add(Op(o,1,a), Op(o,2,b), c)
    case ( 2, Array(o, a,b,c )) => Mul(Op(o,1,a), Op(o,2,b), c)
    case ( 3, Array(o, p, _* )) => In (p)
    case ( 4, Array(o, a, _* )) => Out(Op(o,1,a))
    case (99, _               ) => Halt
  }

  def step: State = decode match {
    case Add(a,b,c) => ip += 4; t(c) = a.get(t) + b.get(t); step
    case Mul(a,b,c) => ip += 4; t(c) = a.get(t) * b.get(t); step
    case Out(a)     => ip += 2; Output(a.get(t))
    case In (p)     => NeedInput
    case Halt       => Halted
  }

  def stepWithInput(v: Int): State = decode match {
    case In(p) => t(p) = v; ip += 2; step
  }
}

val p = Proc(scala.io.Source.stdin.getLines.next.split(',').map{_.toInt})

println(p.stepWithInput(1))

Iterator.continually(p.step)
  .takeWhile {_ != Halted}
  .foreach (println)
