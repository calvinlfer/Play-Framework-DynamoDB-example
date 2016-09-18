package models.dto

import java.util.UUID

import models.domain.{Person => PersonModel}
import models.dto.Gender.Gender
import play.api.data.validation.ValidationError
import play.api.libs.json._

case class CreatePerson(firstName: String, lastName: String, studentId: String, gender: Gender)

object CreatePerson {
  implicit val jsonWrites = Json.writes[CreatePerson]
  implicit val jsonValidatedReads = Reads[CreatePerson] {
    json =>
      for {
        firstName <- (json \ "firstName").validate[String]
          .filter(JsError(ValidationError("must be more than 2 characters")))(fname => fname.length > 2)

        lastName <- (json \ "lastName").validate[String]
          .filter(JsError(ValidationError("must be more than 2 characters")))(lname => lname.length > 2)

        studentId <- (json \ "studentId").validate[String]
          .filter(JsError(ValidationError("must be a number")))(number => number.forall(Character.isDigit))
          .filter(JsError(ValidationError("must be 10 digits")))(number => number.length == 10)

        gender <- (json \ "gender").validate[Gender]
      } yield CreatePerson(firstName, lastName, studentId, gender)
  }

  implicit class CreatePersonOps(request: CreatePerson) {
    def toModel: PersonModel =
      PersonModel(UUID.randomUUID(), request.firstName, request.lastName, request.studentId, request.gender.toModel)
  }

}
