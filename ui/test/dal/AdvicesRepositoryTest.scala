package dal

import com.softwaremill.clippy._
import com.softwaremill.id.DefaultIdGenerator
import util.BaseSqlSpec

class AdvicesRepositoryTest extends BaseSqlSpec {
  it should "store & read an advice" in {
    // given
    val ar = new AdvicesRepository(database, new DefaultIdGenerator())

    // when
    val stored = ar.store("zzz", "yyy", TypeMismatchError[RegexT](RegexT("x"), None, RegexT("y"), None, None), "z",
      AdviceState.Pending, Library("g", "a", "1"), Contributor(None, None, Some("t")), Some("c")).futureValue

    // then
    val r = ar.findAll().futureValue
    r should have size (1)

    val found = r.head

    stored should be (found)
    found.errorTextRaw should be ("zzz")
    found.patternRaw should be ("yyy")
    found.compilationError should be (TypeMismatchError(RegexT("x"), None, RegexT("y"), None, None))
    found.advice should be ("z")
    found.state should be (AdviceState.Pending)
    found.library should be (Library("g", "a", "1"))
    found.contributor should be (Contributor(None, None, Some("t")))
    found.comment should be (Some("c"))
  }
}
