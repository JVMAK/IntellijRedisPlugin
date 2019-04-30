package org.codinjutsu.tools.nosql.redis.logic

import org.assertj.core.api.Assertions
import org.codinjutsu.tools.nosql.dialog.KeyValueResult
import org.codinjutsu.tools.nosql.redis.RedisBaseTest
import org.codinjutsu.tools.nosql.redis.executors.AddKeyValueExecutor
import org.codinjutsu.tools.nosql.redis.model.RedisKeyType
import org.junit.Test


/**
 *
 * @author bruce ge
 */
class AddKeyValueTest : RedisBaseTest() {

    @Test
    fun testAddSimpleStringKeyValue() {
        val redisRecords =
            checkAndGetResult(AddKeyValueExecutor(KeyValueResult("hello", RedisKeyType.STRING, "world")), "hello")
        assertRedisRecordsIndex(redisRecords,0,"hello",RedisKeyType.STRING,"world")
    }


    @Test
    fun testAddListKeyValue() {
        val redisRecords =
            checkAndGetResult(AddKeyValueExecutor(KeyValueResult("hello", RedisKeyType.LIST, "a,b")), "hello")
        assertRedisRecordsIndex(redisRecords,0,"hello",RedisKeyType.LIST,"a","b")
    }

    @Test
    fun testAddSetValue() {
        val redisRecords =
            checkAndGetResult(AddKeyValueExecutor(KeyValueResult("hello", RedisKeyType.SET, "a,b")), "hello")
        assertRedisRecordsIndex(redisRecords,0,"hello",RedisKeyType.SET,"a","b")
    }


    @Test
    fun testAddZSetValue() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Test
    fun testAddHashValue() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



}
