package controllers

import java.util.UUID
import javax.inject.Inject

import database.{PersonsRepository, RepositoryError}
import models.domain.{Person => PersonModel}
import models.dto.ErrorResponse._
import models.dto.Person._
import models.dto.{CreatePerson, ErrorResponse, UpdatePerson}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Result}
import scala.concurrent.Future


class PersonController @Inject()(persons: PersonsRepository) extends Controller {
  private val log = Logger(this.getClass)

  private implicit val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext

  private def validateJsonBody[T](jsonBody: JsValue)(implicit reads: Reads[T]): Either[ErrorResponse, T] =
    jsonBody.validate[T].asEither.fold(
      errors => Left(ErrorResponse("Validation Error", fmtValidationResults(errors))),
      t => Right(t)
    )

  private def onValidationHandleSuccess[T](jsonBody: JsValue)(successFn: T => Future[Result])
                                          (implicit reads: Reads[T]): Future[Result] =
    validateJsonBody(jsonBody).fold(errorResponse => Future.successful(BadRequest(errorResponse.toJson)), successFn)

  private val ServiceError: Result = ServiceUnavailable(ErrorResponse("Service Error", Map.empty).toJson)

  private def onCreateHandleSuccess[T](futureCreateResult: Future[Either[RepositoryError, PersonModel]])
                                      (completeFn: PersonModel => Result): Future[Result] =
    futureCreateResult.map {
      case Right(createdPerson) => completeFn(createdPerson)
      case Left(_) => ServiceError
    }

  private def onUpdateHandleSuccess[T](futureUpdateResult: Future[Either[RepositoryError, PersonModel]])
                                      (completeFn: PersonModel => Result): Future[Result] =
    futureUpdateResult.map {
      case Right(updatedPerson) => completeFn(updatedPerson)
      case Left(_) => ServiceError
    }

  private def onFindHandleSuccess[T](futureFindResult: Future[Either[RepositoryError, Option[PersonModel]]])
                                    (completeFn: PersonModel => Future[Result]): Future[Result] =
    futureFindResult.flatMap {
      case Right(Some(person)) => completeFn(person)
      case Right(None) => Future.successful(NotFound(ErrorResponse("Resource not found", Map.empty).toJson))
      case Left(_) => Future.successful(ServiceError)
    }

  private def onDeleteHandleAll[T](futureDeleteResult: Future[Either[RepositoryError, UUID]]): Future[Result] =
    futureDeleteResult.map {
      case Right(uuid) => Ok(Json toJson Map("id" -> uuid.toString))
      case Left(_) => ServiceError
    }

  def create: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      log.info("POST /persons")
      onValidationHandleSuccess[CreatePerson](request.body) {
        createPersonRequest => {
          val personModel: PersonModel = createPersonRequest.toModel
          onCreateHandleSuccess(persons.create(personModel)) {
            person => Created(person.toDTO.toJson)
          }
        }
      }
  }

  def read(personId: UUID): Action[AnyContent] = Action.async {
    log.info(s"GET /persons/$personId")
    onFindHandleSuccess(persons.find(personId)) {
      person => Future.successful(Ok(person.toDTO.toJson))
    }
  }

  def update(personId: UUID): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      log.info(s"PUT /persons/$personId")
      onValidationHandleSuccess[UpdatePerson](request.body) {
        updatePersonRequest =>
          onFindHandleSuccess(persons.find(personId)) {
            foundPerson => {
              val updatedPerson = updatePersonRequest.toModel(foundPerson)
              onUpdateHandleSuccess(persons.update(updatedPerson)) {
                updatedPerson => Ok(updatedPerson.toDTO.toJson)
              }
            }
          }
      }
  }

  def delete(personId: UUID): Action[AnyContent] = Action.async {
    log.info(s"DELETE /persons/$personId")
    onFindHandleSuccess(persons.find(personId)) {
      person => onDeleteHandleAll(persons.delete(personId))
    }
  }

  def readAll: Action[AnyContent] = Action.async {
    log.info(s"GET /persons")
    persons.all
      .map {
        either => either.fold(
          _ => ServiceError,
          persons => {
            val jsonPersons = persons.map(eachPerson => eachPerson.toDTO.toJson)
            Ok(JsArray(jsonPersons))
          }
        )
      }
  }
}
