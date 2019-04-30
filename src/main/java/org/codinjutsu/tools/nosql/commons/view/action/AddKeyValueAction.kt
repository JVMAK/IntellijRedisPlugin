package org.codinjutsu.tools.nosql.commons.view.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.math.NumberUtils.toByte
import org.codehaus.groovy.runtime.StackTraceUtils
import org.codinjutsu.tools.nosql.dialog.AddKeyValueDialog
import org.codinjutsu.tools.nosql.redis.executors.AddKeyValueExecutor
import org.codinjutsu.tools.nosql.redis.logic.ExecuteResult
import org.codinjutsu.tools.nosql.redis.logic.RedisQueryExecutor
import org.codinjutsu.tools.nosql.redis.view.RedisPanel
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCommands


/**
 *
 * @author bruce ge
 */
class AddKeyValueAction(var redisPanel: RedisPanel) :
    AnAction("AddKeyValue", "Add key value", AllIcons.General.Add) {
    private var myRedispanel: RedisPanel;

    init {
        myRedispanel = redisPanel;
    }

    override fun actionPerformed(e: AnActionEvent?) {
        val addKeyValueDialog = AddKeyValueDialog(e!!.project)
        val showAndGet = addKeyValueDialog.showAndGet()
        if (showAndGet) {
            val keyValueResult = addKeyValueDialog.keyValueResult
            myRedispanel.executeQuery(AddKeyValueExecutor(keyValueResult));
        }

    }

    override fun update(e: AnActionEvent?) {
        e!!.presentation.isVisible = true;
    }
}
