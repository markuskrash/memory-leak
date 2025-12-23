package com.memoryleak.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import com.memoryleak.server.database.DatabaseFactory
import com.memoryleak.server.routes.authRoutes
import com.memoryleak.server.service.AuthService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*


import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.ktor.ext.inject
import com.memoryleak.server.di.appModule

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}


fun Application.module() {

    DatabaseFactory.init()

    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    val authService by inject<AuthService>()

    routing {
        authRoutes(authService)
        gameSocket()
    }
}
