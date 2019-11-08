package net.tcheltsov.card.impl


import java.util.UUID

import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import net.tcheltsov.card.api.Card

import scala.concurrent.{ExecutionContext, Promise}

class CardEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[CardEvent] {
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[CardEvent] = {
    val createTable = () => {
      session.executeCreateTable("CREATE TABLE IF NOT EXISTS cards ( " +
       "id UUID, card TEXT, holderId TEXT, PRIMARY KEY (id))").flatMap(_ =>
        session.executeWrite("CREATE INDEX IF NOT EXISTS ON cards (holderId)"))
    }
    val writeCardPromise = Promise[PreparedStatement]
    val prepareWriteCard = (_: AggregateEventTag[CardEvent]) => {
      val f = session.prepare("INSERT INTO cards (id, card, holderId) VALUES (?, ?, ?)")
      writeCardPromise.completeWith(f)
      f.map(_ => Done)
    }
    val processCardAdded = (eventElement: EventStreamElement[CardAddedEvent]) => {
      writeCardPromise.future.map {ps =>
        val bindWriteCard = ps.bind()
        bindWriteCard.setUUID("id", UUID.fromString(eventElement.event.cardId))
        //TODO What right options are to do this?
        bindWriteCard.setString("card", Card.cardFormat.writes(eventElement.event.card).toString())
        bindWriteCard.setString("holderId", eventElement.event.holderId)
        List(bindWriteCard)
      }
    }
    readSide.builder[CardEvent]("cardsoffset")
      .setGlobalPrepare(createTable)
      .setPrepare(prepareWriteCard)
      .setEventHandler(processCardAdded)
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[CardEvent]] = Set(CardEvent.CardEventTag)
}
