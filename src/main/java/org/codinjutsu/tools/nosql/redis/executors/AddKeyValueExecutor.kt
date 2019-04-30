package org.codinjutsu.tools.nosql.redis.executors

import org.apache.commons.lang3.exception.ExceptionUtils
import org.codinjutsu.tools.nosql.dialog.KeyValueResult
import org.codinjutsu.tools.nosql.redis.logic.ExecuteResult
import org.codinjutsu.tools.nosql.redis.logic.RedisQueryExecutor
import org.codinjutsu.tools.nosql.redis.model.RedisKeyType
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCommands
import java.lang.RuntimeException


/**
 *
 * @author bruce ge
 */
class AddKeyValueExecutor(val keyValueResult: KeyValueResult) : RedisQueryExecutor {

    override fun handleRedisQuery(command: JedisCommands) {
        if (command is Jedis) {
            //need to use with bytes?
            val key = keyValueResult.key
            val value = keyValueResult.value
            when (keyValueResult.keyType) {
                RedisKeyType.STRING -> {
                    command.set(key, value)
                }
                RedisKeyType.LIST -> {
                    //todo check Integer ect.
                    val split = value.split(",").toTypedArray();
                    command.lpush(key, *split);
                }
                RedisKeyType.SET -> {
                    val split = value.split(",").toTypedArray();
                    command.sadd(key, *split);
                }
                else -> throw RuntimeException("not supported now");
            }
        }

    }

}
