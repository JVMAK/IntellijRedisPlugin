package org.codinjutsu.tools.nosql.commons.view.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.codinjutsu.tools.nosql.redis.view.RedisPanel


/**
 *
 * @author bruce ge
 */
class AddKeyValueAction(var redisPanel: RedisPanel) :
    AnAction("AddKeyValue", "Add key value", AllIcons.General.Add) {
    private lateinit var myRedispanel: RedisPanel;

    init {
        myRedispanel = redisPanel;
    }

    override fun actionPerformed(e: AnActionEvent?) {

    }


    override fun update(e: AnActionEvent?) {
        e!!.presentation.isVisible = true;
    }
}
