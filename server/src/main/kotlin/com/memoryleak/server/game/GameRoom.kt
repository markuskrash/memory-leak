package com.memoryleak.server.game

import com.memoryleak.shared.model.*
import com.memoryleak.shared.network.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

class GameRoom {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val players = ConcurrentHashMap<String, PlayerState>()
    private val entities = ConcurrentHashMap<String, GameEntity>()

    private var gameScope = CoroutineScope(Dispatchers.Default)
    private var isRunning = false
    private var lastTickTime = System.currentTimeMillis()
    fun start() {
        if (isRunning) return
        isRunning = true

        spawnResource("mem_1", ResourceType.MEMORY, 100f, 100f)
        spawnResource("mem_2", ResourceType.MEMORY, 700f, 500f)
        spawnResource("cpu_1", ResourceType.CPU, 700f, 100f)
        spawnResource("cpu_2", ResourceType.CPU, 100f, 500f)
        gameScope.launch {
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val delta = (currentTime - lastTickTime) / 1000f
                lastTickTime = currentTime

                update(delta)
                broadcastState()

                delay(1000L / 60L)
            }
        }
    }

    private fun spawnResource(id: String, type: ResourceType, x: Float, y: Float) {
        entities[id] = GameEntity(
            id = id,
            type = EntityType.RESOURCE_NODE,
            x = x,
            y = y,
            ownerId = "0",
            hp = 100,
            maxHp = 100,
            resourceType = type,
            resourceAmount = 1000
        )
    }

    suspend fun join(sessionId: String, socket: WebSocketSession): PlayerState {
        sessions[sessionId] = socket
        val player = PlayerState(
            id = sessionId,
            name = "Player-$sessionId",
            memory = 200,
            cpu = 100
        )

        player.deck = DeckBuilder.createDefaultDeck()
        repeat(4) { DeckBuilder.drawCard(player) }

        players[sessionId] = player

        val instanceId = UUID.randomUUID().toString()
        entities[instanceId] = GameEntity(
            id = instanceId,
            type = EntityType.INSTANCE,
            x = (Math.random() * 800).toFloat(),
            y = (Math.random() * 600).toFloat(),
            ownerId = sessionId,
            hp = 1000,
            maxHp = 1000,
            speed = 15f
        )

        return player
    }

    private fun spawnUnitByCard(ownerId: String, unitType: UnitType, x: Float, y: Float, stats: UnitStats): GameEntity {
        val unitId = UUID.randomUUID().toString()
        val entity = GameEntity(
            id = unitId,
            type = EntityType.UNIT,
            x = x,
            y = y,
            ownerId = ownerId,
            hp = stats.maxHp,
            maxHp = stats.maxHp,
            speed = stats.speed,
            unitType = unitType
        )
        entities[unitId] = entity
        return entity
    }

    fun handleCommand(sessionId: String, cmd: CommandPacket) {

        if (cmd.commandType == CommandType.MOVE && cmd.entityId != null) {
            val entity = entities[cmd.entityId]
            if (entity != null && entity.ownerId == sessionId) {
                entity.targetX = cmd.targetX
                entity.targetY = cmd.targetY
            }
        } else if (cmd.commandType == CommandType.BUILD) {
            if (cmd.entityId != null) {
                val source = entities[cmd.entityId]
                val player = players[sessionId]
                if (source != null && source.ownerId == sessionId && player != null) {
                    if (source.type == EntityType.INSTANCE) {
                        if (player.memory >= 100) {
                            player.memory -= 100
                            val factoryId = UUID.randomUUID().toString()
                            entities[factoryId] = GameEntity(
                                id = factoryId,
                                type = EntityType.FACTORY,
                                x = source.x + (Math.random() * 60 - 30).toFloat(),
                                y = source.y + (Math.random() * 60 - 30).toFloat(),
                                ownerId = sessionId,
                                hp = 200,
                                maxHp = 200,
                                speed = 35f
                            )
                        }
                    } else if (source.type == EntityType.FACTORY) {
                        if (player.cpu >= 50) {
                            player.cpu -= 50
                            val unitId = UUID.randomUUID().toString()
                            entities[unitId] = GameEntity(
                                id = unitId,
                                type = EntityType.UNIT,
                                x = source.x + 20f,
                                y = source.y + 20f,
                                ownerId = sessionId,
                                hp = 50,
                                maxHp = 50,
                                speed = 120f
                            )
                        }
                    }
                }
            }
        } else if (cmd.commandType == CommandType.PLAY_CARD) {
            val cardId = cmd.cardId ?: return
            val player = players[sessionId] ?: return
            val card = DeckBuilder.playCard(player, cardId) ?: return

            if (player.globalCooldown > 0) {
                return
            }
            val myBase = entities.values.find { it.ownerId == sessionId && it.type == EntityType.INSTANCE }
            if (myBase != null) {
                val dx = cmd.targetX - myBase.x
                val dy = cmd.targetY - myBase.y
                val distSq = dx * dx + dy * dy
                val maxDist = 200f

                if (distSq > maxDist * maxDist) {
                    println("Spawn too far from base!")
                    player.hand.add(card)
                    player.discardPile.remove(card)
                    return
                }
            }

            if (player.memory < card.memoryCost || player.cpu < card.cpuCost) {
                player.hand.add(card)
                player.discardPile.remove(card)
                return
            }

            player.memory -= card.memoryCost
            player.cpu -= card.cpuCost

            player.globalCooldown = 1.5f

            when (card.type) {
                CardType.SPAWN_SCOUT -> spawnUnitByCard(
                    sessionId,
                    UnitType.SCOUT,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.SCOUT
                )

                CardType.SPAWN_TANK -> spawnUnitByCard(
                    sessionId,
                    UnitType.TANK,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.TANK
                )

                CardType.SPAWN_RANGED -> spawnUnitByCard(
                    sessionId,
                    UnitType.RANGED,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.RANGED
                )

                CardType.SPAWN_HEALER -> spawnUnitByCard(
                    sessionId,
                    UnitType.HEALER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.HEALER
                )

                CardType.SPAWN_ALLOCATOR -> spawnUnitByCard(
                    sessionId,
                    UnitType.ALLOCATOR,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.ALLOCATOR
                )

                CardType.SPAWN_GARBAGE_COLLECTOR -> spawnUnitByCard(
                    sessionId,
                    UnitType.GARBAGE_COLLECTOR,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.GARBAGE_COLLECTOR
                )

                CardType.SPAWN_BASIC_PROCESS -> spawnUnitByCard(
                    sessionId,
                    UnitType.BASIC_PROCESS,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.BASIC_PROCESS
                )

                CardType.SPAWN_INHERITANCE_DRONE -> spawnUnitByCard(
                    sessionId,
                    UnitType.INHERITANCE_DRONE,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.INHERITANCE_DRONE
                )

                CardType.SPAWN_POLYMORPH_WARRIOR -> spawnUnitByCard(
                    sessionId,
                    UnitType.POLYMORPH_WARRIOR,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.POLYMORPH_WARRIOR
                )

                CardType.SPAWN_ENCAPSULATION_SHIELD -> spawnUnitByCard(
                    sessionId,
                    UnitType.ENCAPSULATION_SHIELD,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.ENCAPSULATION_SHIELD
                )

                CardType.SPAWN_ABSTRACTION_AGENT -> spawnUnitByCard(
                    sessionId,
                    UnitType.ABSTRACTION_AGENT,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.ABSTRACTION_AGENT
                )

                CardType.SPAWN_REFLECTION_SPY -> spawnUnitByCard(
                    sessionId,
                    UnitType.REFLECTION_SPY,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.REFLECTION_SPY
                )

                CardType.SPAWN_CODE_INJECTOR -> spawnUnitByCard(
                    sessionId,
                    UnitType.CODE_INJECTOR,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.CODE_INJECTOR
                )

                CardType.SPAWN_DYNAMIC_DISPATCHER -> spawnUnitByCard(
                    sessionId,
                    UnitType.DYNAMIC_DISPATCHER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.DYNAMIC_DISPATCHER
                )

                CardType.SPAWN_COROUTINE_ARCHER -> spawnUnitByCard(
                    sessionId,
                    UnitType.COROUTINE_ARCHER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.COROUTINE_ARCHER
                )

                CardType.SPAWN_PROMISE_KNIGHT -> spawnUnitByCard(
                    sessionId,
                    UnitType.PROMISE_KNIGHT,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.PROMISE_KNIGHT
                )

                CardType.SPAWN_DEADLOCK_TRAP -> spawnUnitByCard(
                    sessionId,
                    UnitType.DEADLOCK_TRAP,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.DEADLOCK_TRAP
                )

                CardType.SPAWN_LAMBDA_SNIPER -> spawnUnitByCard(
                    sessionId,
                    UnitType.LAMBDA_SNIPER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.LAMBDA_SNIPER
                )

                CardType.SPAWN_RECURSIVE_BOMB -> spawnUnitByCard(
                    sessionId,
                    UnitType.RECURSIVE_BOMB,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.RECURSIVE_BOMB
                )

                CardType.SPAWN_HIGHER_ORDER_COMMANDER -> spawnUnitByCard(
                    sessionId,
                    UnitType.HIGHER_ORDER_COMMANDER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.HIGHER_ORDER_COMMANDER
                )

                CardType.SPAWN_API_GATEWAY -> spawnUnitByCard(
                    sessionId,
                    UnitType.API_GATEWAY,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.API_GATEWAY
                )

                CardType.SPAWN_WEBSOCKET_SCOUT -> spawnUnitByCard(
                    sessionId,
                    UnitType.WEBSOCKET_SCOUT,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.WEBSOCKET_SCOUT
                )

                CardType.SPAWN_RESTFUL_HEALER -> spawnUnitByCard(
                    sessionId,
                    UnitType.RESTFUL_HEALER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.RESTFUL_HEALER
                )

                CardType.SPAWN_CACHE_RUNNER -> spawnUnitByCard(
                    sessionId,
                    UnitType.CACHE_RUNNER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.CACHE_RUNNER
                )

                CardType.SPAWN_INDEXER -> spawnUnitByCard(
                    sessionId,
                    UnitType.INDEXER,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.INDEXER
                )

                CardType.SPAWN_TRANSACTION_GUARD -> spawnUnitByCard(
                    sessionId,
                    UnitType.TRANSACTION_GUARD,
                    cmd.targetX,
                    cmd.targetY,
                    UnitStatsData.TRANSACTION_GUARD
                )

                CardType.BUILD_FACTORY -> {
                    val factoryId = UUID.randomUUID().toString()
                    entities[factoryId] = GameEntity(
                        id = factoryId,
                        type = EntityType.FACTORY,
                        x = cmd.targetX,
                        y = cmd.targetY,
                        ownerId = sessionId,
                        hp = 200,
                        maxHp = 200,
                        speed = 35f
                    )
                }
            }

            DeckBuilder.drawCard(player)
        }
    }

    private var lastTick = 0L
    private val deadUnitsThisFrame = mutableListOf<GameEntity>()

    private suspend fun update(delta: Float) {
        deadUnitsThisFrame.clear()

        players.values.forEach { player ->
            if (player.globalCooldown > 0) {
                player.globalCooldown -= delta
                if (player.globalCooldown < 0) player.globalCooldown = 0f
            }
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTick > 1000) {
            lastTick = currentTime

            players.values.forEach { player ->
                player.memory += 5
                player.cpu += 5
            }

            entities.values.filter { it.type == EntityType.RESOURCE_NODE && it.ownerId != "0" }.forEach { node ->
                val owner = players[node.ownerId]
                if (owner != null) {
                    when (node.resourceType) {
                        ResourceType.MEMORY -> owner.memory += 1
                        ResourceType.CPU -> owner.cpu += 1
                        null -> {}
                    }
                }
            }

            entities.values.filter { it.type == EntityType.UNIT && it.unitType == UnitType.ALLOCATOR }
                .forEach { allocator ->
                    val owner = players[allocator.ownerId]
                    if (owner != null) {
                        owner.memory += 2
                    }
                }
        }

        entities.values.filter { it.type == EntityType.UNIT }.forEach { unit ->
            updateUnitAI(unit, delta, currentTime)
        }

        entities.values.filter {
            it.type == EntityType.FACTORY || it.type == EntityType.INSTANCE
        }.forEach { entity ->
            val tx = entity.targetX
            val ty = entity.targetY
            if (tx != null && ty != null) {
                val dx = tx - entity.x
                val dy = ty - entity.y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist > 2f) {
                    val moveAmount = entity.speed * delta
                    if (moveAmount >= dist) {
                        entity.x = tx
                        entity.y = ty
                        entity.targetX = null
                        entity.targetY = null
                    } else {
                        entity.x += (dx / dist) * moveAmount
                        entity.y += (dy / dist) * moveAmount
                    }
                } else {
                    entity.x = tx
                    entity.y = ty
                    entity.targetX = null
                    entity.targetY = null
                }
            }
        }

        entities.values.filter { it.type == EntityType.RESOURCE_NODE }.forEach { node ->
            val nearbyUnit = entities.values.find {
                it.type == EntityType.UNIT && it.ownerId != "0" && distance(it, node) < 30f
            }
            if (nearbyUnit != null && node.ownerId != nearbyUnit.ownerId) {
                node.ownerId = nearbyUnit.ownerId
            }
        }
    }

    private fun selectTarget(unit: GameEntity): GameEntity? {
        val isCollector = unit.unitType == UnitType.ALLOCATOR || unit.unitType == UnitType.CACHE_RUNNER

        val nearestNode = findNearestResourceNode(unit)
        val nearestEnemyId = findNearestEnemy(unit)
        val nearestEnemy = nearestEnemyId?.let { entities[it] }

        val nodeDist = nearestNode?.let { distance(unit, it) } ?: Float.MAX_VALUE
        val enemyDist = nearestEnemy?.let { distance(unit, it) } ?: Float.MAX_VALUE

        return when {
            isCollector && nearestNode != null -> nearestNode
            nodeDist < enemyDist -> nearestNode
            else -> nearestEnemy
        }
    }


    private suspend fun updateUnitAI(unit: GameEntity, delta: Float, currentTime: Long) {
        val stats = unit.unitType?.let { UnitStatsData.getStats(it) } ?: return

        val isCollector =
            unit.unitType == UnitType.ALLOCATOR ||
                    unit.unitType == UnitType.CACHE_RUNNER

        if (unit.aiState != AIState.ATTACKING) {

            val nearestNode = findNearestResourceNode(unit)
            val nearestEnemyId = findNearestEnemy(unit)
            val nearestEnemy = nearestEnemyId?.let { entities[it] }

            val nodeDist = nearestNode?.let { distance(unit, it) } ?: Float.MAX_VALUE
            val enemyDist = nearestEnemy?.let { distance(unit, it) } ?: Float.MAX_VALUE

            val newTarget = when {
                isCollector && nearestNode != null -> nearestNode

                nodeDist < enemyDist -> nearestNode
                else -> nearestEnemy
            }

            if (newTarget != null) {
                unit.targetEnemyId = newTarget.id
                unit.aiState = AIState.MOVING_TO_TARGET
            } else {
                unit.targetEnemyId = null
                unit.aiState = AIState.IDLE
            }
        }

        val target = unit.targetEnemyId?.let { entities[it] }
        if (target == null) {
            unit.aiState = AIState.IDLE
            unit.attackingTargetId = null
            return
        }

        val dist = distance(unit, target)

        if ((target.type == EntityType.UNIT || target.type == EntityType.INSTANCE) && dist <= stats.attackRange) {
            unit.aiState = AIState.ATTACKING
            unit.targetX = null
            unit.targetY = null

            val attackCooldown = (1000f / stats.attackSpeed).toLong()
            if (currentTime - unit.lastAttackTime >= attackCooldown) {
                performAttack(unit, target, stats, currentTime)
                unit.lastAttackTime = currentTime
            }

            unit.attackingTargetId = target.id
            return
        }

        unit.aiState = AIState.MOVING_TO_TARGET
        unit.attackingTargetId = null
        unit.targetX = target.x
        unit.targetY = target.y

        val dx = target.x - unit.x
        val dy = target.y - unit.y
        val distNorm = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (distNorm > 0f) {
            val moveAmount = unit.speed * delta
            unit.x += (dx / distNorm) * moveAmount
            unit.y += (dy / distNorm) * moveAmount
        }

    }

    private fun findNearestResourceNode(unit: GameEntity): GameEntity? {
        return entities.values
            .filter { it.type == EntityType.RESOURCE_NODE && it.ownerId != unit.ownerId }
            .minByOrNull { distance(unit, it) }
    }

    private fun findNearestEnemy(unit: GameEntity): String? {
        val enemies = entities.values.filter {
            it.ownerId != unit.ownerId && it.ownerId != "0" && it.type != EntityType.RESOURCE_NODE
        }

        if (enemies.isEmpty()) return null

        val prioritized = enemies.sortedWith(
            compareBy(
            {
                when (it.type) {
                    EntityType.INSTANCE -> 3
                    EntityType.FACTORY -> 2
                    EntityType.UNIT -> 1
                    else -> 4
                }
            },
            { distance(unit, it) }
        ))

        return prioritized.firstOrNull()?.id
    }

    private suspend fun performAttack(attacker: GameEntity, target: GameEntity, stats: UnitStats, currentTime: Long) {
        var damage = stats.damage

        if (attacker.unitType == UnitType.POLYMORPH_WARRIOR) {
            damage = when (target.type) {
                EntityType.UNIT -> (stats.damage * 1.3f).toInt()
                EntityType.FACTORY -> (stats.damage * 1.5f).toInt()
                EntityType.INSTANCE -> (stats.damage * 2.0f).toInt()
                else -> stats.damage
            }
        }

        if (target.abilityData.contains("indexed_by")) {
            damage = (damage * 1.25f).toInt()
        }

        if (attacker.unitType == UnitType.COROUTINE_ARCHER) {
            damage = (damage * 1.3f).toInt()
        }

        target.hp -= damage

        if (target.hp <= 0) {
            if (target.unitType == UnitType.PROMISE_KNIGHT) {
                entities.values.filter {
                    it.ownerId != target.ownerId && it.type == EntityType.UNIT && distance(it, target) < 80f
                }.forEach {
                    it.hp -= 15
                }
            }

            if (target.unitType == UnitType.RECURSIVE_BOMB) {
                val bombLevel = target.abilityData.toIntOrNull() ?: 0
                if (bombLevel < 2) {
                    repeat(2) {
                        spawnUnitByCard(
                            target.ownerId,
                            UnitType.RECURSIVE_BOMB,
                            target.x + (Math.random() * 40 - 20).toFloat(),
                            target.y + (Math.random() * 40 - 20).toFloat(),
                            UnitStatsData.RECURSIVE_BOMB.copy(
                                maxHp = UnitStatsData.RECURSIVE_BOMB.maxHp / 2,
                                damage = UnitStatsData.RECURSIVE_BOMB.damage / 2
                            )
                        ).also { newBomb ->
                            newBomb.abilityData = (bombLevel + 1).toString()
                        }
                    }
                }
            }

            if (target.unitType == UnitType.TRANSACTION_GUARD) {
                entities.values.filter {
                    it.type == EntityType.RESOURCE_NODE &&
                            it.ownerId == target.ownerId &&
                            distance(it, target) < 100f
                }.forEach {
                    it.ownerId = "0"
                }
            }

            deadUnitsThisFrame.add(target)
            entities.remove(target.id)

            if (attacker.unitType == UnitType.GARBAGE_COLLECTOR) {
                val owner = players[attacker.ownerId]
                if (owner != null) {
                    owner.memory += 3
                    owner.cpu += 2
                }
            }

            if (target.type == EntityType.INSTANCE) {
                val remainingInstances = entities.values.filter { it.type == EntityType.INSTANCE }
                if (remainingInstances.size == 1) {
                    val winnerId = remainingInstances.first().ownerId
                    broadcastGameOver(winnerId)
                }
            }
        }
    }

    private fun triggerUnitAbility(unit: GameEntity, target: GameEntity?, stats: UnitStats, currentTime: Long) {
        val abilityCooldown = 3000L

        if (currentTime - unit.lastAbilityTime < abilityCooldown) return

        when (unit.unitType) {
            UnitType.INHERITANCE_DRONE -> {
                deadUnitsThisFrame.filter {
                    it.ownerId == unit.ownerId && distance(unit, it) < 70f
                }.forEach { deadAlly ->
                    val inheritedHp = (deadAlly.maxHp * 0.2f).toInt()
                    unit.hp = (unit.hp + inheritedHp).coerceAtMost(unit.maxHp + inheritedHp)
                    unit.speed += deadAlly.speed * 0.1f
                    unit.lastAbilityTime = currentTime
                }
            }

            UnitType.ENCAPSULATION_SHIELD -> {
                entities.values.filter {
                    it.ownerId == unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            it.id != unit.id &&
                            distance(unit, it) < 80f
                }.forEach {
                    it.abilityData = "shielded_until_${currentTime + 2000}"
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.ABSTRACTION_AGENT -> {
                entities.values.filter {
                    it.ownerId == unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            distance(unit, it) < 90f
                }.forEach {
                    it.abilityData = "hidden_until_${currentTime + 3000}"
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.REFLECTION_SPY -> {
                target?.let {
                    it.abilityData = "scanned_by_${unit.id}"
                    unit.lastAbilityTime = currentTime
                }
            }

            UnitType.CODE_INJECTOR -> {
                val nearbyFactory = entities.values.find {
                    it.ownerId != unit.ownerId &&
                            it.type == EntityType.FACTORY &&
                            distance(unit, it) < 100f
                }
                nearbyFactory?.let {
                    it.hp -= 20
                    it.abilityData = "infected_until_${currentTime + 5000}"
                    unit.lastAbilityTime = currentTime
                }
            }

            UnitType.DYNAMIC_DISPATCHER -> {
                entities.values.filter {
                    it.ownerId == unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            it.id != unit.id &&
                            distance(unit, it) < 100f
                }.forEach {
                    it.abilityData = "boosted_until_${currentTime + 2000}"
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.DEADLOCK_TRAP -> {
                val nearbyEnemies = entities.values.filter {
                    it.ownerId != unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            distance(unit, it) < 70f
                }
                if (nearbyEnemies.size >= 2) {
                    nearbyEnemies.forEach {
                        it.speed = 0f
                        it.abilityData = "deadlocked_until_${currentTime + 3000}"
                    }
                    unit.lastAbilityTime = currentTime
                }
            }

            UnitType.LAMBDA_SNIPER -> {
                target?.let {
                    it.hp -= 50
                    unit.lastAbilityTime = currentTime + 5000
                }
            }

            UnitType.HIGHER_ORDER_COMMANDER -> {
                entities.values.filter {
                    it.ownerId == unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            distance(unit, it) < 120f
                }.forEach {
                    it.abilityData = "commanded_until_${currentTime + 3000}"
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.API_GATEWAY -> {
                entities.values.filter {
                    it.ownerId == unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            distance(unit, it) < 100f
                }.forEach {
                    it.abilityData = "range_boosted_until_${currentTime + 4000}"
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.WEBSOCKET_SCOUT -> {
                entities.values.filter {
                    it.ownerId != unit.ownerId && it.type == EntityType.UNIT
                }.forEach {
                    it.abilityData = "revealed_until_${currentTime + 5000}"
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.RESTFUL_HEALER -> {
                val nearbyAllies = entities.values.filter {
                    it.ownerId == unit.ownerId &&
                            it.type == EntityType.UNIT &&
                            it.id != unit.id &&
                            distance(unit, it) < 90f
                }
                nearbyAllies.forEach { ally ->
                    ally.hp = (ally.hp + 15).coerceAtMost(ally.maxHp)
                    if (ally.abilityData.contains("deadlocked") || ally.abilityData.contains("infected")) {
                        ally.abilityData = ""
                        ally.speed = ally.unitType?.let { UnitStatsData.getStats(it).speed } ?: 100f
                    }
                }
                unit.lastAbilityTime = currentTime
            }

            UnitType.INDEXER -> {
                target?.let {
                    it.abilityData = "indexed_by_${unit.id}"
                    unit.lastAbilityTime = currentTime
                }
            }

            else -> {}
        }
    }

    private suspend fun broadcastGameOver(winnerId: String) {
        val packet = GameOverPacket(winnerId)
        val json = Json.encodeToString<Packet>(packet)
        players.keys.forEach { sessionId ->
            sessions[sessionId]?.send(Frame.Text(json))
        }
    }

    private fun distance(a: GameEntity, b: GameEntity): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private suspend fun broadcastState() {
        try {
            val packet = StateUpdatePacket(
                entities = entities.values.toList(),
                players = players.values.toList(),
                serverTime = System.currentTimeMillis()
            )
            val json = Json.encodeToString<Packet>(packet)
            sessions.values.forEach { session ->
                try {
                    session.send(Frame.Text(json))
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            println("Broadcast error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun removePlayer(sessionId: String) {
        players.remove(sessionId)
        entities.entries.removeIf { it.value.ownerId == sessionId }
    }
}