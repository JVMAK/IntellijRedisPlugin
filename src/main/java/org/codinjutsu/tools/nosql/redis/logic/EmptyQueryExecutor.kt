package org.codinjutsu.tools.nosql.redis.logic

import redis.clients.jedis.JedisCommands


/**
 *
 * @author bruce ge
 */
class EmptyQueryExecutor : RedisQueryExecutor {
    override fun handleRedisQuery(command: JedisCommands): ExecuteResult {
        return ExecuteResult(false, "");
    }
}
