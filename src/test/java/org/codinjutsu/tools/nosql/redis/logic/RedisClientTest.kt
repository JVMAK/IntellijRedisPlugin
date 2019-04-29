/*
 * Copyright (c) 2015 David Boissier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codinjutsu.tools.nosql.redis.logic

import org.codinjutsu.tools.nosql.DatabaseVendor
import org.codinjutsu.tools.nosql.ServerConfiguration
import org.codinjutsu.tools.nosql.redis.model.*
import org.junit.Before
import org.junit.Test
import redis.clients.jedis.Jedis


import org.junit.Assert.assertEquals

class RedisClientTest {

    private var jedis: Jedis? = null

    @Test
    @Throws(Exception::class)
    fun loadWithEmptyFilter() {
        jedis!!.sadd("books", "eXtreme Programming", "Haskell for Dummies")
        jedis!!.set("status", "online")
        jedis!!.lpush("todos", "coffee", "code", "drink", "sleep")
        jedis!!.zadd("reviews", 12.0, "writing")
        jedis!!.zadd("reviews", 14.0, "reading")
        jedis!!.zadd("reviews", 15.0, "maths")

        val redisClient = RedisClient()
        val serverConfiguration = ServerConfiguration()
        serverConfiguration.databaseVendor = DatabaseVendor.REDIS
        serverConfiguration.serverUrl = "localhost:6379"

        val query = RedisQuery("*")
        val result = redisClient.loadRecords(serverConfiguration, RedisDatabase("1"), query)

        val redisRecords = result.results
        assertEquals(4, redisRecords.size.toLong())

        redisRecords.sortWith(compareBy {it.key})

        var redisRecord: RedisRecord<*> = redisRecords[0]
        assertEquals(RedisKeyType.SET, redisRecord.keyType)
        assertEquals("books", redisRecord.key)
        redisRecord = redisRecords[1]
        assertEquals(RedisKeyType.ZSET, redisRecord.keyType)
        assertEquals("reviews", redisRecord.key)
        redisRecord = redisRecords[2]
        assertEquals(RedisKeyType.STRING, redisRecord.keyType)
        assertEquals("status", redisRecord.key)
        redisRecord = redisRecords[3]
        assertEquals("todos", redisRecord.key)
        assertEquals(RedisKeyType.LIST, redisRecord.keyType)
    }

    @Test
    @Throws(Exception::class)
    fun loadWithFilter() {
        jedis!!.sadd("books", "eXtreme Programming", "Haskell for Dummies")
        jedis!!.set("status", "online")
        jedis!!.lpush("todos", "coffee", "code", "drink", "sleep")
        jedis!!.zadd("reviews", 12.0, "writing")
        jedis!!.zadd("reviews", 14.0, "reading")
        jedis!!.zadd("reviews", 15.0, "maths")

        val redisClient = RedisClient()
        val serverConfiguration = ServerConfiguration()
        serverConfiguration.databaseVendor = DatabaseVendor.REDIS
        serverConfiguration.serverUrl = "localhost:6379"

        val query = RedisQuery("reviews")
        val result = redisClient.loadRecords(serverConfiguration, RedisDatabase("1"), query)

        val redisRecords = result.results
        assertEquals(1, redisRecords.size.toLong())
        val redisRecord = redisRecords[0]
        assertEquals(RedisKeyType.ZSET, redisRecord.keyType)
        assertEquals("reviews", redisRecord.key)
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        jedis = Jedis("localhost", 6379)
        jedis!!.select(1)
        jedis!!.flushDB()

    }

    @Throws(Exception::class)
    fun tearDown() {
        jedis!!.close()
    }
}
