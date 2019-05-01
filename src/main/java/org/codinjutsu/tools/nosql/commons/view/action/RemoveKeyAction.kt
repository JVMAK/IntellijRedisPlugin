package org.codinjutsu.tools.nosql.commons.view.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.codinjutsu.tools.nosql.dialog.AddKeyValueDialog
import org.codinjutsu.tools.nosql.redis.executors.AddKeyValueExecutor
import org.codinjutsu.tools.nosql.redis.executors.RemoveKeyExecutor
import org.codinjutsu.tools.nosql.redis.view.RedisPanel
import org.codinjutsu.tools.nosql.redis.view.nodedescriptor.RedisKeyValueDescriptor
import java.lang.RuntimeException


/**
 *
 * @author bruce ge
 */
class RemoveKeyAction(var redisPanel: RedisPanel) :
    AnAction("Remove key", "Remove key value", AllIcons.General.Remove) {
    private var myRedispanel: RedisPanel;

    init {
        myRedispanel = redisPanel;
    }

    override fun actionPerformed(e: AnActionEvent?) {
        println("start to remove key?")
        val resultTableView = myRedispanel.resultTableView
        val deletedKeys = mutableSetOf<String>()
        val selectedRows = resultTableView.selectedRows
        for (selectedRow in selectedRows) {
            val valueAt = resultTableView.getValueAt(selectedRow, 1)
            if (valueAt is RedisKeyValueDescriptor) {
                if (valueAt.keyType != null) {
                    val key = valueAt.key
                    if (key.isNullOrEmpty()) {
                        throw RuntimeException("key is null or empty")
                    }
                    deletedKeys.add(key);
                }
            }
        }
        //show a dialog to user.
        myRedispanel.executeQuery(RemoveKeyExecutor(deletedKeys));
    }

    override fun update(e: AnActionEvent?) {
        val resultTableView = myRedispanel.resultTableView
        val selectedRows = resultTableView.selectedRows
        if (selectedRows.isNotEmpty()) {
            e!!.presentation.isEnabled = true;
        } else {
            e!!.presentation.isEnabled = false;
        }
    }
}
