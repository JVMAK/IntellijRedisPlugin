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

package org.codinjutsu.tools.nosql.redis.logic;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.nosql.DatabaseVendor;
import org.codinjutsu.tools.nosql.ServerConfiguration;
import org.codinjutsu.tools.nosql.commons.logic.DatabaseClient;
import org.codinjutsu.tools.nosql.commons.model.AuthenticationSettings;
import org.codinjutsu.tools.nosql.commons.model.Database;
import org.codinjutsu.tools.nosql.commons.model.DatabaseServer;
import org.codinjutsu.tools.nosql.redis.model.RedisDatabase;
import org.codinjutsu.tools.nosql.redis.model.RedisKeyType;
import org.codinjutsu.tools.nosql.redis.model.RedisQuery;
import org.codinjutsu.tools.nosql.redis.model.RedisResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisClient implements DatabaseClient {

    public static RedisClient getInstance(Project project) {
        return ServiceManager.getService(project, RedisClient.class);
    }


    @Override
    public void connect(ServerConfiguration serverConfiguration) {
        Jedis jedis = createJedis(serverConfiguration);
        jedis.connect();
        String userDatabase = serverConfiguration.getUserDatabase();
        int index = 0;
        if (StringUtils.isNotEmpty(userDatabase)) {
            index = Integer.parseInt(userDatabase);
        }
        jedis.select(index);
    }

    @Override
    public void loadServer(DatabaseServer databaseServer) {
        Jedis jedis = createJedis(databaseServer.getConfiguration());
        List<String> databaseNumberTuple = jedis.configGet("databases");
        List<Database> databases = new LinkedList<>();
        String userDatabase = databaseServer.getConfiguration().getUserDatabase();
        if (StringUtils.isNotEmpty(userDatabase)) {
            databases.add(new RedisDatabase(userDatabase));
        } else {
            int totalNumberOfDatabase = Integer.parseInt(databaseNumberTuple.get(1));
            for (int databaseNumber = 0; databaseNumber < totalNumberOfDatabase; databaseNumber++) {
                databases.add(new RedisDatabase(String.valueOf(databaseNumber)));
            }
        }
        databaseServer.setDatabases(databases);
    }

    @Override
    public void cleanUpServers() {

    }

    @Override
    public void registerServer(DatabaseServer databaseServer) {

    }

    @Override
    public ServerConfiguration defaultConfiguration() {
        ServerConfiguration configuration = new ServerConfiguration();
        configuration.setDatabaseVendor(DatabaseVendor.REDIS);
        configuration.setServerUrl(DatabaseVendor.REDIS.defaultUrl);
        configuration.setAuthenticationSettings(new AuthenticationSettings());
        return configuration;
    }


    public RedisResult loadRecords(ServerConfiguration serverConfiguration, RedisDatabase database, RedisQuery query) {
        Jedis jedis = createJedis(serverConfiguration);
        jedis.connect();
        RedisResult redisResult = new RedisResult();
        int index = Integer.parseInt(database.getName());
        jedis.select(index);

        Set<String> keys = jedis.keys(query.getFilter());
        for (String key : keys) {
            RedisKeyType keyType = RedisKeyType.getKeyType(jedis.type(key));
            if (RedisKeyType.LIST.equals(keyType)) {
                List<String> values = jedis.lrange(key, 0, -1);
                redisResult.addList(key, values);
            } else if (RedisKeyType.SET.equals(keyType)) {
                Set<String> values = jedis.smembers(key);
                redisResult.addSet(key, values);
            } else if (RedisKeyType.HASH.equals(keyType)) {
                Map<String, String> values = jedis.hgetAll(key);
                redisResult.addHash(key, values);
            } else if (RedisKeyType.ZSET.equals(keyType)) {
                Set<Tuple> valuesWithScores = jedis.zrangeByScoreWithScores(key, "-inf", "+inf");
                redisResult.addSortedSet(key, valuesWithScores);
            } else if (RedisKeyType.STRING.equals(keyType)) {
                String value = jedis.get(key);
                redisResult.addString(key, value);
            }
        }
        return redisResult;
    }

    private Jedis createJedis(ServerConfiguration serverConfiguration) {
        String redisUri = "redis://";
        String password = serverConfiguration.getAuthenticationSettings().getPassword();
        if (StringUtils.isNotEmpty(password)) {
            redisUri += ":" + password + "@";
        }
        redisUri += serverConfiguration.getServerUrl();
        return new Jedis(redisUri);
    }
}
