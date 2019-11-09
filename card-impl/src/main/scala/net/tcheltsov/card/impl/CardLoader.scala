package net.tcheltsov.card.impl

import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer}
import com.softwaremill.macwire.wire
import net.tcheltsov.card.api.CardService
import net.tcheltsov.payment.api.PaymentService
import play.api.libs.ws.ahc.AhcWSComponents

class CardLoader  extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new CardApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    new CardApplication(context) with LagomDevModeComponents
  }

  override def describeService: Option[Descriptor] = Some(readDescriptor[CardService])
}

abstract class CardApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[CardService](wire[CardServiceImpl])
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = CardSerializerRegistry
  persistentEntityRegistry.register(wire[CardEntity])
  readSide.register(wire[CardEventProcessor])
  lazy val paymentService: PaymentService = serviceClient.implement[PaymentService]
}
