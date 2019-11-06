package net.tcheltsov.person.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import net.tcheltsov.person.api.{PersonService, UserName}

import scala.concurrent.Future


class PersonServiceImpl (persistentEntityRegistry: PersistentEntityRegistry) extends PersonService{
  override def hello(id: String): ServiceCall[NotUsed, String] = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[PersonEntity](id)
    ref.ask(HelloCommand(id))
  }

  override def changeName(id: String): ServiceCall[UserName, String] = ServiceCall {name =>
    val ref = persistentEntityRegistry.refFor[PersonEntity](id)
    ref.ask(ChangeNameCommand(name.name))
  }

}
