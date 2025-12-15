package com.memoryleak.server.database

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisClient {
    private val pool = JedisPool(JedisPoolConfig(), "localhost", 6379)

    fun setSession(token: String, userId: String, ttlSeconds: Long = 3600) {
        pool.resource.use { jedis ->
            jedis.setex(token, ttlSeconds, userId)
        }
    }

    fun getSession(token: String): String? {
        return pool.resource.use { jedis ->
            jedis.get(token)
        }
    }
    
    fun deleteSession(token: String) {
        pool.resource.use { jedis ->
            jedis.del(token)
        }
    }
}
