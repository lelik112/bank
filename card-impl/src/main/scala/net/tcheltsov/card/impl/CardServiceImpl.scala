package net.tcheltsov.card.impl

import java.time.YearMonth
import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import net.tcheltsov.card.api._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

class CardServiceImpl(persistentEntityRegistry: PersistentEntityRegistry,
                      cassandraSession: CassandraSession)
                     (implicit ec: ExecutionContext) extends CardService {


  override def addCard(): ServiceCall[AddCardRequest, AddCardResponse] = { request =>
    def generateLong: Long = Math.abs(scala.util.Random.nextLong())
    def generateCard: Card = {
      val cardType = if (request.cardType.equalsIgnoreCase("visa")) Visa else MasterCard
      Card(generateLong, request.holderName, YearMonth.now().plusYears(2), cardType)
    }
    val ref = persistentEntityRegistry.refFor[CardEntity](UUID.randomUUID.toString)
    //TODO Get holder name from PersonService?
    ref.ask(AddCardCommand(generateCard, request.holderId))
  }

  override def getCard(id: String): ServiceCall[NotUsed, Card] = {_ =>
    val ref = persistentEntityRegistry.refFor[CardEntity](id)
    ref.ask(GetCardCommand(id))
  }

  override def getCards: ServiceCall[NotUsed, Seq[Card]] = {_ =>
    cassandraSession
      .selectAll("SELECT card FROM cards")
      //TODO What right options are to do this?
      .map(rows => rows.map(row => Card.cardFormat.reads(Json.parse(row.getString("card"))).get))
  }

  override def getUserCards(holderId: String): ServiceCall[NotUsed, Seq[Card]] = {_ =>
    cassandraSession
      .selectAll(s"SELECT card FROM cards WHERE holderId = '$holderId'")
      .map(rows => rows.map(row => Card.cardFormat.reads(Json.parse(row.getString("card"))).get))
  }

  override def cardsTopic(): Topic[PersonCard] = {
    TopicProducer.singleStreamWithOffset {fromOffset =>
      persistentEntityRegistry
        .eventStream(CardEvent.CardEventTag, fromOffset)
        .filter(ev => ev.event.isInstanceOf[CardAddedEvent])
        .map(ev => (PersonCard(ev.event.asInstanceOf[CardAddedEvent].holderId, ev.entityId), ev.offset))
    }
  }
}

