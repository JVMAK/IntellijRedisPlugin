package org.codinjutsu.tools.nosql.redis.logic

import org.codinjutsu.tools.nosql.redis.model.RedisResult
import redis.clients.jedis.JedisCommands


/**
 *
 * @author bruce ge
 */
interface RedisQueryExecutor {
    fun handleRedisQuery(command: JedisCommands): ExecuteResult
}
