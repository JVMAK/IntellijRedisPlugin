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

package org.codinjutsu.tools.nosql.redis.view;

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ui.tree.TreeUtil;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.nosql.ServerConfiguration;
import org.codinjutsu.tools.nosql.commons.utils.GuiUtils;
import org.codinjutsu.tools.nosql.commons.view.ErrorPanel;
import org.codinjutsu.tools.nosql.commons.view.NoSqlResultView;
import org.codinjutsu.tools.nosql.commons.view.action.ExecuteQuery;
import org.codinjutsu.tools.nosql.mongo.view.JsonTreeTableView;
import org.codinjutsu.tools.nosql.redis.logic.RedisClient;
import org.codinjutsu.tools.nosql.redis.model.RedisDatabase;
import org.codinjutsu.tools.nosql.redis.model.RedisQuery;
import org.codinjutsu.tools.nosql.redis.model.RedisResult;
import org.codinjutsu.tools.nosql.redis.view.action.EnableGroupingAction;
import org.codinjutsu.tools.nosql.redis.view.action.SetSeparatorAction;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class RedisPanel extends NoSqlResultView<RedisResult> {

    private JPanel toolBarPanel;
    private JPanel containerPanel;
    private JPanel errorPanel;
    private JPanel resultPanel;
    private JPanel mainPanel;
    private final LoadingDecorator loadingDecorator;


    private JsonTreeTableView resultTableView;

    private final Project project;
    private final RedisClient redisClient;
    private final ServerConfiguration configuration;
    private final RedisDatabase database;
    private JBTextField filterField;
    private RedisResult redisResult;
    private boolean groupData;
    private String groupSeparator;

    public RedisPanel(Project project, RedisClient redisClient, ServerConfiguration configuration, RedisDatabase database) {
        this.project = project;
        this.redisClient = redisClient;
        this.configuration = configuration;
        this.database = database;
        this.resultPanel = new JPanel(new BorderLayout());


        buildQueryToolBar();

        loadingDecorator = new LoadingDecorator(resultPanel, this, 0);

        containerPanel.add(loadingDecorator.getComponent());
        loadAndDisplayResults(getFilter(), this.groupData, this.groupSeparator);

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void loadAndDisplayResults(final String filter, final boolean groupByPrefix, final String separator) {
        redisResult = redisClient.loadRecords(configuration, database, new RedisQuery(filter));
        updateResultTableTree(redisResult, groupByPrefix, separator);
    }

    protected void buildQueryToolBar() {
        toolBarPanel.setLayout(new BorderLayout());

        filterField = new JBTextField("*");
        filterField.setColumns(10);

        NonOpaquePanel westPanel = new NonOpaquePanel();

        NonOpaquePanel filterPanel = new NonOpaquePanel();
        filterPanel.add(new JLabel("Filter: "), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        filterPanel.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
        westPanel.add(filterPanel, BorderLayout.WEST);

        toolBarPanel.add(westPanel, BorderLayout.WEST);

        addCommonsActions();
    }

    protected void addCommonsActions() {
        final TreeExpander treeExpander = new TreeExpander() {
            @Override
            public void expandAll() {
                RedisPanel.this.expandAll();
            }

            @Override
            public boolean canExpand() {
                return true;
            }

            @Override
            public void collapseAll() {
                RedisPanel.this.collapseAll();
            }

            @Override
            public boolean canCollapse() {
                return true;
            }
        };

        CommonActionsManager actionsManager = CommonActionsManager.getInstance();

        final AnAction expandAllAction = actionsManager.createExpandAllAction(treeExpander, resultPanel);
        final AnAction collapseAllAction = actionsManager.createCollapseAllAction(treeExpander, resultPanel);

        Disposer.register(this, new Disposable() {
            @Override
            public void dispose() {
                collapseAllAction.unregisterCustomShortcutSet(resultPanel);
                expandAllAction.unregisterCustomShortcutSet(resultPanel);
            }
        });

        DefaultActionGroup actionResultGroup = new DefaultActionGroup("RedisResultGroup", true);
        actionResultGroup.add(new ExecuteQuery<>(this));
        actionResultGroup.addSeparator();
        actionResultGroup.add(new EnableGroupingAction(this));
        actionResultGroup.add(new SetSeparatorAction(this));
        actionResultGroup.addSeparator();
        actionResultGroup.add(expandAllAction);
        actionResultGroup.add(collapseAllAction);

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("MongoResultGroupActions", actionResultGroup, true);
        actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);

        toolBarPanel.add(actionToolBarComponent, BorderLayout.CENTER);
    }

    private String getFilter() {
        String filter = filterField.getText();
        if (StringUtils.isNotBlank(filter)) {
            return filter;
        }
        return "*";
    }

    void expandAll() {
        TreeUtil.expandAll(resultTableView.getTree());
    }

    void collapseAll() {
        TreeTableTree tree = resultTableView.getTree();
        TreeUtil.collapseAll(tree, 1);
    }

    public void updateResultTableTree(RedisResult redisResult, boolean groupByPrefix, String separator) {
        DefaultMutableTreeNode rootNode = RedisTreeModel.buildTree(redisResult);
        DefaultMutableTreeNode renderedNode = rootNode;
        if (groupByPrefix && StringUtils.isNotBlank(separator)) {
            renderedNode = RedisFragmentedKeyTreeModel.wrapNodes(rootNode, separator);
        }
        resultTableView = new JsonTreeTableView(renderedNode, JsonTreeTableView.COLUMNS_FOR_READING);
        resultTableView.setName("resultTreeTable");

        resultPanel.invalidate();
        resultPanel.removeAll();
        resultPanel.add(new JBScrollPane(resultTableView));
        resultPanel.validate();
    }

    @Override
    public void showResults() {
        executeQuery();
    }

    @Override
    public JPanel getResultPanel() {
        return resultPanel;
    }

    @Override
    public RedisResult getRecords() {
        return redisResult;
    }

    @Override
    public void executeQuery() {
        errorPanel.setVisible(false);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Executing query", true)  {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    GuiUtils.runInSwingThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingDecorator.startLoading(false);
                        }
                    });


                    loadAndDisplayResults(getFilter(), isGroupDataEnabled(), getGroupSeparator());
                } catch (final Exception ex) {
                    GuiUtils.runInSwingThread(new Runnable() {
                        @Override
                        public void run() {
                            errorPanel.invalidate();
                            errorPanel.removeAll();
                            errorPanel.add(new ErrorPanel(ex), BorderLayout.CENTER);
                            errorPanel.validate();
                            errorPanel.setVisible(true);
                        }
                    });
                } finally {
                    GuiUtils.runInSwingThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingDecorator.stopLoading();
                        }
                    });
                }
            }
        });

    }

    @Override
    public void dispose() {

    }

    public boolean isGroupDataEnabled() {
        return this.groupData;
    }

    public void toggleGroupData(boolean enabled) {
        this.groupData = enabled;
        updateResultTableTree(redisResult, this.groupData, this.groupSeparator);
    }

    public String getGroupSeparator() {
        return groupSeparator;
    }

    public void setGroupSeparator(String groupSeparator) {
        this.groupSeparator = groupSeparator;
        updateResultTableTree(redisResult, this.groupData, this.groupSeparator);
    }
}
