package com.softwaremill.clippy

import org.scalacheck.Prop._
import org.scalacheck.Properties

class CompilationErrorTest extends Properties("CompilationError") {
  property("obj -> xml -> obj works for type mismatch error") =
    forAll { (found: String, foundExpandsTo: Option[String], required: String, requiredExpandsTo: Option[String]) =>
      forAll { (required: String) =>
        val e = TypeMismatchError(found, foundExpandsTo, required, requiredExpandsTo)
        CompilationError.fromXml(e.toXml).contains(e)
      }
    }

  property("obj -> xml -> obj works for not found error") =
    forAll { (what: String) =>
      val e = NotFoundError(what)
      CompilationError.fromXml(e.toXml).contains(e)
    }
}
