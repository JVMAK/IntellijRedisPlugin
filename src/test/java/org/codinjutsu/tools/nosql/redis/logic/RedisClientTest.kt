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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.codinjutsu.tools.nosql.DatabaseVendor
import org.codinjutsu.tools.nosql.ServerConfiguration
import org.codinjutsu.tools.nosql.redis.RedisBaseTest
import org.codinjutsu.tools.nosql.redis.model.*
import org.junit.Test


import org.junit.Assert.assertEquals
import redis.clients.jedis.Tuple


class RedisClientTest : RedisBaseTest() {

    @Test
    @Throws(Exception::class)
    fun loadWithEmptyFilter() {
        jedis.sadd("books", "eXtreme Programming", "Haskell for Dummies")
        jedis.set("status", "online")
        jedis.lpush("todos", "coffee", "code", "drink", "sleep")
        jedis.zadd("reviews", 19.0, "writing")
        jedis.zadd("reviews", 14.0, "reading")
        jedis.zadd("reviews", 15.0, "maths")
        val redisRecords = checkAndGetResult(EmptyQueryExecutor(), "books", "reviews", "status", "todos")
        assertRedisRecordsIndex(redisRecords, 0, "books", RedisKeyType.SET,"eXtreme Programming", "Haskell for Dummies")
        assertRedisRecordsIndex(redisRecords, 1, "reviews", RedisKeyType.ZSET,"reading","maths","writing")
        assertRedisRecordsIndex(redisRecords, 2, "status", RedisKeyType.STRING,"online")
        assertRedisRecordsIndex(redisRecords, 3, "todos", RedisKeyType.LIST,"coffee", "code", "drink", "sleep")
    }




    @Test
    @Throws(Exception::class)
    fun loadWithFilter() {
        jedis.sadd("books", "eXtreme Programming", "Haskell for Dummies")
        jedis.set("status", "online")
        jedis.lpush("todos", "coffee", "code", "drink", "sleep")
        jedis.zadd("reviews", 12.0, "writing")
        jedis.zadd("reviews", 14.0, "reading")
        jedis.zadd("reviews", 15.0, "maths")

        val redisClient = RedisClient()
        val serverConfiguration = ServerConfiguration()
        serverConfiguration.databaseVendor = DatabaseVendor.REDIS
        serverConfiguration.serverUrl = "localhost:6379"

        val query = RedisQuery("reviews")
        val result = redisClient.loadRecords(serverConfiguration, RedisDatabase("1"), query, EmptyQueryExecutor())

        val redisRecords = result.results
        assertEquals(1, redisRecords.size.toLong())
        val redisRecord = redisRecords[0]
        assertEquals(RedisKeyType.ZSET, redisRecord.keyType)
        assertEquals("reviews", redisRecord.key)
    }


}
