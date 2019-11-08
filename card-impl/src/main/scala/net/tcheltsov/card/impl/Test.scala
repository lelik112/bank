package net.tcheltsov.card.impl

import java.time.{Year, YearMonth}

import net.tcheltsov.card.api.{AddCardRequest, Card, CardServiceSerializers, Visa}
import play.api.libs.json.Json

object Test extends App{
  val card = new Card(1234568L, "name", YearMonth.now(), Visa)
  println(card)
  println(Card.cardFormat.writes(card))
  val request = AddCardRequest("vasya", "vasya pupkin", "visa")
  println(CardServiceSerializers.addCardRequestFormat.writes(request))
}
