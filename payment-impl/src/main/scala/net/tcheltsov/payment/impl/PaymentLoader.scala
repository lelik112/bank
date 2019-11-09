package net.tcheltsov.payment.impl

import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer}
import com.softwaremill.macwire.wire
import net.tcheltsov.payment.api.PaymentService
import play.api.libs.ws.ahc.AhcWSComponents

class PaymentLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new PaymentApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    new PaymentApplication(context) with LagomDevModeComponents
  }

  override def describeService: Option[Descriptor] = Some(readDescriptor[PaymentService])
}

abstract class PaymentApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[PaymentService](wire[PaymentServiceImpl])
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = PaymentSerializerRegistry
  persistentEntityRegistry.register(wire[PaymentEntity])
}
