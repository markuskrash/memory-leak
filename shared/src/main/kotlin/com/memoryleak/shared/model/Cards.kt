package com.memoryleak.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardType {

    SPAWN_SCOUT,
    SPAWN_TANK,
    SPAWN_RANGED,
    SPAWN_HEALER,
    BUILD_FACTORY,
    

    SPAWN_ALLOCATOR,
    SPAWN_GARBAGE_COLLECTOR,
    SPAWN_BASIC_PROCESS,
    

    SPAWN_INHERITANCE_DRONE,
    SPAWN_POLYMORPH_WARRIOR,
    SPAWN_ENCAPSULATION_SHIELD,
    SPAWN_ABSTRACTION_AGENT,
    

    SPAWN_REFLECTION_SPY,
    SPAWN_CODE_INJECTOR,
    SPAWN_DYNAMIC_DISPATCHER,
    

    SPAWN_COROUTINE_ARCHER,
    SPAWN_PROMISE_KNIGHT,
    SPAWN_DEADLOCK_TRAP,
    

    SPAWN_LAMBDA_SNIPER,
    SPAWN_RECURSIVE_BOMB,
    SPAWN_HIGHER_ORDER_COMMANDER,
    

    SPAWN_API_GATEWAY,
    SPAWN_WEBSOCKET_SCOUT,
    SPAWN_RESTFUL_HEALER,
    

    SPAWN_CACHE_RUNNER,
    SPAWN_INDEXER,
    SPAWN_TRANSACTION_GUARD
}

@Serializable
enum class UnitType {

    SCOUT,
    TANK,
    RANGED,
    HEALER,
    

    ALLOCATOR,
    GARBAGE_COLLECTOR,
    BASIC_PROCESS,
    

    INHERITANCE_DRONE,
    POLYMORPH_WARRIOR,
    ENCAPSULATION_SHIELD,
    ABSTRACTION_AGENT,
    

    REFLECTION_SPY,
    CODE_INJECTOR,
    DYNAMIC_DISPATCHER,
    

    COROUTINE_ARCHER,
    PROMISE_KNIGHT,
    DEADLOCK_TRAP,
    

    LAMBDA_SNIPER,
    RECURSIVE_BOMB,
    HIGHER_ORDER_COMMANDER,
    

    API_GATEWAY,
    WEBSOCKET_SCOUT,
    RESTFUL_HEALER,
    

    CACHE_RUNNER,
    INDEXER,
    TRANSACTION_GUARD
}

@Serializable
data class Card(
    val id: String,
    val type: CardType,
    val memoryCost: Int,
    val cpuCost: Int,
    var cooldownRemaining: Float = 0f
)

@Serializable
data class UnitStats(
    val type: UnitType,
    val maxHp: Int,
    val speed: Float,
    val damage: Int,
    val attackRange: Float,
    val attackSpeed: Float
)

object UnitStatsData {

    val SCOUT = UnitStats(
        type = UnitType.SCOUT,
        maxHp = 30,
        speed = 150f,
        damage = 5,
        attackRange = 50f,
        attackSpeed = 1.5f
    )
    
    val TANK = UnitStats(
        type = UnitType.TANK,
        maxHp = 150,
        speed = 50f,
        damage = 10,
        attackRange = 50f,
        attackSpeed = 0.8f
    )
    
    val RANGED = UnitStats(
        type = UnitType.RANGED,
        maxHp = 40,
        speed = 100f,
        damage = 15,
        attackRange = 120f,
        attackSpeed = 1.0f
    )
    
    val HEALER = UnitStats(
        type = UnitType.HEALER,
        maxHp = 50,
        speed = 80f,
        damage = 0,
        attackRange = 80f,
        attackSpeed = 2.0f
    )
    

    val ALLOCATOR = UnitStats(
        type = UnitType.ALLOCATOR,
        maxHp = 40,
        speed = 60f,
        damage = 2,
        attackRange = 30f,
        attackSpeed = 0.5f
    )
    
    val GARBAGE_COLLECTOR = UnitStats(
        type = UnitType.GARBAGE_COLLECTOR,
        maxHp = 60,
        speed = 70f,
        damage = 8,
        attackRange = 60f,
        attackSpeed = 1.0f
    )
    
    val BASIC_PROCESS = UnitStats(
        type = UnitType.BASIC_PROCESS,
        maxHp = 35,
        speed = 90f,
        damage = 6,
        attackRange = 45f,
        attackSpeed = 1.2f
    )
    

    val INHERITANCE_DRONE = UnitStats(
        type = UnitType.INHERITANCE_DRONE,
        maxHp = 45,
        speed = 85f,
        damage = 7,
        attackRange = 50f,
        attackSpeed = 1.0f
    )
    
    val POLYMORPH_WARRIOR = UnitStats(
        type = UnitType.POLYMORPH_WARRIOR,
        maxHp = 80,
        speed = 75f,
        damage = 12,
        attackRange = 55f,
        attackSpeed = 1.1f
    )
    
    val ENCAPSULATION_SHIELD = UnitStats(
        type = UnitType.ENCAPSULATION_SHIELD,
        maxHp = 100,
        speed = 40f,
        damage = 3,
        attackRange = 40f,
        attackSpeed = 0.5f
    )
    
    val ABSTRACTION_AGENT = UnitStats(
        type = UnitType.ABSTRACTION_AGENT,
        maxHp = 35,
        speed = 110f,
        damage = 4,
        attackRange = 50f,
        attackSpeed = 0.8f
    )
    

    val REFLECTION_SPY = UnitStats(
        type = UnitType.REFLECTION_SPY,
        maxHp = 25,
        speed = 130f,
        damage = 1,
        attackRange = 100f,
        attackSpeed = 0.3f
    )
    
    val CODE_INJECTOR = UnitStats(
        type = UnitType.CODE_INJECTOR,
        maxHp = 50,
        speed = 95f,
        damage = 10,
        attackRange = 70f,
        attackSpeed = 0.7f
    )
    
    val DYNAMIC_DISPATCHER = UnitStats(
        type = UnitType.DYNAMIC_DISPATCHER,
        maxHp = 55,
        speed = 80f,
        damage = 5,
        attackRange = 50f,
        attackSpeed = 1.5f
    )
    

    val COROUTINE_ARCHER = UnitStats(
        type = UnitType.COROUTINE_ARCHER,
        maxHp = 38,
        speed = 95f,
        damage = 18,
        attackRange = 130f,
        attackSpeed = 0.9f
    )
    
    val PROMISE_KNIGHT = UnitStats(
        type = UnitType.PROMISE_KNIGHT,
        maxHp = 90,
        speed = 65f,
        damage = 11,
        attackRange = 50f,
        attackSpeed = 1.0f
    )
    
    val DEADLOCK_TRAP = UnitStats(
        type = UnitType.DEADLOCK_TRAP,
        maxHp = 20,
        speed = 120f,
        damage = 2,
        attackRange = 60f,
        attackSpeed = 0.5f
    )
    

    val LAMBDA_SNIPER = UnitStats(
        type = UnitType.LAMBDA_SNIPER,
        maxHp = 30,
        speed = 70f,
        damage = 50,
        attackRange = 150f,
        attackSpeed = 0.2f
    )
    
    val RECURSIVE_BOMB = UnitStats(
        type = UnitType.RECURSIVE_BOMB,
        maxHp = 25,
        speed = 100f,
        damage = 8,
        attackRange = 40f,
        attackSpeed = 1.0f
    )
    
    val HIGHER_ORDER_COMMANDER = UnitStats(
        type = UnitType.HIGHER_ORDER_COMMANDER,
        maxHp = 70,
        speed = 60f,
        damage = 6,
        attackRange = 60f,
        attackSpeed = 0.8f
    )
    

    val API_GATEWAY = UnitStats(
        type = UnitType.API_GATEWAY,
        maxHp = 65,
        speed = 50f,
        damage = 4,
        attackRange = 70f,
        attackSpeed = 0.6f
    )
    
    val WEBSOCKET_SCOUT = UnitStats(
        type = UnitType.WEBSOCKET_SCOUT,
        maxHp = 28,
        speed = 140f,
        damage = 3,
        attackRange = 90f,
        attackSpeed = 0.7f
    )
    
    val RESTFUL_HEALER = UnitStats(
        type = UnitType.RESTFUL_HEALER,
        maxHp = 55,
        speed = 85f,
        damage = 0,
        attackRange = 90f,
        attackSpeed = 1.8f
    )
    

    val CACHE_RUNNER = UnitStats(
        type = UnitType.CACHE_RUNNER,
        maxHp = 20,
        speed = 180f,
        damage = 4,
        attackRange = 35f,
        attackSpeed = 1.5f
    )
    
    val INDEXER = UnitStats(
        type = UnitType.INDEXER,
        maxHp = 42,
        speed = 75f,
        damage = 5,
        attackRange = 80f,
        attackSpeed = 0.9f
    )
    
    val TRANSACTION_GUARD = UnitStats(
        type = UnitType.TRANSACTION_GUARD,
        maxHp = 75,
        speed = 55f,
        damage = 7,
        attackRange = 50f,
        attackSpeed = 0.8f
    )
    

    fun getStats(type: UnitType): UnitStats {
        return when (type) {
            UnitType.SCOUT -> SCOUT
            UnitType.TANK -> TANK
            UnitType.RANGED -> RANGED
            UnitType.HEALER -> HEALER
            UnitType.ALLOCATOR -> ALLOCATOR
            UnitType.GARBAGE_COLLECTOR -> GARBAGE_COLLECTOR
            UnitType.BASIC_PROCESS -> BASIC_PROCESS
            UnitType.INHERITANCE_DRONE -> INHERITANCE_DRONE
            UnitType.POLYMORPH_WARRIOR -> POLYMORPH_WARRIOR
            UnitType.ENCAPSULATION_SHIELD -> ENCAPSULATION_SHIELD
            UnitType.ABSTRACTION_AGENT -> ABSTRACTION_AGENT
            UnitType.REFLECTION_SPY -> REFLECTION_SPY
            UnitType.CODE_INJECTOR -> CODE_INJECTOR
            UnitType.DYNAMIC_DISPATCHER -> DYNAMIC_DISPATCHER
            UnitType.COROUTINE_ARCHER -> COROUTINE_ARCHER
            UnitType.PROMISE_KNIGHT -> PROMISE_KNIGHT
            UnitType.DEADLOCK_TRAP -> DEADLOCK_TRAP
            UnitType.LAMBDA_SNIPER -> LAMBDA_SNIPER
            UnitType.RECURSIVE_BOMB -> RECURSIVE_BOMB
            UnitType.HIGHER_ORDER_COMMANDER -> HIGHER_ORDER_COMMANDER
            UnitType.API_GATEWAY -> API_GATEWAY
            UnitType.WEBSOCKET_SCOUT -> WEBSOCKET_SCOUT
            UnitType.RESTFUL_HEALER -> RESTFUL_HEALER
            UnitType.CACHE_RUNNER -> CACHE_RUNNER
            UnitType.INDEXER -> INDEXER
            UnitType.TRANSACTION_GUARD -> TRANSACTION_GUARD
        }
    }

    fun getShortName(type: UnitType): String {
        return when (type) {
            UnitType.SCOUT -> "SCOUT"
            UnitType.TANK -> "TANK"
            UnitType.RANGED -> "RANGED"
            UnitType.HEALER -> "HEALER"
            UnitType.ALLOCATOR -> "ALLOC"
            UnitType.GARBAGE_COLLECTOR -> "GC"
            UnitType.BASIC_PROCESS -> "PROC"
            UnitType.INHERITANCE_DRONE -> "INHERIT"
            UnitType.POLYMORPH_WARRIOR -> "POLY"
            UnitType.ENCAPSULATION_SHIELD -> "SHIELD"
            UnitType.ABSTRACTION_AGENT -> "ABSTR"
            UnitType.REFLECTION_SPY -> "SPY"
            UnitType.CODE_INJECTOR -> "INJECT"
            UnitType.DYNAMIC_DISPATCHER -> "DISP"
            UnitType.COROUTINE_ARCHER -> "COROUT"
            UnitType.PROMISE_KNIGHT -> "PROM"
            UnitType.DEADLOCK_TRAP -> "TRAP"
            UnitType.LAMBDA_SNIPER -> "LAMBDA"
            UnitType.RECURSIVE_BOMB -> "REC.B"
            UnitType.HIGHER_ORDER_COMMANDER -> "H.O.C"
            UnitType.API_GATEWAY -> "API"
            UnitType.WEBSOCKET_SCOUT -> "WS"
            UnitType.RESTFUL_HEALER -> "REST"
            UnitType.CACHE_RUNNER -> "CACHE"
            UnitType.INDEXER -> "INDEX"
            UnitType.TRANSACTION_GUARD -> "TRANS"
        }
    }

    fun getDescription(type: UnitType): String {
        return when (type) {
            UnitType.SCOUT -> "Fast scout unit. Weak attack."
            UnitType.TANK -> "Heavy armor, slow movement."
            UnitType.RANGED -> "Attacks from distance."
            UnitType.HEALER -> "Heals nearby allies."
            
            UnitType.ALLOCATOR -> "Captures resources. Low combat stats."
            UnitType.GARBAGE_COLLECTOR -> "Recycles dead enemies into resources."
            UnitType.BASIC_PROCESS -> "Standard reliable infantry."
            
            UnitType.INHERITANCE_DRONE -> "Absorbs stats from dead allies."
            UnitType.POLYMORPH_WARRIOR -> "Bonus dmg vs factories/bases."
            UnitType.ENCAPSULATION_SHIELD -> "Grants shields to allies."
            UnitType.ABSTRACTION_AGENT -> "Hides allies from enemy vision."
            
            UnitType.REFLECTION_SPY -> "Reveals enemy stats."
            UnitType.CODE_INJECTOR -> "Injects DoT bugs into factories."
            UnitType.DYNAMIC_DISPATCHER -> "Aura: Boosts ally attack speed."
            
            UnitType.COROUTINE_ARCHER -> "Long range, ignores armor."
            UnitType.PROMISE_KNIGHT -> "Explodes on death (AoE)."
            UnitType.DEADLOCK_TRAP -> "Freezes clustered enemies."
            
            UnitType.LAMBDA_SNIPER -> "One-shot kill ability (long CD)."
            UnitType.RECURSIVE_BOMB -> "Splits into smaller bombs on death."
            UnitType.HIGHER_ORDER_COMMANDER -> "Buffs nearby allies damage."
            
            UnitType.API_GATEWAY -> "Extends ally attack range."
            UnitType.WEBSOCKET_SCOUT -> "Reveals all enemies on map."
            UnitType.RESTFUL_HEALER -> "Heals and cleanses debuffs."
            
            UnitType.CACHE_RUNNER -> "Super fast, captures nodes."
            UnitType.INDEXER -> "Marks enemies for bonus damage."
            UnitType.TRANSACTION_GUARD -> "Reverts enemy text on death."
        }
    }
}

