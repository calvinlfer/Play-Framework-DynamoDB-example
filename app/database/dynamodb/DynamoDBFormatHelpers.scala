package database.dynamodb

import java.util.UUID

import com.gu.scanamo._
import models.domain.Gender
import models.domain.Gender.Gender

/**
  * Scanamo format helpers that help with serialization and deserialization of custom formats like
  * Gender Enumerations and UUIDs
  */
object DynamoDBFormatHelpers {
  implicit def enumerationStringFormat: DynamoFormat[Gender] = DynamoFormat.coercedXmap[Gender, String, IllegalArgumentException] {
    genderString => Gender.withName(genderString)
  } {
    gender => gender.toString
  }

  implicit def uuidStringFormat: DynamoFormat[UUID] = DynamoFormat.coercedXmap[UUID, String, IllegalArgumentException] {
    uuidString => UUID.fromString(uuidString)
  } {
    uuid => uuid.toString
  }
}
