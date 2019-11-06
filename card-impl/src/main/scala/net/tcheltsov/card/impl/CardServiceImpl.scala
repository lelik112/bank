package net.tcheltsov.card.impl

import java.time.YearMonth

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import net.tcheltsov.card.api.{AddCardRequest, AddCardResponse, Card, CardService, MasterCard, Visa}

class CardServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends CardService {
  override def addCard(): ServiceCall[AddCardRequest, AddCardResponse] = { request =>
    def generateLong: Long = Math.abs(scala.util.Random.nextLong())
    def generateCard: Card = {
      val cardType = if (request.cardType.equalsIgnoreCase("visa")) Visa else MasterCard
      Card(generateLong, request.holderName, YearMonth.now().plusYears(2), cardType)
    }
    //TODO How to check that it is a new entity?
    val ref = persistentEntityRegistry.refFor[CardEntity](generateLong.toString)
    //TODO Get holder name from PersonService?
    ref.ask(AddCardCommand(generateCard, request.holderId))
  }

  override def getCard(id: String): ServiceCall[NotUsed, Card] = {_ =>
    val ref = persistentEntityRegistry.refFor[CardEntity](id)
    ref.ask(GetCardCommand(id))
  }
}

