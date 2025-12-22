package com.memoryleak.core

import com.memoryleak.shared.model.GameEntity
import com.memoryleak.shared.network.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class NetworkClient(private val host: String = "127.0.0.1") {
    private val client = HttpClient {
        install(WebSockets)
    }


    val entities = ConcurrentHashMap<String, GameEntity>()
    val players = ConcurrentHashMap<String, com.memoryleak.shared.model.PlayerState>()

    var myId: String? = null
    var winnerId: String? = null
    
    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(token: String) {
        scope.launch {
            try {

                client.webSocket(method = HttpMethod.Get, host = host, port = 8080, path = "/game?token=$token") {
                    session = this
                    println("Connected to server")
                    
                    while(true) {
                        val frame = incoming.receive()
                        if (frame is Frame.Text) {
                            handleMessage(frame.readText())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun login(username: String, pass: String): String? {
        return try {
            val response = client.post("http://$host:8080/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$username","password":"$pass"}""")
            }
            if (response.status == HttpStatusCode.OK) {
                val json = response.bodyAsText()


                val token = json.substringAfter("token\":\"").substringBefore("\"")
                token
            } else {
                println("Login failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun register(username: String, pass: String): String? {
        return try {
            val response = client.post("http://$host:8080/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$username","password":"$pass"}""")
            }
            if (response.status == HttpStatusCode.OK) {
                val json = response.bodyAsText()
                val token = json.substringAfter("token\":\"").substringBefore("\"")
                token
            } else {
                println("Register failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleMessage(text: String) {
        try {
            val packet = Json.decodeFromString<Packet>(text)
            
            when(packet) {
                is LoginPacket -> { /* Handled by join flow usually, or ignore */ }
                is JoinAckPacket -> {
                    myId = packet.playerId
                    println("My ID is $myId")
                }
                is StateUpdatePacket -> {


                    val currentIds = packet.entities.map { it.id }.toSet()


                    packet.entities.forEach { entity ->
                        entities[entity.id] = entity
                    }


                    players.clear()
                    packet.players.forEach { player ->
                        players[player.id] = player
                    }


                    entities.keys.filter { !currentIds.contains(it) }.forEach { 
                        entities.remove(it)
                    }
                }
                is GameOverPacket -> {
                    winnerId = packet.winnerId
                    println("Game Over! Winner: ${winnerId ?: "No winner"}")
                }
                else -> {
                    println("Received unknown packet type: ${packet::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            println("msg error: ${e.message}")
        }
    }

    fun sendCommand(cmd: CommandPacket) {
        scope.launch {
            val json = Json.encodeToString<Packet>(cmd)
            session?.send(Frame.Text(json))
        }
    }
    
    fun dispose() {
        client.close()
        scope.cancel()
    }
}
