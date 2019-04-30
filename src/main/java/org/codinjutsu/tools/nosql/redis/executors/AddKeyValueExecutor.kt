package org.codinjutsu.tools.nosql.redis.executors

import org.apache.commons.lang3.exception.ExceptionUtils
import org.codinjutsu.tools.nosql.dialog.KeyValueResult
import org.codinjutsu.tools.nosql.redis.logic.ExecuteResult
import org.codinjutsu.tools.nosql.redis.logic.RedisQueryExecutor
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCommands


/**
 *
 * @author bruce ge
 */
class AddKeyValueExecutor(val keyValueResult: KeyValueResult) : RedisQueryExecutor {

    override fun handleRedisQuery(command: JedisCommands): ExecuteResult {
        try {
            if (command is Jedis) {
                //need to use with bytes?
                val key = keyValueResult.key
                val value = keyValueResult.value
                when (keyValueResult.keyType) {
                    "Simple" -> {
                        command.set(key, value)
                    }
                    "List" -> {
                        //todo check Integer ect.
                        val split = value.split(",").toTypedArray();
                        command.lpush(key, *split);
                    }
                    "Set" -> {
                        val split = value.split(",").toTypedArray();
                        command.sadd(key, *split);
                    }
                }

                return ExecuteResult(false);
            }
            return ExecuteResult(false);
        } catch (e: Exception) {
            return ExecuteResult(true, ExceptionUtils.getStackTrace(e)!!)
        }
    }

}
