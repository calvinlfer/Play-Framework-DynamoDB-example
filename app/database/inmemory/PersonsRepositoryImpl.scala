package database.inmemory

import java.util.UUID

import database.{PersonsRepository, RepositoryError}
import models.domain.Person

import scala.collection.parallel.mutable
import scala.concurrent.Future


class PersonsRepositoryImpl extends PersonsRepository {
  private val store = mutable.ParTrieMap.empty[UUID, Person]
  private implicit val ec = scala.concurrent.ExecutionContext.Implicits.global


  override def create(person: Person): Future[Either[RepositoryError, Person]] =
    Future {
      store += (person.id -> person)
      Right(person)
    }

  override def update(person: Person): Future[Either[RepositoryError, Person]] =
    Future {
      store += (person.id -> person)
      Right(person)
    }

  override def all: Future[Either[RepositoryError, Seq[Person]]] = Future successful Right(store.values.toList)

  override def delete(personId: UUID): Future[Either[RepositoryError, UUID]] =
    Future {
      store remove personId
      Right(personId)
    }

  override def find(personId: UUID): Future[Either[RepositoryError, Option[Person]]] =
    Future {
      Right(store.get(personId))
    }
}
