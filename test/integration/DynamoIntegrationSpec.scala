package integration

import java.util.UUID

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import config.DynamoDB.Settings
import models.dto.CreatePerson
import models.dto.Gender.{Gender, _}
import org.scalatest.{FunSuite, MustMatchers}
import org.scalatestplus.play._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import DynamoIntegrationSpec._

import scala.concurrent.ExecutionContext.{global => globalExecutionContext}

/**
  * An example of a DynamoDB integration test. This must be run in conjunction with local DynamoDB. The easiest way to
  * run this is via `activator test` or `sbt test` which uses Localytics' sbt-dynamodb to spawn a local version of
  * DynamoDB on port 8000 right before all the tests run and perform cleanup right after.
  */
class DynamoIntegrationSpec extends FunSuite with MustMatchers with OneAppPerTest {
  val application = new GuiceApplicationBuilder().build

  implicit val ec = globalExecutionContext
  val examplePerson = CreatePerson("Cal", "Fer", "0123456789", Male)

  test("Sending valid JSON to POST /persons responds with same valid JSON") {
    withDatabase(application) {
      val Some(result) = route(application, FakeRequest(POST, "/persons").withJsonBody(Json.toJson(examplePerson)))
      status(result) mustEqual CREATED
      contentType(result) mustEqual Some("application/json")

      val responseNode = Json.parse(contentAsString(result))
      (responseNode \ "firstName").as[String] mustEqual examplePerson.firstName
      (responseNode \ "lastName").as[String] mustEqual examplePerson.lastName
      (responseNode \ "studentId").as[String] mustEqual examplePerson.studentId
      (responseNode \ "gender").as[Gender] mustEqual examplePerson.gender
    }
  }

  test("POST /persons with valid json followed by GET /persons/{returned UUID from POST} returns a Person in JSON") {
    withDatabase(application) {
      val Some(postResult) = route(application, FakeRequest(POST, "/persons").withJsonBody(Json.toJson(examplePerson)))
      val postResponseNode = Json.parse(contentAsString(postResult))
      val uuid: UUID = (postResponseNode \ "id").as[UUID]

      val Some(getResult) = route(application, FakeRequest(GET, s"/persons/$uuid"))
      status(getResult) mustEqual OK
      contentType(getResult) mustEqual Some("application/json")

      val getResponseNode = Json.parse(contentAsString(getResult))
      (getResponseNode \ "firstName").as[String] mustEqual examplePerson.firstName
      (getResponseNode \ "lastName").as[String] mustEqual examplePerson.lastName
      (getResponseNode \ "studentId").as[String] mustEqual examplePerson.studentId
      (getResponseNode \ "gender").as[Gender] mustEqual examplePerson.gender
    }
  }

  test("POST /persons with valid json followed by GET /persons returns a List of 1 Person in JSON") {
    withDatabase(application) {
      val Some(postResult) = route(application, FakeRequest(POST, "/persons").withJsonBody(Json.toJson(examplePerson)))
      val Some(getResult) = route(application, FakeRequest(GET, s"/persons"))
      status(getResult) mustEqual OK
      contentType(getResult) mustEqual Some("application/json")

      val getPersonsAsNode = Json.parse(contentAsString(getResult))
      val getFirstPerson = getPersonsAsNode(0)
      (getFirstPerson \ "firstName").as[String] mustEqual examplePerson.firstName
      (getFirstPerson \ "lastName").as[String] mustEqual examplePerson.lastName
      (getFirstPerson \ "studentId").as[String] mustEqual examplePerson.studentId
      (getFirstPerson \ "gender").as[Gender] mustEqual examplePerson.gender
    }
  }

  test("POST /persons with valid json followed by DELETE /persons/{returned UUID from POST} deletes a Person") {
    withDatabase(application) {
      val Some(postResult) = route(application, FakeRequest(POST, "/persons").withJsonBody(Json.toJson(examplePerson)))
      val postResponseNode = Json.parse(contentAsString(postResult))
      val uuid: UUID = (postResponseNode \ "id").as[UUID]

      val Some(deleteResult) = route(application, FakeRequest(DELETE, s"/persons/$uuid"))
      status(deleteResult) mustEqual OK
      contentType(deleteResult) mustEqual Some("application/json")
      val responseNode = Json.parse(contentAsString(deleteResult))
      (responseNode \ "id").as[UUID] mustEqual uuid
    }
  }

  test("POST /persons with valid json followed by UPDATE /persons/{returned UUID from POST} updates a Person") {
    withDatabase(application) {
      val Some(postResult) = route(application, FakeRequest(POST, "/persons").withJsonBody(Json.toJson(examplePerson)))
      val postResponseNode = Json.parse(contentAsString(postResult))
      val uuid: UUID = (postResponseNode \ "id").as[UUID]

      val Some(updateResult) = route(application, FakeRequest(PUT, s"/persons/$uuid").withJsonBody(Json.toJson(examplePerson.copy(firstName = "calvin"))))
      status(updateResult) mustEqual OK
      contentType(updateResult) mustEqual Some("application/json")
      val responseNode = Json.parse(contentAsString(updateResult))
      (responseNode \ "firstName").as[String] mustEqual "calvin"
      (responseNode \ "lastName").as[String] mustEqual examplePerson.lastName
      (responseNode \ "studentId").as[String] mustEqual examplePerson.studentId
      (responseNode \ "gender").as[Gender] mustEqual examplePerson.gender
    }
  }
}

object DynamoIntegrationSpec {
  /**
    * Runs your test within the context of the presence of Local DynamoDB
    * @param app is the Play Application Registry used to obtain the Guice injector to ask for the Dynamo client in
    *            order to create the table
    * @param thunk is the expression you want to test
    * @tparam T is the return type of your test expression, this allows your code to be written without explicitly
    *           having to stick a Unit return at the end of every test expression that uses the database
    */
  def withDatabase[T](app: Application)(thunk: => T): Unit  = {
    // obtain Amazon DynamoDB client from Guice DI mechanism
    val client = app.injector.instanceOf(classOf[AmazonDynamoDBAsyncClient])
    val tableName = Settings(app.configuration).tableName.getOrElse("persons-test")

    LocalDynamoDB.usingTable(client)(tableName)('id -> S)(thunk)
  }
}
