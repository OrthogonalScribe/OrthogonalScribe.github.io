#!/usr/bin/env amm2

type Num = Long

case class Tape(var is: Array[Num]) {
  def apply (a: Num        ) = { resize(a.toInt+1); is(a.toInt)     }
  def update(a: Num, v: Num) = { resize(a.toInt+1); is(a.toInt) = v }
  def resize(s: Int        ) =
    if (s >= is.size) is = is padTo (s max 2*is.size, 0L)
}

sealed trait Rval              { def v(implicit t: Tape, rb: Num): Num   }
sealed trait Lval extends Rval { def a(implicit          rb: Num): Num
                                 def v(implicit t: Tape, rb: Num) = t(a) }

case class Pos(x: Num) extends Lval { def a(implicit          rb: Num) = x    }
case class Imm(x: Num) extends Rval { def v(implicit t: Tape, rb: Num) = x    }
case class Idx(x: Num) extends Lval { def a(implicit          rb: Num) = x+rb }

sealed trait Insn
case object Halt extends Insn
case class Add(a: Rval, b: Rval, l: Lval) extends Insn
case class Mul(a: Rval, b: Rval, l: Lval) extends Insn
case class In (l: Lval                  ) extends Insn
case class Out(r: Rval                  ) extends Insn
case class JNZ(a: Rval, b: Rval         ) extends Insn
case class JZ (a: Rval, b: Rval         ) extends Insn
case class LT (a: Rval, b: Rval, l: Lval) extends Insn
case class Eq (a: Rval, b: Rval, l: Lval) extends Insn
case class RB (r: Rval                  ) extends Insn

sealed trait State
case object Halted          extends State
case object UnexpectedInput extends State
case object NeedInput       extends State
case class  Output(v: Num)  extends State

case class Proc(tape: Array[Num]) {
  implicit val t = Tape(tape)
  implicit var rb = 0L
  var ip = 0

  def stepWithInput(v: Int) = decode match {
    case In(l)      => ip += 2; t(l.a) = v                      ; step
    case _          =>          UnexpectedInput
  }
  def step: State = decode match {
    case Halt       =>          Halted
    case In (l    ) =>          NeedInput
    case Out(r    ) => ip += 2; Output(r.v)
    case RB (r    ) => ip += 2; rb += r.v                       ; step
    case Add(a,b,l) => ip += 4; t(l.a) = a.v + b.v              ; step
    case Mul(a,b,l) => ip += 4; t(l.a) = a.v * b.v              ; step
    case LT (a,b,l) => ip += 4; t(l.a) = if(a.v <  b.v) 1 else 0; step
    case Eq (a,b,l) => ip += 4; t(l.a) = if(a.v == b.v) 1 else 0; step
    case JNZ(a,b  ) => ip  = if(a.v != 0) b.v.toInt else ip+3   ; step
    case JZ (a,b  ) => ip  = if(a.v == 0) b.v.toInt else ip+3   ; step
  }

  def decode = {
    val opnds = {
      val vals  = t.is.slice(ip+1,ip+4)
      val modes = (t.is(ip)/100).toString
        .reverse.padTo (vals.size, '0')
        .map{_-'0'}

      (modes, vals).zipped map {
        case (0, v) => Pos(v)
        case (1, v) => Imm(v)
        case (2, v) => Idx(v)
      }
    }

    (t.is(ip)%100, opnds) match {
      case ( 1, Seq(a: Rval, b: Rval, l: Lval)) => Add(a,b,l)
      case ( 2, Seq(a: Rval, b: Rval, l: Lval)) => Mul(a,b,l)
      case ( 3, Seq(l: Lval,               _*)) => In (l)
      case ( 4, Seq(r: Rval,               _*)) => Out(r)
      case ( 5, Seq(a: Rval, b: Rval,      _*)) => JNZ(a,b)
      case ( 6, Seq(a: Rval, b: Rval,      _*)) => JZ (a,b)
      case ( 7, Seq(a: Rval, b: Rval, l: Lval)) => LT (a,b,l)
      case ( 8, Seq(a: Rval, b: Rval, l: Lval)) => Eq (a,b,l)
      case ( 9, Seq(r: Rval,               _*)) => RB (r)
      case (99, _                             ) => Halt
    }
  }
}

val p = Proc(scala.io.Source.stdin.getLines.next.split(',').map{_.toLong})
p.step
println(p stepWithInput 1)
