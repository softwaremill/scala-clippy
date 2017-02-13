package com.softwaremill.clippy

import scala.reflect.internal.util.{NoPosition, Position, SourceFile}
import scala.reflect.macros.Attachments

class DelegatingPosition(delegate: Position, colorsConfig: ColorsConfig) extends Position {

  // used by scalac to report errors
  override def showError(msg: String): String = highlight(delegate.showError(msg))

  // used by sbt
  override def lineContent: String = highlight(delegate.lineContent)

  def highlight(str: String): String = colorsConfig match {
    case e: ColorsConfig.Enabled =>
      Highlighter.defaultHighlight(
        str.toVector,
        e.comment, e.`type`, e.literal,
        e.keyword, e.reset
      ).mkString
    case _ => str
  }

  // impl copied

  override def fail(what: String): Nothing = throw new UnsupportedOperationException(s"Position.$what on $this")

  // simple delegates with position wrapping when position is returned

  @scala.deprecated("use `point`")
  override def offset: Option[Int] = delegate.offset

  override def all: Set[Any] = delegate.all

  @scala.deprecated("use `focus`")
  override def toSingleLine: Position = DelegatingPosition.wrap(delegate.toSingleLine, colorsConfig)

  override def pos: Position = DelegatingPosition.wrap(delegate.pos, colorsConfig)

  override def get[T](implicit evidence$2: ClassManifest[T]): Option[T] = delegate.get(evidence$2)

  override def finalPosition: Pos = DelegatingPosition.wrap(delegate.finalPosition, colorsConfig)

  override def withPos(newPos: Position): Attachments { type Pos = DelegatingPosition.this.Pos } = delegate.withPos(newPos)

  @scala.deprecated("use `line`")
  override def safeLine: Int = delegate.safeLine

  override def isTransparent: Boolean = delegate.isTransparent

  override def contains[T](implicit evidence$3: ClassManifest[T]): Boolean = delegate.contains(evidence$3)

  override def isOffset: Boolean = delegate.isOffset

  @scala.deprecated("use `showDebug`")
  override def dbgString: String = delegate.dbgString

  override def isOpaqueRange: Boolean = delegate.isOpaqueRange

  override def update[T](attachment: T)(implicit evidence$4: ClassManifest[T]): Attachments { type Pos = DelegatingPosition.this.Pos } = delegate.update(attachment)(evidence$4)

  override def pointOrElse(alt: Int): Int = delegate.pointOrElse(alt)

  @scala.deprecated("use `finalPosition`")
  override def inUltimateSource(source: SourceFile): Position = DelegatingPosition.wrap(delegate.inUltimateSource(source), colorsConfig)

  override def isDefined: Boolean = delegate.isDefined

  override def makeTransparent: Position = DelegatingPosition.wrap(delegate.makeTransparent, colorsConfig)

  override def isRange: Boolean = delegate.isRange

  override def source: SourceFile = delegate.source

  override def remove[T](implicit evidence$5: ClassManifest[T]): Attachments { type Pos = DelegatingPosition.this.Pos } = delegate.remove(evidence$5)

  override def withStart(start: Int): Position = DelegatingPosition.wrap(delegate.withStart(start), colorsConfig)

  @scala.deprecated("use `lineCaret`")
  override def lineWithCarat(maxWidth: Int): (String, String) = delegate.lineWithCarat(maxWidth)

  override def start: Int = delegate.start

  override def withPoint(point: Int): Position = DelegatingPosition.wrap(delegate.withPoint(point), colorsConfig)

  override def point: Int = delegate.point

  override def isEmpty: Boolean = delegate.isEmpty

  override def end: Int = delegate.end

  override def withEnd(end: Int): Position = DelegatingPosition.wrap(delegate.withEnd(end), colorsConfig)

  @scala.deprecated("Use `withSource(source)` and `withShift`")
  override def withSource(source: SourceFile, shift: Int): Position = DelegatingPosition.wrap(delegate.withSource(source, shift), colorsConfig)

  override def withSource(source: SourceFile): Position = DelegatingPosition.wrap(delegate.withSource(source), colorsConfig)

  @scala.deprecated("Use `start` instead")
  override def startOrPoint: Int = delegate.startOrPoint

  override def withShift(shift: Int): Position = DelegatingPosition.wrap(delegate.withShift(shift), colorsConfig)

  @scala.deprecated("Use `end` instead")
  override def endOrPoint: Int = delegate.endOrPoint

  override def focusStart: Position = DelegatingPosition.wrap(delegate.focusStart, colorsConfig)

  override def focus: Position = DelegatingPosition.wrap(delegate.focus, colorsConfig)

  override def focusEnd: Position = DelegatingPosition.wrap(delegate.focusEnd, colorsConfig)

  override def |(that: Position, poses: Position*): Position = DelegatingPosition.wrap(delegate.|(that, poses: _*), colorsConfig)

  override def |(that: Position): Position = DelegatingPosition.wrap(delegate.|(that), colorsConfig)

  override def ^(point: Int): Position = DelegatingPosition.wrap(delegate.^(point), colorsConfig)

  override def |^(that: Position): Position = DelegatingPosition.wrap(delegate.|^(that), colorsConfig)

  override def ^|(that: Position): Position = DelegatingPosition.wrap(delegate.^|(that), colorsConfig)

  override def union(pos: Position): Position = DelegatingPosition.wrap(delegate.union(pos), colorsConfig)

  override def includes(pos: Position): Boolean = delegate.includes(pos)

  override def properlyIncludes(pos: Position): Boolean = delegate.properlyIncludes(pos)

  override def precedes(pos: Position): Boolean = delegate.precedes(pos)

  override def properlyPrecedes(pos: Position): Boolean = delegate.properlyPrecedes(pos)

  override def sameRange(pos: Position): Boolean = delegate.sameRange(pos)

  override def overlaps(pos: Position): Boolean = delegate.overlaps(pos)

  override def line: Int = delegate.line

  override def column: Int = delegate.column

  override def lineCaret: String = delegate.lineCaret

  @scala.deprecated("use `lineCaret`")
  override def lineCarat: String = delegate.lineCarat

  override def showDebug: String = delegate.showDebug

  override def show: String = delegate.show
}

object DelegatingPosition {
  def wrap(pos: Position, colorsConfig: ColorsConfig): Position = {
    pos match {
      case NoPosition => pos
      case wrapped: DelegatingPosition => wrapped
      case _ => new DelegatingPosition(pos, colorsConfig)
    }
  }
}