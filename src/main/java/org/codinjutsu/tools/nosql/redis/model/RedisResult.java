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

package org.codinjutsu.tools.nosql.redis.model;

import redis.clients.jedis.Tuple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisResult {

    private final List<RedisRecord> redisRecords = new LinkedList<RedisRecord>();


    public void addString(String key, String value,byte[] keyBytes) {
        redisRecords.add(new RedisRecord<String>(RedisKeyType.STRING, key, value,keyBytes));
    }

    public void addList(String key, List values,byte[] keyBytes) {
        redisRecords.add(new RedisRecord<List>(RedisKeyType.LIST, key, values,keyBytes));
    }

    public void addSet(String key, Set values,byte[] keyBytes) {
        redisRecords.add(new RedisRecord<Set>(RedisKeyType.SET, key, values,keyBytes));
    }

    public void addHash(String key, Map values,byte[] keyBytes) {
        redisRecords.add(new RedisRecord<Map>(RedisKeyType.HASH, key, values,keyBytes));
    }

    public void addSortedSet(String key, Set<Tuple> values,byte[] keyBytes) {
        redisRecords.add(new RedisRecord<Set<Tuple>>(RedisKeyType.ZSET, key, values,keyBytes));
    }

    public List<RedisRecord> getResults() {
        return redisRecords;
    }

}
