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

package org.codinjutsu.tools.nosql.commons.view;

import com.intellij.openapi.Disposable;
import org.codinjutsu.tools.nosql.redis.logic.RedisQueryExecutor;
import org.codinjutsu.tools.nosql.redis.model.RedisQuery;

import javax.swing.*;

public abstract class NoSqlResultView<R> extends JPanel implements Disposable {

    public abstract void showResults();

    public abstract JPanel getResultPanel();

    public abstract R getRecords();

    public abstract void executeQuery(RedisQueryExecutor redisQueryExecutor);
}
