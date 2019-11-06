package net.tcheltsov.card.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import net.tcheltsov.card.api.{AddCardResponse, Card}
import play.api.libs.json.Json

import scala.collection.immutable.Seq

class CardEntity extends PersistentEntity {
  override type Command = CardCommand
  override type Event = CardEvent
  override type State = CardState

  override def initialState: CardState = EmptyCardState

  override def behavior: Behavior = {
    case EmptyCardState => initial
    case _ => cardAdded
  }

  private val initial: Actions = Actions()
    .onReadOnlyCommand[GetCardCommand, Card] {case (GetCardCommand(_), ctx, _) =>
      ctx.invalidCommand("Card with given Id is not persist")
    }
    .onCommand[AddCardCommand, AddCardResponse]{case (AddCardCommand(card, holderId), ctx, _) =>
      ctx.thenPersist(CardAddedEvent(entityId, card, holderId)) {_ =>
        ctx.reply(AddCardResponse(entityId))
      }
    }
    .onEvent {
      case (CardAddedEvent(_, card, holderId), state) => CardState(Some(card), holderId, state.balance)
    }

  private val cardAdded: Actions = Actions()
    .onReadOnlyCommand[GetCardCommand, Card] {case (GetCardCommand(_), ctx, state) =>
      ctx.reply(state.card.get)
    }
    .onCommand[AddCardCommand, AddCardResponse] {case (_, ctx, _) =>
      ctx.invalidCommand("Card with given Id was persist")
      ctx.done
    }
}

sealed trait CardSerializers
object CardSerializers {
  val serializers = Seq(
    JsonSerializer(Json.format[CardState]),
    JsonSerializer(Json.format[AddCardCommand]),
    JsonSerializer(Json.format[CardAddedEvent]),
    JsonSerializer(Json.format[GetCardCommand])
  )
}

sealed trait CardCommand extends CardSerializers
case class AddCardCommand(card: Card, holderId: String) extends CardCommand with ReplyType[AddCardResponse]
case class GetCardCommand(id: String) extends CardCommand with ReplyType[Card]

sealed trait CardEvent extends CardSerializers
case class CardAddedEvent(cardId: String, card: Card, holderId: String) extends CardEvent

case class CardState(card: Option[Card], holderId: String, balance: Double) extends CardSerializers
object EmptyCardState extends CardState(None, "", 0.0)


object CardSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = CardSerializers.serializers
}
