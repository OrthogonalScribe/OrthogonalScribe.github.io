#!/usr/bin/env amm2

object ICP {
  // IntCodeProcess
  type Num = Long

  case class Tape(var is: Array[Num]) {
    override def clone = Tape(is.clone)
    def apply (p: Num        ) = { resize(p.toInt + 1); is(p.toInt)     }
    def update(p: Num, v: Num) = { resize(p.toInt + 1); is(p.toInt) = v }
    def resize(s: Int        ) =
      if (s >= is.size) is = is padTo (s max 2*is.size, 0L)
  }

  sealed trait Rval              { def v(implicit t: Tape, rb: Num): Num   }
  sealed trait Lval extends Rval { def p(implicit          rb: Num): Num
                                   def v(implicit t: Tape, rb: Num) = t(p) }

  case class Pos(x:Num) extends Lval { def p(implicit          rb: Num) = x    }
  case class Imm(x:Num) extends Rval { def v(implicit t: Tape, rb: Num) = x    }
  case class Idx(x:Num) extends Lval { def p(implicit          rb: Num) = x+rb }

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

  def apply(tape: Array[Num]): ICP = ICP(Tape(tape))
}

case class ICP(t0: ICP.Tape, rb0: ICP.Num = 0L, var ip: Int = 0) {
  import ICP._

  implicit val t = t0
  implicit var rb = rb0

  override def clone = ICP(t.clone, rb, ip)

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
        case (0, x) => ICP.Pos(x)
        case (1, x) => Imm(x)
        case (2, x) => Idx(x)
      }
    }

    (t.is(ip) % 100, opnds) match {
      case ( 1, Seq(a: Rval, b: Rval, l: Lval)) => Add(a,b,l)
      case ( 2, Seq(a: Rval, b: Rval, l: Lval)) => Mul(a,b,l)
      case ( 3, Seq(l: Lval,               _*)) => In (l    )
      case ( 4, Seq(r: Rval,               _*)) => Out(r    )
      case ( 5, Seq(a: Rval, b: Rval,      _*)) => JNZ(a,b  )
      case ( 6, Seq(a: Rval, b: Rval,      _*)) => JZ (a,b  )
      case ( 7, Seq(a: Rval, b: Rval, l: Lval)) => LT (a,b,l)
      case ( 8, Seq(a: Rval, b: Rval, l: Lval)) => Eq (a,b,l)
      case ( 9, Seq(r: Rval,               _*)) => RB (r    )
      case (99, _                             ) => Halt
    }
  }
}

// ==========================================================

case class Pos(r: Int, c: Int) {
  def +(p: Pos) = Pos(p.r+r, p.c+c)
  def neighbors = allMoves map {this + _.delta}
  def moveFrom(p: Pos) =
    allMoves.find { _.delta == Pos(r-p.r,c-p.c) }.get
}

import ICP.Num

sealed abstract class Move(val num: Num, val delta: Pos)
case object North extends Move(1, Pos(-1, 0))
case object South extends Move(2, Pos( 1, 0))
case object West  extends Move(3, Pos( 0,-1))
case object East  extends Move(4, Pos( 0, 1))
val allMoves: Seq[Move] = Seq(North, South, West, East)

sealed abstract class Status(val num: Num)
case object NokWall  extends Status(0)
case object Ok       extends Status(1)
case object OkOxygen extends Status(2)
val toStatus: Map[Num,Status] =
  Seq(NokWall, Ok, OkOxygen).map{s => s.num->s}.toMap

sealed trait Tile
case object Wall    extends Tile
case object Empty   extends Tile
case object Oxygen  extends Tile

// ====================================================================
// part 1

val proc0 = ICP(scala.io.Source.stdin.getLines.next.split(',').map{_.toLong})

import collection.{mutable => m}

val pos0 = Pos(0,0)
val maze = m.Map[Pos,Tile](pos0->Empty)

case class PosProc(po: Pos, pr: ICP)

def bfs1(depth: Int, seen: m.Set[Pos], wave: Seq[PosProc]): Unit = {
  def genNext(pp: PosProc) =
    for {
      n <- pp.po.neighbors
      if !seen(n)
    } yield {
      seen += n
      val p1 = pp.pr.clone
      val s = {p1.step; p1 stepWithInput (n moveFrom pp.po).num} match {
        case ICP.Output(n) => toStatus(n)
      }
      s match {
        case NokWall  => maze(n) = Wall  ; None
        case Ok       => maze(n) = Empty ; Some(PosProc(n, p1))
        case OkOxygen => maze(n) = Oxygen; Some(PosProc(n, p1))
      }
    }

  dump1

  if (wave.nonEmpty)
    bfs1(1+depth, seen, wave.flatMap(genNext).flatten)
}
bfs1(1, m.Set(pos0), Seq(PosProc(pos0,proc0)))

def dump1 = {
  val rs = maze.keys.map{_.r}
  val cs = maze.keys.map{_.c}
  println(s"P3\n${(cs.min to cs.max).size} ${(rs.min to rs.max).size}\n255")
  for   (r <- rs.min to rs.max) {
    for (c <- cs.min to cs.max)
      maze.get(Pos(r,c)) match {
        case Some(Wall  ) => print("100 0 0 ")
        case Some(Empty ) => print("100 100 100 ")
        case Some(Oxygen) => print("178 255 255 ")
        case None         => print("0 0 0 ")
      }
      println
  }
}

// =============================================================
// part 2, reuses maze only

@annotation.tailrec
def bfs2(depth: Int, seen: m.Set[Pos], wave: Seq[Pos]): Unit = {
  def isValid(p: Pos) = !seen(p) && maze(p) == Empty
  val nextWave = wave flatMap {_.neighbors} filter (isValid)
  seen ++= nextWave

  dump2(seen)

  if (nextWave.nonEmpty)
    bfs2(1+depth, seen, nextWave)
}

val posOxy = maze.find{_._2==Oxygen}.get._1
bfs2(0, m.Set(posOxy), Seq(posOxy))

def dump2(seen: m.Set[Pos]) = {
  val rs = maze.keys.map{_.r}
  val cs = maze.keys.map{_.c}
  println(s"P3\n${(cs.min to cs.max).size} ${(rs.min to rs.max).size}\n255")
  for   (r <- rs.min to rs.max) {
    for (c <- cs.min to cs.max)
      maze.get(Pos(r,c)) match {
        case Some(Wall  ) => print("100 50 50 ")
        case Some(Empty ) => print(if (seen(Pos(r,c))) "100 100 255 " else "100 100 100 ")
        case Some(Oxygen) => print("178 255 255 ")
        case None         => print("0 0 0 ")
      }
      println
  }
}
