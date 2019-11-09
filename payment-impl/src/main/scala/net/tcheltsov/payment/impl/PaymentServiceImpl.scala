package net.tcheltsov.payment.impl

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import net.tcheltsov.payment.api.{Payment, PaymentService}

import scala.concurrent.ExecutionContext

class PaymentServiceImpl(persistentEntityRegistry: PersistentEntityRegistry,
                         cassandraSession: CassandraSession)
                        (implicit ec: ExecutionContext) extends PaymentService {
  override def pay(cardId: String, amount: Double): ServiceCall[NotUsed, String] = { _ =>
    val ref = persistentEntityRegistry.refFor[PaymentEntity](UUID.randomUUID.toString)
    ref.ask(PaymentCommand(cardId, amount))
  }

  override def payTopic(): Topic[Payment] = {
    TopicProducer.singleStreamWithOffset {fromOffset =>
      persistentEntityRegistry
        .eventStream(PaymentEvent.Tag, fromOffset)
        .map(ev => (Payment(ev.entityId, ev.event.cardId, ev.event.amount), ev.offset))
    }
  }
}
