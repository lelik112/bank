package net.tcheltsov.person.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

trait PersonService extends Service {

  def addPerson(): ServiceCall[Person, Done]
  def getPersonInfo(id: String): ServiceCall[NotUsed, PersonInfoResponse]
  def getPersons: ServiceCall[NotUsed, Seq[Person]]
  def changeName(id: String):ServiceCall[UserName, String]

  override final def descriptor: Descriptor = {
    import Service._
    named("person")
      .withCalls(
        pathCall("/api/person", addPerson _),
        pathCall("/api/person/:id", getPersonInfo _),
        pathCall("/api/person/:id", changeName _),
        pathCall("/api/persons", getPersons _)
      ).withAutoAcl(true)
  }
}

case class UserName(name: String)
object UserName {
  implicit val format: Format[UserName] = Json.format
}

case class PersonInfoResponse(name: String, cards: Seq[String])
object PersonInfoResponse {
  implicit val format: Format[PersonInfoResponse] = Json.format
}

case class Person(login: String, name: String)
object Person {
  implicit val format: Format[Person] = Json.format
}

