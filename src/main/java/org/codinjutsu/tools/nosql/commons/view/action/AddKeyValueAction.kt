package org.codinjutsu.tools.nosql.commons.view.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


/**
 *
 * @author bruce ge
 */
class AddKeyValueAction : AnAction("Execute query", "Execute query with options", AllIcons.General.Add) {
    override fun actionPerformed(e: AnActionEvent?) {
        println("start add it");
    }


    override fun update(e: AnActionEvent?) {
        e!!.presentation.isVisible = true;
    }
}
