package org.codinjutsu.tools.nosql.redis

import org.assertj.core.api.Assertions
import org.codinjutsu.tools.nosql.DatabaseVendor
import org.codinjutsu.tools.nosql.ServerConfiguration
import org.codinjutsu.tools.nosql.redis.logic.EmptyQueryExecutor
import org.codinjutsu.tools.nosql.redis.logic.RedisClient
import org.codinjutsu.tools.nosql.redis.logic.RedisQueryExecutor
import org.codinjutsu.tools.nosql.redis.model.RedisDatabase
import org.codinjutsu.tools.nosql.redis.model.RedisKeyType
import org.codinjutsu.tools.nosql.redis.model.RedisQuery
import org.codinjutsu.tools.nosql.redis.model.RedisRecord
import org.junit.Assert
import org.junit.Before
import redis.clients.jedis.Jedis
import redis.clients.jedis.Tuple


/**
 *
 * @author bruce ge
 */
open abstract class RedisBaseTest {
    public lateinit var jedis: Jedis

    @Before
    @Throws(Exception::class)
    fun setUp() {
        jedis = Jedis("localhost", 6379)
        jedis.select(1)
        jedis.flushDB()

    }

    @Throws(Exception::class)
    fun tearDown() {
        jedis.close()
    }


    fun checkAndGetResult(emptyQueryExecutor: RedisQueryExecutor, vararg results:String): List<RedisRecord<Any>> {
        val redisClient = RedisClient()
        val serverConfiguration = ServerConfiguration()
        serverConfiguration.databaseVendor = DatabaseVendor.REDIS
        serverConfiguration.serverUrl = "localhost:6379"
        val query = RedisQuery("*")
        val result = redisClient.loadRecords(serverConfiguration, RedisDatabase("1"), query, emptyQueryExecutor)
        val redisRecords = result.results
        val map = redisRecords.map { it.key }
        Assertions.assertThat(map).containsExactlyInAnyOrder(*results);
        redisRecords.sortWith(compareBy { it.key })
        return redisRecords
    }


     fun assertRedisRecordsIndex(
        redisRecords: List<RedisRecord<Any>>,
        i: Int,
        key: String,
        type: RedisKeyType,
        vararg valueList: String
    ): RedisRecord<*> {
        var redisRecord: RedisRecord<*> = redisRecords[i]
        Assert.assertEquals(type, redisRecord.keyType)
        Assert.assertEquals(key, redisRecord.key)
        val value = redisRecord.value
        when (type) {
            RedisKeyType.SET -> {
                val list = value as Set<*>
                val map = list.map { it as String }
                Assertions.assertThat(map).containsExactlyInAnyOrder(*valueList);
            }

            RedisKeyType.HASH -> {
                //todo
            }

            RedisKeyType.LIST -> {
                val list = value as List<*>
                val map = list.map { it as String }
                Assertions.assertThat(map).containsExactlyInAnyOrder(*valueList);
            }

            RedisKeyType.STRING -> {
                val s = value as String
                Assertions.assertThat(valueList).hasSize(1)
                Assertions.assertThat(s).isEqualTo(valueList.get(0));
            }

            RedisKeyType.ZSET -> {
                val list = value as Set<*>
                val map = list.map {it as Tuple
                }.sortedBy { it.score }.map { it.element }
                Assertions.assertThat(map).containsExactly(*valueList);
            }
        }
        return redisRecord
    }
}
