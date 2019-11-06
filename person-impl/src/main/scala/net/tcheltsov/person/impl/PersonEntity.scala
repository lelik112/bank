package net.tcheltsov.person.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import net.tcheltsov.person.api.UserName
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

class PersonEntity extends PersistentEntity {
  override type Command = PersonCommand[_]
  override type Event = PersonEvent
  override type State = PersonState

  override def initialState: PersonState = PersonState(None, Nil)

  override def behavior: Behavior = {
    case PersonState(name, _) => Actions()
      .onReadOnlyCommand[HelloCommand, String] {
        case (HelloCommand(personId), ctx, state) => ctx.reply("Hello, " + name.getOrElse("Anonymous"))
      }
      .onCommand[ChangeNameCommand, String] {
        case (ChangeNameCommand(name), ctx, state) => {
          ctx.thenPersist(NameChangedEvent(name)) {_ => ctx.reply("Done")}
        }
      }
      .onEvent {
        case (NameChangedEvent(newName), state) => PersonState(Some(newName), state.cards)
      }
  }
}

case class PersonState(name: Option[String], cards: List[Long])
object PersonState {
  implicit val format: Format[PersonState] = Json.format
}

sealed trait PersonCommand[R] extends ReplyType[R]
case class HelloCommand(personId: String) extends PersonCommand[String]
object HelloCommand {
  implicit  val format: Format[HelloCommand] = Json.format
}
case class ChangeNameCommand(name: String) extends PersonCommand[String]
object ChangeNameCommand {
  implicit  val format: Format[ChangeNameCommand] = Json.format
}

sealed trait PersonEvent extends AggregateEvent[PersonEvent] {
  override def aggregateTag: AggregateEventTagger[PersonEvent] = PersonEvent.Tag
}
object PersonEvent {
  val Tag: AggregateEventTag[PersonEvent] = AggregateEventTag[PersonEvent]
}
case class NameChangedEvent(name: String) extends PersonEvent
object NameChangedEvent {
  implicit  val format: Format[NameChangedEvent] = Json.format
}

object PersonSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[PersonState],
    JsonSerializer[HelloCommand],
    JsonSerializer[ChangeNameCommand],
    JsonSerializer[NameChangedEvent],
    JsonSerializer[UserName]
  )
}