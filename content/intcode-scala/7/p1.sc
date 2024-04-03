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
case class Add(a: Op, b: Op, p: Int) extends Insn
case class Mul(a: Op, b: Op, p: Int) extends Insn
case class In (p: Int              ) extends Insn
case class Out(a: Op               ) extends Insn
case class JNZ(a: Op, b: Op        ) extends Insn
case class JZ (a: Op, b: Op        ) extends Insn
case class LT (a: Op, b: Op, p: Int) extends Insn
case class Eq (a: Op, b: Op, p: Int) extends Insn

sealed trait State
case object Halted          extends State
case object UnexpectedInput extends State
case object NeedInput       extends State
case class  Output(v: Int)  extends State

case class Proc(tape: Seq[Int]) {
  val t = Array(tape: _*)  // copy
  var ip = 0

  def decode = (t(ip)%100, t.slice(ip,ip+4)) match {
    case ( 1, Array(o, a,  b,  p)) => Add(Op(o,1,a), Op(o,2,b), p)
    case ( 2, Array(o, a,  b,  p)) => Mul(Op(o,1,a), Op(o,2,b), p)
    case ( 3, Array(o, p,     _*)) => In (p)
    case ( 4, Array(o, a,     _*)) => Out(Op(o,1,a))
    case ( 5, Array(o, a,  b, _*)) => JNZ(Op(o,1,a), Op(o,2,b))
    case ( 6, Array(o, a,  b, _*)) => JZ (Op(o,1,a), Op(o,2,b))
    case ( 7, Array(o, a,  b,  p)) => LT (Op(o,1,a), Op(o,2,b), p)
    case ( 8, Array(o, a,  b,  p)) => Eq (Op(o,1,a), Op(o,2,b), p)
    case (99, _                  ) => Halt
  }

  def step: State = decode match {
    case Halt       =>          Halted
    case In (p    ) =>          NeedInput
    case Out(a    ) => ip += 2; Output(a.get(t))
    case Add(a,b,p) => ip += 4; t(p) = a.get(t) + b.get(t)              ; step
    case Mul(a,b,p) => ip += 4; t(p) = a.get(t) * b.get(t)              ; step
    case JNZ(a,b  ) => ip  = if(a.get(t) != 0) b.get(t) else ip+3       ; step
    case JZ (a,b  ) => ip  = if(a.get(t) == 0) b.get(t) else ip+3       ; step
    case LT (a,b,p) => ip += 4; t(p) = if(a.get(t) <  b.get(t)) 1 else 0; step
    case Eq (a,b,p) => ip += 4; t(p) = if(a.get(t) == b.get(t)) 1 else 0; step
  }

  def stepWithInput(v: Int) = decode match {
    case In(p) => t(p) = v; ip += 2; step
    case _     => UnexpectedInput
  }
}

val tape = scala.io.Source.stdin.getLines.next.split(',').map{_.toInt}

def run(phases: Seq[Int]) = {
  def runAmp(amp: Proc, phase: Int, input: Int) = {
    amp.stepWithInput(phase)
    amp.stepWithInput(input) match {
      case Output(v) => Some(v)
      case _ => None
    }
  }

  val amps = Array.fill(5)(Proc(tape))
  var signal = 0
  for (i <- 0 to 4)
    signal = runAmp(amps(i), phases(i), signal).get
  signal
}

println((0 to 4).permutations.map(run).max)
