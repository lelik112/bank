package net.tcheltsov.person.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

trait PersonService extends Service {

  def hello(id: String): ServiceCall[NotUsed, String]
  def changeName(id: String):ServiceCall[UserName, String]

  override final def descriptor: Descriptor = {
    import Service._
    named("person")
      .withCalls(
        pathCall("/api/person/:id", hello _),
        pathCall("/api/person/:id", changeName _)
      ).withAutoAcl(true)
  }
}

case class UserName(name: String)
object UserName {
  implicit val format: Format[UserName] = Json.format[UserName]
}
