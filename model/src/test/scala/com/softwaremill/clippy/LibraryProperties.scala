package com.softwaremill.clippy

import org.scalacheck.Prop._
import org.scalacheck.Properties

class LibraryProperties extends Properties("Library") {
  property("obj -> xml -> obj") =
    forAll { (gid: String, aid: String, v: String) =>
      val l = Library(gid, aid, v)
      Library.fromXml(l.toXml).contains(l)
    }
}