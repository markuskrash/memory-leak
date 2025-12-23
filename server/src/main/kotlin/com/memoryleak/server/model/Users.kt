package com.memoryleak.server.model

import org.jetbrains.exposed.sql.Table


import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val rating = integer("rating").default(1000)
}

data class User(
    val id: String,
    val username: String,
    val passwordHash: String,
    val rating: Int
)
