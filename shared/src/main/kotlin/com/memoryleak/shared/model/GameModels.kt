package com.memoryleak.shared.model

import kotlinx.serialization.Serializable

enum class EntityType {
    INSTANCE, FACTORY, UNIT, RESOURCE_NODE
}

enum class AIState {
    IDLE,
    MOVING_TO_TARGET,
    ATTACKING,
    USING_ABILITY,
    RETREATING
}

enum class ResourceType {
    MEMORY, CPU
}

@Serializable
data class GameEntity(
    val id: String,
    val type: EntityType,
    var x: Float,
    var y: Float,
    var ownerId: String,
    var hp: Int,
    val maxHp: Int,

    var resourceType: ResourceType? = null,
    var resourceAmount: Int = 0,
    var attackingTargetId: String? = null,

    var targetX: Float? = null,
    var targetY: Float? = null,
    var speed: Float = 100f,
    var unitType: UnitType? = null,
    var lastAttackTime: Long = 0L,

    var aiState: AIState = AIState.IDLE,
    var targetEnemyId: String? = null,
    var lastAbilityTime: Long = 0L,
    var abilityData: String = "",
    var inheritedStats: String = ""
)

@Serializable
data class PlayerState(
    val id: String,
    val name: String,
    var memory: Int = 0,
    var cpu: Int = 0,
    var deck: MutableList<Card> = mutableListOf(),
    var hand: MutableList<Card> = mutableListOf(),
    var discardPile: MutableList<Card> = mutableListOf(),
    var globalCooldown: Float = 0f
)

@Serializable
data class GameState(
    val entities: MutableMap<String, GameEntity>,
    val players: MutableMap<String, PlayerState>,
    var serverTime: Long = 0
)
