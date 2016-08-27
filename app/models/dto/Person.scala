package models.dto

import java.util.UUID

import models.dto.Gender.Gender
import play.api.libs.json.{JsValue, Json}
import models.domain.{Person => PersonModel}

case class Person(id: UUID, firstName: String, lastName: String, studentId: String, gender: Gender)

object Person {
  import models.dto.Gender._
  implicit val jsonWrites = Json.writes[Person]

  implicit class PersonDomainModel2PersonDTO(domain: PersonModel) {
    def toDTO: Person = Person(domain.id, domain.firstName, domain.lastName, domain.studentId, domain.gender.toDTO)
  }

  implicit class PersonDTOJsonOps(person: Person) {
    def toJson: JsValue = Json.toJson(person)
  }
}
