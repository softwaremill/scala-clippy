package controllers

import com.softwaremill.clippy.PersonApi
import dal.PersonRepository

class PersonApiImpl(personRepository: PersonRepository) extends PersonApi {
  override def list() = personRepository.list()
}
