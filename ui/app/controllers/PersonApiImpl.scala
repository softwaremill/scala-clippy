package controllers

import com.softwaremill.clippy.PersonApi
import dal.PersonRepository
import scala.concurrent.ExecutionContext.Implicits.global

class PersonApiImpl(personRepository: PersonRepository) extends PersonApi {
  override def list() = personRepository.list()

  override def add(name: String, age: Int) = personRepository.create(name, age).map(_ => ())
}
