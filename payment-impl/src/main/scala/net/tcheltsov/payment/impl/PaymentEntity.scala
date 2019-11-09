package net.tcheltsov.payment.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.Json

import scala.collection.immutable.Seq

class PaymentEntity extends PersistentEntity {
  override type Command = PaymentCommand
  override type Event = PaymentEvent
  override type State = PaymentState

  override def initialState: PaymentState = PaymentState("", 0.0)

  override def behavior: Behavior = {
    Actions()
      .onCommand[PaymentCommand, String] {case (PaymentCommand(cardId, amount), ctx, _) =>
        ctx.thenPersist(PaymentEvent(entityId, cardId, amount)) {_ =>
          ctx.reply(if (amount > 0) "Money was ADDED" else "Money was DEBITED")
        }
      }
      .onEvent {
        case (PaymentEvent(_, cardId, amount), _) => PaymentState(cardId, amount)
      }
  }
}
sealed trait PaymentSerializers
object PaymentSerializers {
  val serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer(Json.format[PaymentState]),
    JsonSerializer(Json.format[PaymentEvent]),
    JsonSerializer(Json.format[PaymentState])
  )
}
object PaymentSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = PaymentSerializers.serializers
}

case class PaymentCommand(cardId: String, amount: Double) extends PaymentSerializers with ReplyType[String]
case class PaymentEvent(paymentId: String, cardId: String, amount: Double) extends AggregateEvent[PaymentEvent]
  with PaymentSerializers {
  override def aggregateTag: AggregateEventTagger[PaymentEvent] = PaymentEvent.Tag
}
object PaymentEvent {
  val Tag: AggregateEventTag[PaymentEvent] = AggregateEventTag[PaymentEvent]
}
case class PaymentState (cardId: String, amount: Double)  extends PaymentSerializers

