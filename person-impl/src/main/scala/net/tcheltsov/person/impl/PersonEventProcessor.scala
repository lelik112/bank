package net.tcheltsov.person.impl

import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Promise}

class PersonEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[PersonEvent] {
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[PersonEvent] = {
    val createTable = () => {
      session.executeCreateTable("CREATE TABLE IF NOT EXISTS persons ( " +
        "id TEXT, name TEXT, PRIMARY KEY (id))").flatMap(_ =>
        session.executeWrite("CREATE INDEX IF NOT EXISTS ON persons (name)"))
    }
    val writePersonPromise = Promise[PreparedStatement]
    val prepareWritePerson = (_: AggregateEventTag[PersonEvent]) => {
      val f = session.prepare("INSERT INTO persons (id, name) VALUES (?, ?)")
      writePersonPromise.completeWith(f)
      f.map(_ => Done)
    }
    val changeNamePromise = Promise[PreparedStatement]
    val prepareChangeName = (_: AggregateEventTag[PersonEvent]) => {
      val f = session.prepare("UPDATE persons SET name = ? WHERE id = ?")
      changeNamePromise.completeWith(f)
      f.map(_ => Done)
    }
    val processPersonAdded = (eventElement: EventStreamElement[PersonAddedEvent]) => {
      writePersonPromise.future.map {ps =>
        val bindWritePerson = ps.bind()
        bindWritePerson.setString("id", eventElement.entityId)
        bindWritePerson.setString("name", eventElement.event.name)
        List(bindWritePerson)
      }
    }
    val processPersonNameChanged = (eventElement: EventStreamElement[NameChangedEvent]) => {
      changeNamePromise.future.map {ps =>
        val bindChangeName = ps.bind()
        bindChangeName.setString("name", eventElement.event.newName)
        bindChangeName.setString("id", eventElement.entityId)
        List(bindChangeName)
      }
    }
    readSide.builder[PersonEvent]("personsoffset")
      .setGlobalPrepare(createTable)
      .setPrepare(prepareWritePerson)
      .setPrepare(prepareChangeName)
      .setEventHandler(processPersonAdded)
      .setEventHandler(processPersonNameChanged)
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[PersonEvent]] = Set(PersonEvent.Tag)
}