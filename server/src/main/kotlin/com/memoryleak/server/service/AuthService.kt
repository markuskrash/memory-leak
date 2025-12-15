package com.memoryleak.server.service

import com.memoryleak.server.database.DatabaseFactory.dbQuery
import com.memoryleak.server.database.RedisClient
import com.memoryleak.server.model.User
import com.memoryleak.server.model.Users
import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.UUID

class AuthService {
    
    suspend fun register(username: String, password: String): String? {
        val existing = dbQuery {
            Users.select { Users.username eq username }.singleOrNull()
        }
        if (existing != null) return null // User exists

        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val newId = UUID.randomUUID().toString()
        
        dbQuery {
            Users.insert {
                it[Users.id] = newId
                it[Users.username] = username
                it[Users.passwordHash] = hashedPassword
            }
        }
        
        return login(username, password)
    }

    suspend fun login(username: String, password: String): String? {
        val row = dbQuery {
            Users.select { Users.username eq username }.singleOrNull()
        } ?: return null

        val storedHash = row[Users.passwordHash]
        val result = BCrypt.verifyer().verify(password.toCharArray(), storedHash)

        if (result.verified) {
            val token = UUID.randomUUID().toString()
            val userId = row[Users.id]
            RedisClient.setSession(token, userId)
            return token
        }
        return null
    }
    
    fun verifyToken(token: String): String? {
        return RedisClient.getSession(token)
    }
    
    suspend fun getUsername(userId: String): String? {
        return dbQuery {
            Users.select { Users.id eq userId }.singleOrNull()?.get(Users.username)
        }
    }
}
