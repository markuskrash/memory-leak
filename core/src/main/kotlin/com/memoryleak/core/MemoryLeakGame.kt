package com.memoryleak.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.memoryleak.shared.model.EntityType
import com.memoryleak.shared.network.CommandPacket
import com.memoryleak.shared.network.CommandType
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.memoryleak.shared.model.ResourceType
import com.memoryleak.shared.model.CardType
import com.memoryleak.shared.model.UnitStatsData
import com.memoryleak.shared.model.UnitType
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport

import com.badlogic.gdx.utils.viewport.ExtendViewport

class MemoryLeakGame : ApplicationAdapter() {
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var network: NetworkClient

    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var labelFont: BitmapFont
    

    private var selectedEntityId: String? = null
    private var selectedCardId: String? = null
    private var isPlacingCard: Boolean = false
    
    private val uiMatrix = com.badlogic.gdx.math.Matrix4()
    private lateinit var uiViewport: Viewport
    private lateinit var worldViewport: Viewport


    enum class GameState { LOGIN, PLAYING }
    private var state = GameState.LOGIN
    private lateinit var loginScreen: com.memoryleak.core.screens.LoginScreen

    override fun create() {
        camera = OrthographicCamera(800f, 600f)

        uiViewport = ExtendViewport(800f, 600f)
        worldViewport = ExtendViewport(800f, 600f, camera)
        
        shapeRenderer = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()
        font.region.texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        
        labelFont = BitmapFont()
        labelFont.region.texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        
        font.color = Color.WHITE
        labelFont.data.setScale(0.7f)
        


        camera.position.set(400f, 300f, 0f)
        camera.update()

        network = NetworkClient()

        
        loginScreen = com.memoryleak.core.screens.LoginScreen(batch, network) { token ->
            startGame(token)
        }
        

        Gdx.input.inputProcessor = loginScreen.stage
    }
    
    private fun startGame(token: String) {
        state = GameState.PLAYING
        network.connect(token)
        
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {

                val uiTouchPos = uiViewport.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
                val uiX = uiTouchPos.x
                val uiY = uiTouchPos.y
                

                val myPlayer = network.players[network.myId]
                if (myPlayer != null && myPlayer.hand.isNotEmpty()) {

                     val cardWidth = 150f
                    val cardHeight = 80f
                    val cardGap = 10f
                    val cardsStartX = (uiViewport.worldWidth - (cardWidth + cardGap) * myPlayer.hand.size) / 2f
                    val cardsY = 10f
                     
                     myPlayer.hand.forEachIndexed { index, card ->
                        val cardX = cardsStartX + index * (cardWidth + cardGap)
                        if (uiX >= cardX && uiX <= cardX + cardWidth && uiY >= cardsY && uiY <= cardsY + cardHeight) {

                            val canAfford = myPlayer.memory >= card.memoryCost && myPlayer.cpu >= card.cpuCost
                            if (canAfford) {
                                selectedCardId = card.id
                                isPlacingCard = true
                                selectedEntityId = null
                                println("Selected card: ${card.type}")
                                return true
                            } else {
                                println("Can't afford this card!")
                                return true
                            }
                        }
                     }
                }



                val worldTouchPos = worldViewport.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
                

                if (isPlacingCard && selectedCardId != null) {
                    network.sendCommand(CommandPacket(
                        commandType = CommandType.PLAY_CARD,
                        cardId = selectedCardId,
                        targetX = worldTouchPos.x,
                        targetY = worldTouchPos.y
                    ))
                    println("Placing card at ${worldTouchPos.x}, ${worldTouchPos.y}")
                    selectedCardId = null
                    isPlacingCard = false
                    return true
                }
                

                val clickedEntity = network.entities.values.find { entity ->
                    val dx = entity.x - worldTouchPos.x
                    val dy = entity.y - worldTouchPos.y
                    (dx*dx + dy*dy) < 20*20
                }

                if (clickedEntity != null) {
                    if (clickedEntity.ownerId != "0") {
                        selectedEntityId = clickedEntity.id
                        selectedCardId = null
                        isPlacingCard = false
                        println("Selected: ${clickedEntity.id}")
                    }
                } else if (selectedEntityId != null) {

                    println("Move to ${worldTouchPos.x}, ${worldTouchPos.y}")
                    network.sendCommand(CommandPacket(
                        commandType = CommandType.MOVE,
                        entityId = selectedEntityId,
                        targetX = worldTouchPos.x,
                        targetY = worldTouchPos.y
                    ))
                }
                
                return true
            }

            override fun keyUp(keycode: Int): Boolean {
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {

                    selectedCardId = null
                    isPlacingCard = false
                }
                return true
            }

        }
    }

    override fun render() {

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        if (state == GameState.LOGIN) {
            loginScreen.render(Gdx.graphics.deltaTime)
            return
        }
        

        val delta = Gdx.graphics.deltaTime
        val speed = 300f * delta
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) camera.translate(0f, speed)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) camera.translate(0f, -speed)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) camera.translate(-speed, 0f)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) camera.translate(speed, 0f)
        camera.update()
        


        worldViewport.apply()
        shapeRenderer.projectionMatrix = camera.combined
        

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color(1f, 1f, 1f, 0.1f)
        for(i in 0..800 step 50) {
            shapeRenderer.line(i.toFloat(), 0f, i.toFloat(), 600f)
        }
        for(i in 0..600 step 50) {
            shapeRenderer.line(0f, i.toFloat(), 800f, i.toFloat())
        }
        shapeRenderer.end()


        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        network.entities.values.forEach { entity ->

            when(entity.type) {
                EntityType.INSTANCE -> {
                    shapeRenderer.color = Color(0.2f, 0.8f, 1f, 1f)
                    shapeRenderer.rect(entity.x - 20, entity.y - 20, 40f, 40f)

                    shapeRenderer.color = Color(0f, 0.4f, 0.8f, 1f)
                    shapeRenderer.rect(entity.x - 10, entity.y - 10, 20f, 20f)
                }
                EntityType.RESOURCE_NODE -> {

                    if (entity.ownerId == "0") {
                        shapeRenderer.color = Color(1f, 0.8f, 0f, 1f)
                    } else if (entity.ownerId == network.myId) {
                        shapeRenderer.color = Color(0.2f, 1f, 0.2f, 1f)
                    } else {
                        shapeRenderer.color = Color(1f, 0.2f, 0.2f, 1f)
                    }
                    

                    shapeRenderer.triangle(
                        entity.x, entity.y + 15,
                        entity.x - 15, entity.y,
                        entity.x + 15, entity.y
                    )
                    shapeRenderer.triangle(
                        entity.x, entity.y - 15,
                        entity.x - 15, entity.y,
                        entity.x + 15, entity.y
                    )
                }
                EntityType.FACTORY -> {
                    shapeRenderer.color = Color(0.8f, 0.2f, 1f, 1f)

                    shapeRenderer.triangle(
                        entity.x, entity.y + 20,
                        entity.x - 20, entity.y - 15,
                        entity.x + 20, entity.y - 15
                    )
                }
                EntityType.UNIT -> {

                    val unitType = entity.unitType
                    when {

                        unitType == UnitType.SCOUT -> {
                            shapeRenderer.color = Color(0.5f, 1f, 0.5f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 8f)
                        }
                        unitType == UnitType.TANK -> {
                            shapeRenderer.color = Color(0.3f, 0.6f, 0.3f, 1f)
                            shapeRenderer.rect(entity.x - 12, entity.y - 12, 24f, 24f)
                        }
                        unitType == UnitType.RANGED -> {
                            shapeRenderer.color = Color(0.6f, 0.8f, 1f, 1f)
                            shapeRenderer.triangle(
                                entity.x, entity.y + 10,
                                entity.x - 10, entity.y - 10,
                                entity.x + 10, entity.y - 10
                            )
                        }
                        unitType == UnitType.HEALER || unitType == UnitType.RESTFUL_HEALER -> {
                            shapeRenderer.color = Color(1f, 0.8f, 1f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 10f)
                            shapeRenderer.color = Color(0.8f, 0f, 0.8f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 3f)
                        }
                        

                        unitType == UnitType.ALLOCATOR || unitType == UnitType.GARBAGE_COLLECTOR || unitType == UnitType.BASIC_PROCESS -> {
                            shapeRenderer.color = Color(0.6f, 0.6f, 0.6f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 9f)
                            if (unitType == UnitType.ALLOCATOR) {
                                shapeRenderer.color = Color(1f, 0.9f, 0f, 1f)
                                shapeRenderer.circle(entity.x, entity.y, 4f)
                            }
                        }
                        

                        unitType == UnitType.INHERITANCE_DRONE -> {
                            shapeRenderer.color = Color(1f, 0.5f, 0f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 11f)
                            shapeRenderer.color = Color(1f, 0.7f, 0.2f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 7f)
                            shapeRenderer.color = Color(1f, 0.9f, 0.4f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 3f)
                        }
                        unitType == UnitType.POLYMORPH_WARRIOR -> {
                            shapeRenderer.color = Color(0.8f, 0.2f, 1f, 1f)
                            shapeRenderer.rect(entity.x - 10, entity.y - 10, 20f, 20f)
                        }
                        unitType == UnitType.ENCAPSULATION_SHIELD -> {
                            shapeRenderer.color = Color(0.2f, 0.4f, 1f, 1f)
                            shapeRenderer.rect(entity.x - 12, entity.y - 12, 24f, 24f)

                            shapeRenderer.color = Color(0.4f, 0.6f, 1f, 1f)
                            shapeRenderer.rect(entity.x - 6, entity.y - 6, 12f, 12f)
                        }
                        unitType == UnitType.ABSTRACTION_AGENT -> {
                            shapeRenderer.color = Color(0.5f, 0.5f, 1f, 0.5f)
                            shapeRenderer.circle(entity.x, entity.y, 10f)
                        }
                        

                        unitType == UnitType.REFLECTION_SPY -> {
                            shapeRenderer.color = Color(0f, 1f, 1f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 8f)

                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            shapeRenderer.color = Color(0f, 1f, 1f, 0.6f)
                            shapeRenderer.line(entity.x - 15, entity.y, entity.x + 15, entity.y)
                            shapeRenderer.line(entity.x, entity.y - 15, entity.x, entity.y + 15)
                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                        }
                        unitType == UnitType.CODE_INJECTOR -> {
                            shapeRenderer.color = Color(1f, 0f, 1f, 1f)
                            shapeRenderer.triangle(
                                entity.x, entity.y + 11,
                                entity.x - 11, entity.y - 6,
                                entity.x + 11, entity.y - 6
                            )
                        }
                        unitType == UnitType.DYNAMIC_DISPATCHER -> {
                            shapeRenderer.color = Color(0f, 0.8f, 0.8f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 11f)

                            shapeRenderer.color = Color(0f, 0.8f, 0.8f, 0.4f)
                            shapeRenderer.circle(entity.x, entity.y, 16f)
                        }
                        

                        unitType == UnitType.COROUTINE_ARCHER -> {
                            shapeRenderer.color = Color(1f, 0.8f, 0f, 1f)
                            shapeRenderer.triangle(
                                entity.x, entity.y + 10,
                                entity.x - 8, entity.y - 8,
                                entity.x + 8, entity.y - 8
                            )
                        }
                        unitType == UnitType.PROMISE_KNIGHT -> {
                            shapeRenderer.color = Color(1f, 0.6f, 0f, 1f)
                            shapeRenderer.rect(entity.x - 11, entity.y - 11, 22f, 22f)
                        }
                        unitType == UnitType.DEADLOCK_TRAP -> {
                            shapeRenderer.color = Color(1f, 0f, 0f, 0.8f)
                            shapeRenderer.circle(entity.x, entity.y, 7f)

                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            shapeRenderer.line(entity.x - 10, entity.y - 10, entity.x + 10, entity.y + 10)
                            shapeRenderer.line(entity.x - 10, entity.y + 10, entity.x + 10, entity.y - 10)
                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                        }
                        

                        unitType == UnitType.LAMBDA_SNIPER -> {
                            shapeRenderer.color = Color(1f, 1f, 0f, 1f)

                            shapeRenderer.triangle(
                                entity.x - 8, entity.y + 12,
                                entity.x - 8, entity.y - 12,
                                entity.x + 10, entity.y
                            )
                        }
                        unitType == UnitType.RECURSIVE_BOMB -> {
                            shapeRenderer.color = Color(1f, 0.2f, 0f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 9f)
                            shapeRenderer.color = Color(0.8f, 0f, 0f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 5f)
                        }
                        unitType == UnitType.HIGHER_ORDER_COMMANDER -> {
                            shapeRenderer.color = Color(1f, 0.8f, 0.2f, 1f)
                            shapeRenderer.rect(entity.x - 13, entity.y - 13, 26f, 26f)
                            shapeRenderer.color = Color(1f, 1f, 0.5f, 1f)
                            shapeRenderer.rect(entity.x - 8, entity.y - 8, 16f, 16f)
                        }
                        

                        unitType == UnitType.API_GATEWAY -> {
                            shapeRenderer.color = Color(0.3f, 1f, 0.5f, 1f)
                            shapeRenderer.rect(entity.x - 10, entity.y - 10, 20f, 20f)

                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            shapeRenderer.line(entity.x, entity.y + 10, entity.x, entity.y + 18)
                            shapeRenderer.circle(entity.x, entity.y + 20, 3f)
                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                        }
                        unitType == UnitType.WEBSOCKET_SCOUT -> {
                            shapeRenderer.color = Color(0.2f, 1f, 0.2f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 8f)

                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            shapeRenderer.color = Color(0.2f, 1f, 0.2f, 0.4f)
                            shapeRenderer.circle(entity.x, entity.y, 14f)
                            shapeRenderer.circle(entity.x, entity.y, 20f)
                            shapeRenderer.end()
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                        }
                        

                        unitType == UnitType.CACHE_RUNNER -> {
                            shapeRenderer.color = Color(0.9f, 0.9f, 0.9f, 1f)
                            shapeRenderer.rect(entity.x - 6, entity.y - 6, 12f, 12f)
                        }
                        unitType == UnitType.INDEXER -> {
                            shapeRenderer.color = Color(0.7f, 0.5f, 0.3f, 1f)
                            shapeRenderer.rect(entity.x - 9, entity.y - 9, 18f, 18f)
                            shapeRenderer.color = Color(0.9f, 0.7f, 0.5f, 1f)
                            shapeRenderer.rect(entity.x - 5, entity.y - 5, 10f, 10f)
                        }
                        unitType == UnitType.TRANSACTION_GUARD -> {
                            shapeRenderer.color = Color(0.5f, 0.5f, 0.8f, 1f)
                            shapeRenderer.rect(entity.x - 11, entity.y - 11, 22f, 22f)
                        }
                        
                        else -> {

                            shapeRenderer.color = Color(0.3f, 1f, 0.3f, 1f)
                            shapeRenderer.circle(entity.x, entity.y, 10f)
                        }
                    }
                }
            }
            

            if (entity.type == EntityType.UNIT) {

                val time = System.currentTimeMillis() / 1000f
                

                if (entity.unitType == UnitType.ENCAPSULATION_SHIELD) {
                    shapeRenderer.end()
                    Gdx.gl.glEnable(GL20.GL_BLEND)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                    val radius = 40f + Math.sin(time * 3.0).toFloat() * 2f
                    shapeRenderer.color = Color(0.2f, 0.4f, 1f, 0.6f)
                    shapeRenderer.circle(entity.x, entity.y, radius)
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                }
                

                if (entity.unitType == UnitType.ABSTRACTION_AGENT) {

                    shapeRenderer.end()
                    Gdx.gl.glEnable(GL20.GL_BLEND)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                    shapeRenderer.color = Color(0.8f, 0.8f, 1f, 0.2f)
                    shapeRenderer.circle(entity.x, entity.y, 30f)
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                }
                

                if (entity.unitType == UnitType.DYNAMIC_DISPATCHER || entity.unitType == UnitType.HIGHER_ORDER_COMMANDER) {
                    shapeRenderer.end()
                    Gdx.gl.glEnable(GL20.GL_BLEND)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                    
                    val auraColor = if(entity.unitType == UnitType.DYNAMIC_DISPATCHER) Color.CYAN else Color.GOLD
                    val maxRadius = if(entity.unitType == UnitType.HIGHER_ORDER_COMMANDER) 120f else 60f
                    val pulse = (time * 2f) % 1f
                    
                    shapeRenderer.color = Color(auraColor.r, auraColor.g, auraColor.b, 1f - pulse)
                    shapeRenderer.circle(entity.x, entity.y, maxRadius * pulse)
                    
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                }
                

                if (entity.unitType == UnitType.DEADLOCK_TRAP) {
                     shapeRenderer.end()
                    Gdx.gl.glEnable(GL20.GL_BLEND)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                    shapeRenderer.color = Color(1f, 0f, 0f, 0.3f)
                    shapeRenderer.circle(entity.x, entity.y, 60f)
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                }
            }


            if (entity.id == selectedEntityId) {
                shapeRenderer.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.color = Color.WHITE
                shapeRenderer.circle(entity.x, entity.y, 25f)
                shapeRenderer.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            }
            

            if (entity.type != EntityType.RESOURCE_NODE) {
                val hpPct = entity.hp.toFloat() / entity.maxHp.toFloat()
                val barWidth = 40f
                val barHeight = 4f
                val yOffset = 30f
                

                shapeRenderer.color = Color(0.2f, 0f, 0f, 0.8f)
                shapeRenderer.rect(entity.x - barWidth/2, entity.y + yOffset, barWidth, barHeight)
                

                shapeRenderer.color = Color(0.2f, 1f, 0.2f, 0.8f)
                shapeRenderer.rect(entity.x - barWidth/2, entity.y + yOffset, barWidth * hpPct, barHeight)
            }
            

            if (entity.attackingTargetId != null) {
                val target = network.entities[entity.attackingTargetId!!]
                if (target != null) {
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                    
                    val isHealer = entity.unitType == UnitType.HEALER || entity.unitType == UnitType.RESTFUL_HEALER
                    val laserColor = if (isHealer) Color(0f, 1f, 0f, 0.6f) else Color(1f, 0f, 0f, 0.6f)
                    
                    shapeRenderer.color = laserColor
                    shapeRenderer.line(entity.x, entity.y, target.x, target.y)
                    
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                }
            }
        }
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
        

        batch.projectionMatrix = camera.combined
        batch.begin()
        

        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        network.entities.values.forEach { entity ->
            if (entity.type == EntityType.UNIT) {
                 val isMine = entity.ownerId == network.myId
                 val isNeutral = entity.ownerId == "0"
                 shapeRenderer.color = when {
                     isMine -> Color.GREEN
                     isNeutral -> Color.GRAY
                     else -> Color.RED
                 }

                 shapeRenderer.circle(entity.x, entity.y, 14f)
            }
        }
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()
        network.entities.values.forEach { entity ->
            val label = when(entity.type) {
                EntityType.INSTANCE -> "BASE"
                EntityType.FACTORY -> "FACTORY"
                EntityType.RESOURCE_NODE -> when(entity.resourceType) {
                    ResourceType.MEMORY -> "MEM"
                    ResourceType.CPU -> "CPU"
                    null -> "???"
                }
                EntityType.UNIT -> entity.unitType?.let { UnitStatsData.getShortName(it) } ?: "UNIT"
            }
            

            val isMine = entity.ownerId == network.myId
            val isNeutral = entity.ownerId == "0"
            
            labelFont.color = when {
                entity.type == EntityType.RESOURCE_NODE -> Color.YELLOW
                isMine -> Color.GREEN
                isNeutral -> Color.LIGHT_GRAY
                else -> Color.RED
            }
            
            labelFont.draw(batch, label, entity.x - 15, entity.y - 25)
        }
        
        batch.end()


        if (isPlacingCard) {
            val myBase = network.entities.values.find { it.ownerId == network.myId && it.type == EntityType.INSTANCE }
            if (myBase != null) {
                Gdx.gl.glEnable(GL20.GL_BLEND)

                shapeRenderer.projectionMatrix = camera.combined
                shapeRenderer.transformMatrix.idt()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                

                val mousePos = worldViewport.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
                val dx = mousePos.x - myBase.x
                val dy = mousePos.y - myBase.y
                val distSq = dx*dx + dy*dy
                val maxDist = 200f
                
                if (distSq <= maxDist * maxDist) {
                    shapeRenderer.color = Color.GREEN
                } else {
                    shapeRenderer.color = Color.RED
                }
                
                shapeRenderer.circle(myBase.x, myBase.y, maxDist)
                shapeRenderer.end()
                Gdx.gl.glDisable(GL20.GL_BLEND)
            }
        }
        


        uiViewport.apply()
        batch.projectionMatrix = uiViewport.camera.combined
        shapeRenderer.projectionMatrix = uiViewport.camera.combined
        batch.begin()
        

        

        if (network.winnerId != null) {
            val isWin = network.winnerId == network.myId
            val message = if (isWin) "VICTORY!" else "DEFEAT"
            val color = if (isWin) Color.GREEN else Color.RED
            

            batch.projectionMatrix = uiViewport.camera.combined
            
            font.color = color
            font.data.setScale(3f)
            font.draw(batch, message, 300f, 350f)
            font.data.setScale(1f)
            font.color = Color.WHITE
            font.draw(batch, "Restart the server to play again", 280f, 250f)
            batch.end()
            return
        }
        

        if (selectedEntityId != null) {
            val selected = network.entities[selectedEntityId!!]
            if (selected != null) {
                var infoY = uiViewport.worldHeight - 50f
                val infoX = uiViewport.worldWidth - 200f
                font.color = Color.CYAN
                font.draw(batch, "=== SELECTED ===", infoX, infoY)
                infoY -= 20
                font.color = Color.WHITE
                font.draw(batch, "Type: ${selected.type}", infoX, infoY)
                infoY -= 20
                font.draw(batch, "HP: ${selected.hp}/${selected.maxHp}", infoX, infoY)
                infoY -= 20
                val ownerName = network.players[selected.ownerId]?.name ?: "Neutral"
                font.draw(batch, "Owner: $ownerName", infoX, infoY)
                

                if (selected.type == EntityType.UNIT && selected.unitType != null) {
                    infoY -= 30
                    font.color = Color.YELLOW
                    font.draw(batch, "Unit: ${UnitStatsData.getShortName(selected.unitType!!)}", infoX, infoY)
                    infoY -= 20
                    font.color = Color.LIGHT_GRAY
                    val desc = UnitStatsData.getDescription(selected.unitType!!)

                    font.draw(batch, desc, infoX, infoY, 190f, Align.left, true)
                }
            }
        }
        
        batch.end()
        

        

        uiViewport.apply()
        batch.projectionMatrix = uiViewport.camera.combined
        batch.begin()
        var yOffset = uiViewport.worldHeight - 20f
        font.color = Color.WHITE
        font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, yOffset)
        network.players.values.forEach { player ->
             yOffset -= 25f
             val info = "${player.name} [MEM: ${player.memory} | CPU: ${player.cpu}]"
             font.color = Color.BLACK
             font.draw(batch, info, 22f, yOffset - 2)
             font.color = Color.WHITE
             font.draw(batch, info, 20f, yOffset)
        }
        
        if (network.players.size < 2) {
             yOffset -= 40f
             font.color = Color.YELLOW
             font.draw(batch, "WAITING FOR OPPONENT...", 20f, yOffset)
             font.draw(batch, "(Run the client again to simulate Player 2)", 20f, yOffset - 20f)
             font.color = Color.WHITE
         }


        yOffset = 140f  
        font.color = Color.LIGHT_GRAY
        font.draw(batch, "Controls: Click Card > Click Map to Deploy | ESC=Cancel | WASD=Camera", 20f, yOffset)
        
        batch.end()
        

        val myId = network.myId
        if (myId != null) {
            val myPlayer = network.players[myId]
            if (myPlayer != null && myPlayer.hand.isNotEmpty()) {
                val cardWidth = 150f
                val cardHeight = 80f
                val cardGap = 10f
                val cardsStartX = (uiViewport.worldWidth - (cardWidth + cardGap) * myPlayer.hand.size) / 2f
                val cardsY = 10f
                
                var hoveredCardIndex = -1
                
                

                val mousePos = uiViewport.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
                val mouseX = mousePos.x
                val mouseY = mousePos.y

                

                Gdx.gl.glEnable(GL20.GL_BLEND)
                shapeRenderer.projectionMatrix = uiViewport.camera.combined
                shapeRenderer.transformMatrix.idt()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                
                myPlayer.hand.forEachIndexed { index, card ->
                    val cardX = cardsStartX + index * (cardWidth + cardGap)
                    val canAfford = myPlayer.memory >= card.memoryCost && myPlayer.cpu >= card.cpuCost
                    val isSelected = selectedCardId == card.id
                    val onCooldown = myPlayer.globalCooldown > 0
                    

                    if (mouseX >= cardX && mouseX <= cardX + cardWidth &&
                        mouseY >= cardsY && mouseY <= cardsY + cardHeight) {
                        hoveredCardIndex = index
                    }
                    
                     shapeRenderer.color = when {
                        isSelected -> Color(0.2f, 0.8f, 1f, 0.9f)
                        onCooldown -> Color(0.3f, 0.3f, 0.3f, 0.5f)
                        canAfford -> Color(0.3f, 0.3f, 0.4f, 0.9f)
                        else -> Color(0.2f, 0.2f, 0.2f, 0.6f)
                    }
                    shapeRenderer.rect(cardX, cardsY, cardWidth, cardHeight)
                    

                    if (onCooldown) {
                        shapeRenderer.color = Color(0f, 0f, 0f, 0.5f)
                        val cooldownRatio = (myPlayer.globalCooldown / 1.5f).coerceIn(0f, 1f)
                        shapeRenderer.rect(cardX, cardsY, cardWidth, cardHeight * cooldownRatio)
                    }
                }
                shapeRenderer.end()
                

                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                myPlayer.hand.forEachIndexed { index, card ->
                    val cardX = cardsStartX + index * (cardWidth + cardGap)
                    shapeRenderer.color = if (selectedCardId == card.id) Color.WHITE else Color(0.5f, 0.5f, 0.5f, 1f)
                    shapeRenderer.rect(cardX, cardsY, cardWidth, cardHeight)
                }
                shapeRenderer.end()
                Gdx.gl.glDisable(GL20.GL_BLEND)
                

                batch.projectionMatrix = uiViewport.camera.combined
                batch.begin()
                myPlayer.hand.forEachIndexed { index, card ->
                    val cardX = cardsStartX + index * (cardWidth + cardGap)
                    
                    val cardName = when(card.type) {
                        CardType.BUILD_FACTORY -> "FACTORY"
                        else -> {

                           val typeName = card.type.name.removePrefix("SPAWN_")
                           try {
                               val uType = UnitType.valueOf(typeName)
                               UnitStatsData.getShortName(uType)
                           } catch (e: Exception) { card.type.name }
                        }
                    }
                    
                    font.color = Color.WHITE
                    font.draw(batch, cardName, cardX + 10, cardsY + cardHeight - 10)
                    
                    font.color = if (myPlayer.memory >= card.memoryCost) Color.GREEN else Color.RED
                    font.draw(batch, "${card.memoryCost}M", cardX + 10, cardsY + 35)
                    
                    font.color = if (myPlayer.cpu >= card.cpuCost) Color.GREEN else Color.RED
                    font.draw(batch, "${card.cpuCost}C", cardX + 80, cardsY + 35)
                }
                batch.end()
                

                if (hoveredCardIndex != -1) {
                    val card = myPlayer.hand[hoveredCardIndex]
                    val cardX = cardsStartX + hoveredCardIndex * (cardWidth + cardGap)
                    val tooltipW = 220f
                    val tooltipH = 100f

                    val tooltipX = cardX + (cardWidth - tooltipW) / 2f
                    val tooltipY = cardsY + cardHeight + 15f
                    
                    Gdx.gl.glEnable(GL20.GL_BLEND)
                    shapeRenderer.projectionMatrix = uiViewport.camera.combined
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                    shapeRenderer.color = Color(0f, 0f, 0f, 0.9f)
                    shapeRenderer.rect(tooltipX, tooltipY, tooltipW, tooltipH)
                    shapeRenderer.end()
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                    shapeRenderer.color = Color.CYAN
                    shapeRenderer.rect(tooltipX, tooltipY, tooltipW, tooltipH)
                    shapeRenderer.end()
                    Gdx.gl.glDisable(GL20.GL_BLEND)
                    
                    batch.projectionMatrix = uiViewport.camera.combined
                    batch.begin()
                    font.color = Color.YELLOW
                    
                    val typeName = card.type.name.removePrefix("SPAWN_")
                    val desc = if (card.type == CardType.BUILD_FACTORY) {
                        "Defensive Structure.\nSpawns units."
                    } else {
                         try {
                               val uType = UnitType.valueOf(typeName)
                               val stats = UnitStatsData.getStats(uType)
                               val d = UnitStatsData.getDescription(uType)
                               "HP:${stats.maxHp} DMG:${stats.damage} SPD:${stats.speed.toInt()}\n$d"
                           } catch (e: Exception) { "Unknown Unit" }
                    }
                    
                    font.draw(batch, desc, tooltipX + 10, tooltipY + tooltipH - 20, tooltipW - 20, Align.left, true)
                    batch.end()
                }
            }
        }
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        font.dispose()
        labelFont.dispose()
        shapeRenderer.dispose()
        network.dispose()
        if (::loginScreen.isInitialized) loginScreen.dispose()
    }

    override fun resize(width: Int, height: Int) {
        if (state == GameState.LOGIN) {
            loginScreen.resize(width, height)
        }
        uiViewport.update(width, height, true)
        worldViewport.update(width, height, true)
    }
}
