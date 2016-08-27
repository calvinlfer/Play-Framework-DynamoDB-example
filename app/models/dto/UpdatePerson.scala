package models.dto

import models.dto.Gender._
import models.domain.{Person => PersonModel}
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json}

case class UpdatePerson(firstName: Option[String], lastName: Option[String], studentId: Option[String], gender: Option[Gender])

object UpdatePerson {
  implicit val jsonWrites = Json.writes[UpdatePerson]
  implicit val jsonReads = (
    (JsPath \ "firstName").readNullable[String] //vanilla read followed by additional validators
      .filter(ValidationError("must be more than 2 characters"))(fnameOpt => fnameOpt.forall(_.length > 2)) and

      (JsPath \ "lastName").readNullable[String]
        .filter(ValidationError("must be more than 2 characters"))(lnameOpt => lnameOpt.forall(_.length > 2)) and

      (JsPath \ "studentId").readNullable[String]
        .filter(ValidationError("must be 10 digits"))(numberOpt => numberOpt.forall(_.length == 10))
        .filter(ValidationError("must be a number"))(numberOpt => numberOpt.forall(numberStr => numberStr.forall(Character.isDigit))) and

      (JsPath \ "gender").readNullable[Gender]

    ) (UpdatePerson.apply _)

  implicit class UpdatePersonOps(person: UpdatePerson) {
    def toModel(source: PersonModel): PersonModel =
      source.copy(
        firstName = person.firstName.getOrElse(source.firstName),
        lastName = person.lastName.getOrElse(source.lastName),
        studentId = person.studentId.getOrElse(source.studentId),
        gender = person.gender.map(_.toModel).getOrElse(source.gender)
      )
  }
}
