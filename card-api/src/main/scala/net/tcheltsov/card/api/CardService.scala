package net.tcheltsov.card.api

import java.time.YearMonth

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json}

object CardService {
  val TOPIC_NAME = "cardsTopic"
}
trait CardService extends Service {

  def addCard(): ServiceCall[AddCardRequest, AddCardResponse]
  def getCard(id: String): ServiceCall[NotUsed, Card]
  def getBalance(id: String):ServiceCall[NotUsed, Double]
  def getCards: ServiceCall[NotUsed, Seq[Card]]
  def getUserCards(holderId: String): ServiceCall[NotUsed, Seq[Card]]

  def cardsTopic(): Topic[PersonCard]

  override final def descriptor: Descriptor = {
    import Service._
    named("card")
      .withCalls(
        pathCall("/api/card", addCard _),
        pathCall("/api/card/:id", getCard _),
        pathCall("/api/card/:id/balance", getBalance _),
        pathCall("/api/cards", getCards _),
        pathCall("/api/:holderId/cards", getUserCards _)
      )
      .withTopics(
        topic(CardService.TOPIC_NAME, cardsTopic _)
      )
      .withAutoAcl(true)
  }
}

sealed case class CardType(`type`: String)
object Visa extends CardType("Visa")
object MasterCard extends CardType("MasterCard")

case class Card(number: Long, holder: String, expireDate: YearMonth, `type`: CardType)
object Card {
  //TODO add validation??? https://www.playframework.com/documentation/2.6.x/ScalaJsonCombinators
  implicit val cardTypeFormat: Format[CardType] = Json.format
  implicit val yearMonthFormat: Format[YearMonth] = (
    (JsPath \ "expireYear").format[Int] and
      (JsPath \ "expireMonth").format[Int]
    )(YearMonth.of, it => (it.getYear, it.getMonthValue))
  //TODO add custom format??? (ignored case)
  implicit val cardFormat: Format[Card] = Json.format
}

sealed trait CardServiceSerializers
object CardServiceSerializers {
  implicit val addCardRequestFormat: Format[AddCardRequest] = Json.format
  implicit val addCardResponseFormat: Format[AddCardResponse] = Json.format
  implicit val personCardFormat: Format[PersonCard] = Json.format
}

case class AddCardRequest(holderId: String, holderName: String, cardType: String) extends CardServiceSerializers
case class AddCardResponse(cardId: String) extends CardServiceSerializers
case class PersonCard(personId: String, cardId: String) extends CardServiceSerializers