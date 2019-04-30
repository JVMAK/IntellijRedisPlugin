package org.codinjutsu.tools.nosql.dialog

import org.codinjutsu.tools.nosql.redis.model.RedisKeyType

/**
 * @author bruce ge
 */
data class KeyValueResult(val key: String, val keyType: RedisKeyType, val value: String)
