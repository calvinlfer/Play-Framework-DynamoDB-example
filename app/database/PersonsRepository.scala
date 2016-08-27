package database

import java.util.UUID

import models.domain.Person

import scala.concurrent.Future

sealed trait RepositoryError
case object ConnectionError extends RepositoryError
case object DeserializationError extends RepositoryError

trait PersonsRepository {
  def create(person: Person): Future[Either[RepositoryError, Person]]

  def find(personId: UUID): Future[Either[RepositoryError, Option[Person]]]

  def update(person: Person): Future[Either[RepositoryError, Person]]

  def delete(personId: UUID): Future[Either[RepositoryError, UUID]]

  def all: Future[Either[RepositoryError, Seq[Person]]]
}
