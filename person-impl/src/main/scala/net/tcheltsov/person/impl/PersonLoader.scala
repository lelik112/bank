package net.tcheltsov.person.impl

import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer}
import net.tcheltsov.person.api.PersonService
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._

class PersonLoader extends LagomApplicationLoader{
  override def load(context: LagomApplicationContext): LagomApplication =
    new PersonApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    new PersonApplication(context) with LagomDevModeComponents
  }

  override def describeService: Option[Descriptor] = Some(readDescriptor[PersonService])
}

abstract class PersonApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[PersonService](wire[PersonServiceImpl])
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = PersonSerializerRegistry
  persistentEntityRegistry.register(wire[PersonEntity])
}
