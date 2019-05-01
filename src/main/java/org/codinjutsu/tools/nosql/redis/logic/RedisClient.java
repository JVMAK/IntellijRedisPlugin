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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import kotlin.text.Charsets;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.nosql.DatabaseVendor;
import org.codinjutsu.tools.nosql.ServerConfiguration;
import org.codinjutsu.tools.nosql.commons.logic.DatabaseClient;
import org.codinjutsu.tools.nosql.commons.model.AuthenticationSettings;
import org.codinjutsu.tools.nosql.commons.model.Database;
import org.codinjutsu.tools.nosql.commons.model.DatabaseServer;
import org.codinjutsu.tools.nosql.redis.model.*;
import redis.clients.jedis.*;

import java.nio.charset.StandardCharsets;
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


//    public ExecuteResult executeQuery(ServerConfiguration serverConfiguration, RedisDatabase database, RedisQueryExecutor query) {
//        if (createJedis(serverConfiguration) instanceof Jedis) {
//            Jedis jedis = (Jedis) createJedis(serverConfiguration);
//            jedis.connect();
//            int index = Integer.parseInt(database.getName());
//            jedis.select(index);
//            return query.handleRedisQuery(jedis);
//        } else if (createJedis(serverConfiguration) instanceof JedisCluster) {
//            JedisCluster jedisCluster = (JedisCluster) createJedis(serverConfiguration);
//            return query.handleRedisQuery(jedisCluster);
//        } else {
//            throw new IllegalStateException("should not happen");
//        }
//    }


    public RedisResult loadRecords(ServerConfiguration serverConfiguration, RedisDatabase database, RedisQuery query, RedisQueryExecutor executor) {
        JedisCommands commands = createJedis(serverConfiguration);
        byte[] filterCondition = query.getFilter().getBytes(Charsets.UTF_8);
        if (commands instanceof Jedis) {
            Jedis jedis = (Jedis) commands;
            jedis.connect();
            jedis.select(Integer.parseInt(database.getName()));
            executor.handleRedisQuery(jedis);
            return keySearch(jedis, filterCondition);
        }
        if (commands instanceof JedisCluster) {
            JedisCluster cluster = (JedisCluster) commands;
            executor.handleRedisQuery(commands);
            return keySearch(cluster, filterCondition);
        }
        return null;
    }

    private RedisResult keySearch(JedisCommands commands, byte[] filterCondition) {

        if (commands instanceof Jedis) {
            Jedis jedis = (Jedis) commands;
            return sortKeys(jedis, jedis.keys(filterCondition));
        }
        if (commands instanceof JedisCluster) {
            JedisCluster cluster = (JedisCluster) commands;
            TreeSet<RedisResult> resultTreeSet = new TreeSet<>();
            Map<String, JedisPool> clusterNodes = cluster.getClusterNodes();
            clusterNodes.keySet().stream().map(clusterNodes::get).map(JedisPool::getResource).forEachOrdered(conn -> {
                try {
                    resultTreeSet.add(sortKeys(conn, conn.keys(filterCondition)));
                } catch (Exception e) {
                } finally {
                    conn.close();
                }
            });

            RedisResult result = new RedisResult();
            final List<RedisRecord> redisRecords = new LinkedList<>();
            resultTreeSet.forEach(x -> redisRecords.addAll(x.getResults()));
            redisRecords.forEach(redisRecord -> {
                RedisKeyType keyType = redisRecord.getKeyType();
                String key = redisRecord.getKey();
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                Object value = redisRecord.getValue();
                if (RedisKeyType.LIST.equals(keyType)) {
                    result.addList(key, (List) value, keyBytes);
                } else if (RedisKeyType.SET.equals(keyType)) {
                    result.addSet(key, (Set) value, keyBytes);
                } else if (RedisKeyType.HASH.equals(keyType)) {
                    result.addHash(key, (Map) value, keyBytes);
                } else if (RedisKeyType.ZSET.equals(keyType)) {
                    result.addSortedSet(key, (Set<Tuple>) value, keyBytes);
                } else if (RedisKeyType.STRING.equals(keyType)) {
                    result.addString(key, (String) value, keyBytes);
                } else {
                    //ignore this.
                    throw new RuntimeException("unSupport type:" + keyType);
                }

            });
            return result;
        }

        return null;
    }

    private RedisResult sortKeys(Jedis jedis, Set<byte[]> keys) {
        RedisResult redisResult = new RedisResult();
        for (byte[] key : keys) {
            //may be null pointer.
            String type = jedis.type(key);
            RedisKeyType keyType = RedisKeyType.getKeyType(type);
            if (RedisKeyType.LIST.equals(keyType)) {
                List<byte[]> values = jedis.lrange(key, 0, -1);
                List<String> collect = values.stream().map(this::convertByteToString).collect(Collectors.toList());
                redisResult.addList(convertByteToString(key), collect, key);
            } else if (RedisKeyType.SET.equals(keyType)) {
                Set<byte[]> values = jedis.smembers(key);
                Set<String> collect = values.stream().map(this::convertByteToString).collect(Collectors.toSet());
                redisResult.addSet(convertByteToString(key), collect, key);
            } else if (RedisKeyType.HASH.equals(keyType)) {
                Map<byte[], byte[]> values = jedis.hgetAll(key);
                Map<String, String> myValues = Maps.newHashMap();
                for (byte[] bytes : values.keySet()) {
                    myValues.put(convertByteToString(bytes), convertByteToString(values.get(bytes)));
                }
                redisResult.addHash(convertByteToString(key), myValues, key);
            } else if (RedisKeyType.ZSET.equals(keyType)) {
                Set<Tuple> valuesWithScores = jedis.zrangeByScoreWithScores(key, "-inf".getBytes(Charsets.UTF_8), "+inf".getBytes(Charsets.UTF_8));
                redisResult.addSortedSet(convertByteToString(key), valuesWithScores, key);
            } else if (RedisKeyType.STRING.equals(keyType)) {
                byte[] value = jedis.get(key);
                redisResult.addString(convertByteToString(key), convertByteToString(value), key);
            } else {
                //ignore this.
                throw new RuntimeException("unSupport type:" + type);
            }
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
                String password = serverConfiguration.getAuthenticationSettings().getPassword();
//    In this way, when special characters are input(example: characters '[' or ']'), special characters will be abnormal.

//                if (StringUtils.isNotEmpty(password)) {
//                    redisUri += ":" + password + "@";
//                }
                redisUri += serverConfiguration.getServerUrl();
                Jedis jedis = new Jedis(redisUri);
                if (StringUtils.isNotEmpty(password)) {
                    jedis.auth(password);
                }
                return jedis;
            }
        }
        throw new RuntimeException("Service configuration error:");
    }
}
