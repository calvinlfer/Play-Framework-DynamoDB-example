package unit

import models.dto.Gender
import models.dto.Gender.Gender
import org.scalatest.{FunSuite, MustMatchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class EnumerationHelperTest extends FunSuite with MustMatchers {

  test("Sending valid JSON produces a correct Scala Enumeration") {
    val jsonAst = JsString("Female")
    val result: JsResult[Gender] = Gender.enumReads.reads(jsonAst)
    result.isError mustBe false
    result.isSuccess mustBe true
    result.fold(
      _ => fail(),
      gender => gender mustBe Gender.Female
    )
  }

  test("Sending invalid JSON string produces a JsError") {
    val jsonAst = JsString("")
    val result: JsResult[Gender] = Gender.enumReads.reads(jsonAst)
    result.isError mustBe true
    result.isSuccess mustBe false
    result.fold(
      {
        case (_: JsPath, firstValidationError :: Nil) :: Nil =>
          firstValidationError mustBe ValidationError("Expected values: (Male, Female) but you provided: ''")

        case _ => fail()
      },
      _ => fail()
    )
  }

  test("Sending invalid JSON type produces a JsError") {
    val jsonAst = JsNumber(1)
    val result: JsResult[Gender] = Gender.enumReads.reads(jsonAst)
    result.isError mustBe true
    result.isSuccess mustBe false
    result.fold(
      {
        case (_: JsPath, firstValidationError :: Nil) :: Nil =>
          firstValidationError mustBe ValidationError("String value expected")

        case _ => fail()
      },
      _ => fail()
    )
  }

  test("Sending a valid Scala enumeration value produces a valid JSON AST") {
    val male = Gender.Male
    val jsonAst: JsValue = Gender.enumWrites.writes(male)
    jsonAst mustBe JsString("Male")
  }
}
