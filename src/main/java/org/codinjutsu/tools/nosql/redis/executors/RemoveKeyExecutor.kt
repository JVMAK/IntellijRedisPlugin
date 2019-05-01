package org.codinjutsu.tools.nosql.redis.executors

import org.codinjutsu.tools.nosql.dialog.KeyValueResult
import org.codinjutsu.tools.nosql.redis.logic.RedisQueryExecutor
import org.codinjutsu.tools.nosql.redis.model.RedisKeyType
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCommands


/**
 *
 * @author bruce ge
 */
class RemoveKeyExecutor(val lists: Set<String>) : RedisQueryExecutor {

    override fun handleRedisQuery(command: JedisCommands) {
        if (command is Jedis) {
            //need to use with bytes?
            for (oneKey in lists) {
                command.del(oneKey);
            }
        }
    }

}
