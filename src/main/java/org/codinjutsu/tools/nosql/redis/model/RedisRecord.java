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

public class RedisRecord<T> {
    private final RedisKeyType keyType;
    private final String keyName;
    private final T keyValue;
    private final byte[] keyNameBytes;

    public RedisRecord(RedisKeyType keyType, String keyName, T keyValue,byte[] keyNameBytes) {
        this.keyType = keyType;
        this.keyName = keyName;
        this.keyValue = keyValue;
        this.keyNameBytes = keyNameBytes;
    }

    public RedisKeyType getKeyType() {
        return keyType;
    }

    public String getKey() {
        return keyName;
    }

    public T getValue() {
        return keyValue;
    }


    public byte[] getKeyNameBytes() {
        return keyNameBytes;
    }
}
