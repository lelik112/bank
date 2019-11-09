package net.tcheltsov.person.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import net.tcheltsov.person.api.{PersonInfoResponse, UserName}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

class PersonEntity extends PersistentEntity {
  override type Command = PersonCommand[_]
  override type Event = PersonEvent
  override type State = PersonState

  override def initialState: PersonState = EmptyPersonState

  override def behavior: Behavior = {
    case EmptyPersonState => initial
    case _ => personAdded
  }

  private val initial = Actions()
    .onReadOnlyCommand[PersonInfoCommand, PersonInfoResponse] {case (_, ctx, _) =>
      ctx.invalidCommand("Person with given Id is not persist")
    }
    .onCommand[ChangeNameCommand, String] {case (_, ctx, _) =>
      ctx.invalidCommand("Person with given Id is not persist")
      ctx.done
    }
    .onCommand[AddCardCommand, Done] {
      //TODO refuse to add?
      case (AddCardCommand(cardId), ctx, _) => {
        ctx.thenPersist(CardAddedEvent(cardId)) { _ => ctx.reply(Done) }
      }
    }
    .onCommand[AddPersonCommand, Done] {case (AddPersonCommand(name), ctx, _) =>
      ctx.thenPersist(PersonAddedEvent(entityId, name)) {_ => ctx.reply(Done)}
    }
    .onEvent {
      case (CardAddedEvent(cardId), state) => PersonState(state.name, cardId :: state.cards)
      case (PersonAddedEvent(_, name), state) => PersonState(Some(name), state.cards)
    }

  private val personAdded = Actions()
    .onReadOnlyCommand[PersonInfoCommand, PersonInfoResponse] {case (_, ctx, state) =>
      ctx.reply(PersonInfoResponse(state.name.get, state.cards))
    }
    .onCommand[ChangeNameCommand, String] {case (ChangeNameCommand(name), ctx, _) =>
      ctx.thenPersist(NameChangedEvent(name)) { _ => ctx.reply("Done") }
    }
    .onCommand[AddCardCommand, Done] {case (AddCardCommand(cardId), ctx, _) =>
      ctx.thenPersist(CardAddedEvent(cardId)) { _ => ctx.reply(Done) }
    }
    .onCommand[AddPersonCommand, Done] {case (_, ctx, _) =>
      ctx.invalidCommand("User with given login is already persist")
      ctx.done
    }
    .onEvent {
      case (NameChangedEvent(newName), state) => PersonState(Some(newName), state.cards)
      case (CardAddedEvent(cardId), state) => PersonState(state.name, cardId :: state.cards)
    }
}

case class PersonState(name: Option[String], cards: List[String])
object PersonState {
  implicit val format: Format[PersonState] = Json.format
}
object EmptyPersonState extends PersonState(None, Nil)

sealed trait PersonCommand[R] extends ReplyType[R]
case class PersonInfoCommand(personId: String) extends PersonCommand[PersonInfoResponse]
object PersonInfoCommand {
  implicit val format: Format[PersonInfoCommand] = Json.format
}
case class ChangeNameCommand(name: String) extends PersonCommand[String]
object ChangeNameCommand {
  implicit val format: Format[ChangeNameCommand] = Json.format
}
case class AddCardCommand(cardId: String) extends PersonCommand[Done]
object AddCardCommand {
  implicit val format: Format[AddCardCommand] = Json.format
}
case class AddPersonCommand(name: String) extends PersonCommand[Done]
object AddPersonCommand {
  implicit val format: Format[AddPersonCommand] = Json.format
}

sealed trait PersonEvent extends AggregateEvent[PersonEvent] {
  override def aggregateTag: AggregateEventTagger[PersonEvent] = PersonEvent.Tag
}
object PersonEvent {
  val Tag: AggregateEventTag[PersonEvent] = AggregateEventTag[PersonEvent]
}
case class NameChangedEvent(name: String) extends PersonEvent
object NameChangedEvent {
  implicit val format: Format[NameChangedEvent] = Json.format
}
case class CardAddedEvent(cardId: String) extends PersonEvent
object CardAddedEvent {
  implicit val format: Format[CardAddedEvent] = Json.format
}
case class PersonAddedEvent(personId: String, name: String) extends PersonEvent
object PersonAddedEvent {
  implicit val format: Format[PersonAddedEvent] = Json.format
}

object PersonSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[PersonState],
    JsonSerializer[PersonInfoCommand],
    JsonSerializer[ChangeNameCommand],
    JsonSerializer[AddCardCommand],
    JsonSerializer[NameChangedEvent],
    JsonSerializer[CardAddedEvent]
  )
}