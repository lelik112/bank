package net.tcheltsov.payment.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

object PaymentService {
  val TOPIC_NAME = "paymentTopic"
}

trait PaymentService extends Service {

  def pay(cardId: String, amount: Double): ServiceCall[NotUsed, String]

  def payTopic(): Topic[Payment]

  override final def descriptor: Descriptor = {
    import Service._
    named("payment")
      .withCalls(
        //TODO CChange to POST
        pathCall("/api/payment/:cardId/:amount", pay _),
      )
      .withTopics(
        topic(PaymentService.TOPIC_NAME, payTopic _)
      )
      .withAutoAcl(true)
  }
}

case class Payment(paymentId: String, cardId: String, amount: Double)
object Payment {
  implicit val format: Format[Payment] = Json.format
}