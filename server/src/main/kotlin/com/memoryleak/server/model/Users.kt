package com.memoryleak.server.model

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 36)
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val rating = integer("rating").default(1000)
    
    override val primaryKey = PrimaryKey(id)
}

data class User(
    val id: String,
    val username: String,
    val passwordHash: String,
    val rating: Int
)
