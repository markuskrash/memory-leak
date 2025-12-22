package com.memoryleak.server.game

import com.memoryleak.shared.model.*
import java.util.UUID

object DeckBuilder {
    fun createDefaultDeck(): MutableList<Card> {
        val deck = mutableListOf<Card>()
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_SCOUT, 30, 20))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_TANK, 80, 40))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_RANGED, 50, 60))
        

        repeat(2) {  deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_ALLOCATOR, 40, 30)) }
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_GARBAGE_COLLECTOR, 50, 40))
        repeat(2) { deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_BASIC_PROCESS, 35, 25)) }
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_INHERITANCE_DRONE, 60, 45))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_POLYMORPH_WARRIOR, 85, 60))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_ENCAPSULATION_SHIELD, 100, 50))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_ABSTRACTION_AGENT, 45, 35))
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_REFLECTION_SPY, 30, 25))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_CODE_INJECTOR, 70, 55))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_DYNAMIC_DISPATCHER, 65, 50))
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_COROUTINE_ARCHER, 75, 65))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_PROMISE_KNIGHT, 90, 70))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_DEADLOCK_TRAP, 40, 40))
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_LAMBDA_SNIPER, 100, 80))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_RECURSIVE_BOMB, 55, 50))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_HIGHER_ORDER_COMMANDER, 80, 65))
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_API_GATEWAY, 70, 55))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_WEBSOCKET_SCOUT, 35, 30))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_RESTFUL_HEALER, 70, 60))
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_CACHE_RUNNER, 25, 20))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_INDEXER, 50, 45))
        deck.add(Card(UUID.randomUUID().toString(), CardType.SPAWN_TRANSACTION_GUARD, 75, 60))
        

        deck.add(Card(UUID.randomUUID().toString(), CardType.BUILD_FACTORY, 100, 0))
        
        deck.shuffle()
        return deck
    }
    
    fun drawCard(player: PlayerState) {
        if (player.hand.size >= 4) return
        
        if (player.deck.isEmpty()) {

            if (player.discardPile.isEmpty()) return
            player.deck.addAll(player.discardPile)
            player.discardPile.clear()
            player.deck.shuffle()
        }
        
        val card = player.deck.removeAt(0)
        player.hand.add(card)
    }
    
    fun playCard(player: PlayerState, cardId: String): Card? {
        val card = player.hand.find { it.id == cardId } ?: return null
        player.hand.remove(card)
        player.discardPile.add(card)
        return card
    }
}
