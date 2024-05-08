#!/usr/bin/env amm2

type Num = Long

case class Tape(var is: Array[Num]) {
  def apply (p: Num        ) = { resize(p.toInt + 1); is(p.toInt)     }
  def update(p: Num, v: Num) = { resize(p.toInt + 1); is(p.toInt) = v }
  def resize(s: Int        ) =
    if (s >= is.size) is = is padTo (s max 2*is.size, 0L)
}

// operands: rvalues and lvalues
sealed trait R           { def v(implicit t: Tape, rb: Num): Num   }
sealed trait L extends R { def p(implicit          rb: Num): Num
                           def v(implicit t: Tape, rb: Num) = t(p) }

case class Pos(x: Num) extends L { def p(implicit          rb: Num) = x    }
case class Imm(x: Num) extends R { def v(implicit t: Tape, rb: Num) = x    }
case class Idx(x: Num) extends L { def p(implicit          rb: Num) = x+rb }

sealed trait Insn
case object Halt extends Insn
case class Add(a:R, b:R, l:L) extends Insn
case class Mul(a:R, b:R, l:L) extends Insn
case class In (l:L          ) extends Insn
case class Out(r:R          ) extends Insn
case class JNZ(a:R, b:R     ) extends Insn
case class JZ (a:R, b:R     ) extends Insn
case class LT (a:R, b:R, l:L) extends Insn
case class Eq (a:R, b:R, l:L) extends Insn
case class RB (r:R          ) extends Insn

sealed trait State
case object Halted          extends State
case object UnexpectedInput extends State
case object NeedInput       extends State
case class  Output(v: Num)  extends State

case class Proc(tape: Array[Num]) {
  implicit val t = Tape(tape)
  implicit var rb = 0L
  var ip = 0

  @annotation.tailrec
  final def step: State = decode match {
    case Halt       =>          Halted
    case In (l    ) =>          NeedInput
    case Out(r    ) => ip += 2; Output(r.v)

    case RB (r    ) => ip += 2; rb += r.v                       ; step
    case Add(a,b,l) => ip += 4; t(l.p) = a.v + b.v              ; step
    case Mul(a,b,l) => ip += 4; t(l.p) = a.v * b.v              ; step
    case LT (a,b,l) => ip += 4; t(l.p) = if(a.v <  b.v) 1 else 0; step
    case Eq (a,b,l) => ip += 4; t(l.p) = if(a.v == b.v) 1 else 0; step
    case JNZ(a,b  ) => ip  = if(a.v != 0) b.v.toInt else ip + 3 ; step
    case JZ (a,b  ) => ip  = if(a.v == 0) b.v.toInt else ip + 3 ; step
  }
  def stepWithInput(v: Num) = decode match {
    case In(l)      => ip += 2; t(l.p) = v                      ; step
    case _          =>          UnexpectedInput
  }

  def decode = {
    val opnds = {
      val vals  = t.is.slice (ip + 1, ip + 4)
      val modes = (t.is(ip) / 100).toString
        .reverse.padTo (vals.size, '0')
        .map           {_ - '0'}

      (modes, vals).zipped map {
        case (0, x) => Pos(x)
        case (1, x) => Imm(x)
        case (2, x) => Idx(x)
      }
    }

    (t.is(ip) % 100, opnds) match {
      case ( 1, Seq(a:R, b:R, l:L)) => Add(a,b,l)
      case ( 2, Seq(a:R, b:R, l:L)) => Mul(a,b,l)
      case ( 3, Seq(l:L,       _*)) => In (l    )
      case ( 4, Seq(r:R,       _*)) => Out(r    )
      case ( 5, Seq(a:R, b:R,  _*)) => JNZ(a,b  )
      case ( 6, Seq(a:R, b:R,  _*)) => JZ (a,b  )
      case ( 7, Seq(a:R, b:R, l:L)) => LT (a,b,l)
      case ( 8, Seq(a:R, b:R, l:L)) => Eq (a,b,l)
      case ( 9, Seq(r:R,       _*)) => RB (r    )
      case (99, _                 ) => Halt
    }
  }
}

val p = Proc(scala.io.Source.stdin.getLines.next.split(',').map{_.toLong})
p.step
println(p stepWithInput 2)
