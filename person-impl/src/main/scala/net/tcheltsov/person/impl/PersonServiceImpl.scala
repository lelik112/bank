package net.tcheltsov.person.impl

import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import net.tcheltsov.card.api.CardService
import net.tcheltsov.person.api.{Person, PersonInfoResponse, PersonService, UserName}

import scala.concurrent.ExecutionContext

class PersonServiceImpl (persistentEntityRegistry: PersistentEntityRegistry,
                         cassandraSession: CassandraSession,
                         cardService: CardService)
                        (implicit ec: ExecutionContext) extends PersonService{
  cardService
    .cardsTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction {userCard =>
        val ref = persistentEntityRegistry.refFor[PersonEntity](userCard.personId)
        ref.ask(AddCardCommand(userCard.cardId))
        Done
      }
    )

  override def getPersonInfo(id: String): ServiceCall[NotUsed, PersonInfoResponse] = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[PersonEntity](id)
    ref.ask(PersonInfoCommand(id))
  }

  override def changeName(id: String): ServiceCall[UserName, String] = ServiceCall {name =>
    val ref = persistentEntityRegistry.refFor[PersonEntity](id)
    ref.ask(ChangeNameCommand(name.name))
  }

  override def addPerson(): ServiceCall[Person, Done] = ServiceCall { request =>
    val ref = persistentEntityRegistry.refFor[PersonEntity](request.login)
    ref.ask(AddPersonCommand(request.name))
  }

  override def getPersons: ServiceCall[NotUsed, Seq[Person]] = {_ =>
    cassandraSession
      .selectAll("SELECT id, name FROM persons")
      .map(rows => rows.map(row => Person(row.getString("id"), row.getString("name"))))
  }
}
