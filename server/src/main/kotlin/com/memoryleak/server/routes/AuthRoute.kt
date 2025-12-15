package com.memoryleak.server.routes

import com.memoryleak.server.service.AuthService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val username: String, val password: String)

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            try {
                val request = call.receive<AuthRequest>()
                val token = authService.register(request.username, request.password)
                if (token != null) {
                    call.respond(mapOf("token" to token))
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.Conflict, "Username already exists")
                }
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid request")
            }
        }
        
        post("/login") {
            try {
                val request = call.receive<AuthRequest>()
                val token = authService.login(request.username, request.password)
                if (token != null) {
                    call.respond(mapOf("token" to token))
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid request")
            }
        }
    }
}
