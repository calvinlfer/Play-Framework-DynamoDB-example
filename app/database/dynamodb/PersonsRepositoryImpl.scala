package database.dynamodb

import java.util.UUID
import javax.inject.{Inject, Named}

import cats.data.Xor
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.gu.scanamo.error.DynamoReadError._
import com.gu.scanamo.error._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{ScanamoAsync, Table}
import DynamoDBFormatHelpers._
import config.{DynamoDB, Guice}
import database.{ConnectionError, DeserializationError, PersonsRepository, RepositoryError}
import models.domain.Person
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
  * Note that @Inject is required due to the fact that in order for Guice to work,
  * classes must have either one (and only one) constructor annotated with @Inject
  * or a zero-argument constructor that is not private
  *
  * @param client           a fully configured Amazon DynamoDB client
  * @param executionContext an execution context that allows Futures to execute (bulkhead pattern)
  */
class PersonsRepositoryImpl @Inject()(config: Configuration, client: AmazonDynamoDBAsyncClient,
                                      @Named(Guice.DynamoRepository) executionContext: ExecutionContext) extends PersonsRepository {
  val log = Logger(this.getClass)
  lazy val tableName = {
    val result = DynamoDB.Settings(config).tableName.getOrElse("local-members-table")
    log.info(s"Table: $result")
    result
  }
  val personsTable = Table[Person](tableName)

  implicit val ec = executionContext

  private def captureAndFail[R](msg: String): PartialFunction[Throwable, Either[RepositoryError, R]] = {
    case t: Throwable =>
      log.error(msg, t)
      Left[RepositoryError, R](ConnectionError)
  }

  override def create(person: Person): Future[Either[RepositoryError, Person]] = {
    val putRequest = personsTable.put(person)
    val result = ScanamoAsync.exec(client)(putRequest)
    result.map(_ => Right(person)).recover(captureAndFail[Person]("Create Person failed"))
  }

  override def update(person: Person): Future[Either[RepositoryError, Person]] = create(person)

  override def all: Future[Either[RepositoryError, Seq[Person]]] = {
    val result = ScanamoAsync.scan[Person](client)(tableName)
    val manipulatedResult: Future[Either[RepositoryError, Seq[Person]]] =
    // non-strict => strict (this will cause all the results to be pulled from DynamoDB)
      result
        .map(_.toList)
        .map(listXor => {
          val badResults = listXor.collect {
            case Xor.Left(dynamoReadError) => dynamoReadError
          }

          // Each individual result has the potential to fail and so we capture this
          badResults.foreach((dynamoError: DynamoReadError) => log.error(s"all: ${describe(dynamoError)}"))

          val goodResults = listXor.collect {
            case Xor.Right(person) => person
          }
          Right(goodResults)
        })
    manipulatedResult.recover(captureAndFail("Retrieve (all) persons failed"))
  }

  override def delete(personId: UUID): Future[Either[RepositoryError, UUID]] = {
    val deleteRequest = personsTable.delete('id -> personId.toString)
    val result = ScanamoAsync.exec(client)(deleteRequest)
    result.map(_ => Right(personId)).recover(captureAndFail(s"Delete person with ID: ${personId.toString} failed"))
  }

  override def find(personId: UUID): Future[Either[RepositoryError, Option[Person]]] = {
    val findRequest = personsTable.get('id -> personId.toString)
    val dynamoResult = ScanamoAsync.exec(client)(findRequest)
    val xorManipulatedResult = dynamoResult.map(
      (optionXor: Option[Xor[DynamoReadError, Person]]) =>
        // If we get a None, it means that that person does not exist so we return a Right None
        optionXor.fold(Xor.right[RepositoryError, Option[Person]](None)) {
          // Deserialization from (DynamoDB => Scala) has the potential to fail so we capture this
          xor =>
            xor.fold(
              dynamoReadError => {
                log.info(describe(dynamoReadError))
                Xor.left[RepositoryError, Option[Person]](DeserializationError)
              },
              (person: Person) =>
                Xor.right[RepositoryError, Option[Person]](Some(person))
            )
        }
    )
    val finalResult: Future[Either[RepositoryError, Option[Person]]] = xorManipulatedResult.map(_.toEither)
    finalResult.recover(captureAndFail(s"Find person with ID: ${personId.toString} failed"))
  }
}
