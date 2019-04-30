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

import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ArrayUtil;
import kotlin.text.Charsets;
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
import redis.clients.jedis.*;

import java.util.*;
import java.util.stream.Collectors;

public class RedisClient implements DatabaseClient {

    public static RedisClient getInstance(Project project) {
        return ServiceManager.getService(project, RedisClient.class);
    }


    private static Logger LOG = Logger.getInstance(RedisClient.class);


    @Override
    public void connect(ServerConfiguration serverConfiguration) {
        if (createJedis(serverConfiguration) instanceof Jedis) {
            Jedis jedis = (Jedis) createJedis(serverConfiguration);
            jedis.connect();
            String userDatabase = serverConfiguration.getUserDatabase();
            int index = 0;
            if (StringUtils.isNotEmpty(userDatabase)) {
                index = Integer.parseInt(userDatabase);
            }
            jedis.select(index);
        }
    }

    @Override
    public void loadServer(DatabaseServer databaseServer) {
        if (createJedis(databaseServer.getConfiguration()) instanceof Jedis) {
            Jedis jedis = (Jedis) createJedis(databaseServer.getConfiguration());
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


    public ExecuteResult executeQuery(ServerConfiguration serverConfiguration, RedisDatabase database, RedisQueryExecutor query) {
        if (createJedis(serverConfiguration) instanceof Jedis) {
            Jedis jedis = (Jedis) createJedis(serverConfiguration);
            jedis.connect();
            int index = Integer.parseInt(database.getName());
            jedis.select(index);
            return query.handleRedisQuery(jedis);
        } else if (createJedis(serverConfiguration) instanceof JedisCluster) {
            JedisCluster jedisCluster = (JedisCluster) createJedis(serverConfiguration);
            return query.handleRedisQuery(jedisCluster);
        } else {
            throw new IllegalStateException("should not happen");
        }
    }


    public RedisResult loadRecords(ServerConfiguration serverConfiguration, RedisDatabase database, RedisQuery query, RedisQueryExecutor executor) {
        RedisResult redisResult = new RedisResult();
        if (createJedis(serverConfiguration) instanceof Jedis) {
            Jedis jedis = (Jedis) createJedis(serverConfiguration);
            jedis.connect();
            int index = Integer.parseInt(database.getName());
            jedis.select(index);

            ExecuteResult executeResult = executor.handleRedisQuery(jedis);
            if(executeResult.getHasError()){
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showErrorDialog(executeResult.getErrorMsg(), "Redis Execute Failed");
                    }
                });
            }

            Set<byte[]> keys = jedis.keys(query.getFilter().getBytes(Charsets.UTF_8));
            for (byte[] key : keys) {
                //may be null pointer.
                String type = jedis.type(key);
                RedisKeyType keyType = RedisKeyType.getKeyType(type);
                if (RedisKeyType.LIST.equals(keyType)) {
                    List<byte[]> values = jedis.lrange(key, 0, -1);
                    List<String> collect = values.stream().map(b -> convertByteToString(b)).collect(Collectors.toList());
                    redisResult.addList(convertByteToString(key), collect);
                } else if (RedisKeyType.SET.equals(keyType)) {
                    Set<byte[]> values = jedis.smembers(key);
                    Set<String> collect = values.stream().map(b -> convertByteToString(b)).collect(Collectors.toSet());
                    redisResult.addSet(convertByteToString(key), collect);
                } else if (RedisKeyType.HASH.equals(keyType)) {
                    Map<byte[], byte[]> values = jedis.hgetAll(key);
                    Map<String, String> myValues = Maps.newHashMap();
                    for (byte[] bytes : values.keySet()) {
                        myValues.put(convertByteToString(bytes), convertByteToString(values.get(bytes)));
                    }
                    redisResult.addHash(convertByteToString(key), myValues);
                } else if (RedisKeyType.ZSET.equals(keyType)) {
                    Set<Tuple> valuesWithScores = jedis.zrangeByScoreWithScores(key, "-inf".getBytes(Charsets.UTF_8), "+inf".getBytes(Charsets.UTF_8));
                    redisResult.addSortedSet(convertByteToString(key), valuesWithScores);
                } else if (RedisKeyType.STRING.equals(keyType)) {
                    byte[] value = jedis.get(key);
                    redisResult.addString(convertByteToString(key), convertByteToString(value));
                } else {
                    //ignore this.
                    throw new RuntimeException("unSupport type:" + type);
                }
            }
        } else if (createJedis(serverConfiguration) instanceof JedisCluster) {
            JedisCluster jedisCluster = (JedisCluster) createJedis(serverConfiguration);

        }
        return redisResult;
    }

    private String convertByteToString(byte[] b) {
        if (b == null) {
            return null;
        }
        return new String(b, Charsets.UTF_8);
    }

    private JedisCommands createJedis(ServerConfiguration serverConfiguration) {
        String[] servers = serverConfiguration.getServerUrl().split(",");
        if (!ArrayUtil.isEmpty(servers)) {
            if (servers.length > 1) {
                Set<HostAndPort> hostAndPortSet = new HashSet<>();
                for (String hostAndPort : servers) {
                    String[] hostAndPortArry = hostAndPort.split(":");
                    HostAndPort andPort = new HostAndPort(hostAndPortArry[0], Integer.valueOf(hostAndPortArry[1]));
                    hostAndPortSet.add(andPort);
                }
                return new JedisCluster(hostAndPortSet);
            }
            if (servers.length == 1) {
                String redisUri = "redis://";
                String redisPwd = serverConfiguration.getAuthenticationSettings().getPassword();
                if (StringUtils.isNotEmpty(redisPwd)) {
                    redisUri += ":" + redisPwd + "@";
                }
                redisUri += serverConfiguration.getServerUrl();
                Jedis jedis = new Jedis(redisUri);
                String password = serverConfiguration.getAuthenticationSettings().getPassword();
                if (StringUtils.isNotEmpty(password)) {
                    jedis.auth(password);
                }
                return jedis;
            }
        }
        return null;
    }
}
