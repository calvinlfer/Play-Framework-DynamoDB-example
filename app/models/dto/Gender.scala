package models.dto

import models.EnumerationHelpers
import play.api.libs.json.{Reads, Writes}
import models.domain.{Gender => GenderModel}

object Gender extends Enumeration {
  type Gender = Value
  val Male, Female = Value

  implicit val enumReads: Reads[Gender] = EnumerationHelpers.enumReads(Gender)
  implicit val enumWrites: Writes[Gender] = EnumerationHelpers.enumWrites

  implicit class GenderDTO2GenderModel(gender: Gender) {
    def toModel: GenderModel.Gender = gender match {
      case Gender.Male => GenderModel.Male
      case Gender.Female => GenderModel.Female
    }
  }

  implicit class GenderModel2GenderDTO(gender: GenderModel.Gender) {
    def toDTO: Gender.Gender = gender match {
      case GenderModel.Male => Gender.Male
      case GenderModel.Female => Gender.Female
    }
  }
}
