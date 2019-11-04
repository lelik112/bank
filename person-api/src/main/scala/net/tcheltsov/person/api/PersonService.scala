package net.tcheltsov.person.api

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}

trait PersonService extends Service {

  override final def descriptor: Descriptor = {
    import Service._
    named("person")
  }
}
