package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import com.jdbernard.pit.Project
import com.jdbernard.pit.FlatProjectView
import java.awt.Font
import java.awt.GridBagConstraints as GBC
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

actions {
    action (
        id: 'newIssue',
        name: 'New Issue',
        icon: imageIcon("/add.png"),
        accelerator: shortcut('N'),
        closure: controller.newIssue,
        enabled: bind { model.selectedProject != null }
    )

    action (
        id: 'newProject',
        name: 'New Project...',
        icon: imageIcon("/add.png"),
        closure: controller.newProject
    )

    action (
        id: 'deleteProject',
        name: 'Delete Project',
        closure: controller.deleteProject,
        enabled: bind { model.selectedProject != null }
    )

    action (
        id: 'deleteProjectPop',
        name: 'Delete Project',
        icon: imageIcon("/delete.png"),
        closure: controller.deleteProject,
        enabled: bind { model.popupProject != null }
    )

    action (
        id: 'deleteIssue',
        name: 'Delete Issue',
        icon: imageIcon("/delete.png"),
        closure: controller.deleteIssue,
    )

    action (
        id: 'deleteIssuePop',
        name: 'Delete Issue',
        icon: imageIcon("/delete.png"),
        closure: controller.deleteIssue,
        enabled: bind { model.popupIssue != null }
    )
}

// popup menu for projects
projectPopupMenu = popupMenu() {
    menuItem(newProject)
    menuItem(deleteProjectPop)
}

// popup menu for issues
issuePopupMenu = popupMenu() {
    menuItem(newIssue)
    menuItem(deleteIssuePop)
    separator()

    menu('Change Category', enabled: bind { model.popupIssue != null }) {
        Category.values().each { category ->
            menuItem(category.toString(),
                icon: model.mainMVC.model.categoryIcons[(category)],
                enabled: bind { model.popupIssue != null },
                actionPerformed: {
                    try {
                        model.popupIssue.category = category
                        controller.refreshIssues()
                    } catch (IOException ioe) {
                        JOptionPane.showMessage(mainMVC.view.frame,
                            ioe.getLocalizedMessage(), "Change Category",
                            JOptionPane.ERROR_MESSAGE)
                    }
                })
        }
    }

    menu('Change Status', enabled: bind { model.popupIssue != null}) {
        Status.values().each { status ->
            menuItem(status.toString(),
                icon: model.mainMVC.model.statusIcons[(status)],
                enabled: bind { model.popupIssue != null },
                actionPerformed: {
                    try {
                        model.popupIssue.status = status
                        controller.refreshIssues()
                    } catch (IOException ioe) {
                        JOptionPane.showMessage(model.mainMVC.view.frame,
                            ioe.getLocalizedMessage(), 'Change Status',
                            JOptionPane.ERROR_MESSAGE)
                    }
                })
        }
    }

    menuItem('Change Priority...',
        enabled: bind { model.popupIssue != null },
        actionPerformed: {
            def newPriority = JOptionPane.showInputDialog(mainMVC.view.frame,
                'New priority (0-9)', 'Change Priority...',
                JOptionPane.QUESTION_MESSAGE)
            try { model.popupIssue.priority = newPriority.toInteger() }
            catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(mainMVC.view.frame,
                    'The priority value must be an integer in [0-9].',
                    'Change Priority...', JOptionPane.ERROR_MESSAGE)
                return
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(model.mainMVC.view.frame,
                    ioe.getLocalizedMessage(), 'Change Priority...',
                    JOptionPane.ERROR_MESSAGE)
            }
            controller.refreshIssues()
        })
}

// main split view
panel = splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT,
    dividerLocation: 200,
    oneTouchExpandable: true,
    constraints: gbc(fill: GBC.BOTH, insets: [10, 10, 10, 10],
        weightx: 2, weighty: 2)) {

    // left side (project tree and buttons
    panel(constraints: 'left') {
        gridBagLayout()

        // tree view of projects
        scrollPane(constraints: gbc(fill: GBC.BOTH, gridx: 0, gridy: 0,
            gridwidth: 2, weightx: 2, weighty: 2)) {
            treeCellRenderer = new DefaultTreeCellRenderer()
            treeCellRenderer.leafIcon = treeCellRenderer.closedIcon

            projectTree = tree(cellRenderer: treeCellRenderer,
                model: bind(source: model, sourceProperty: 'rootProject',
                    sourceValue: {
                        if (model.rootProject) {
                            def rootNode = new DefaultMutableTreeNode()
                            def flatview = new FlatProjectView('All Issues')
                            flatview.projects[(model.rootProject.name)] =
                                model.rootProject
                            rootNode.add(new DefaultMutableTreeNode(flatview))
                            rootNode.add(controller.makeNodes(model.rootProject))
                            new DefaultTreeModel(rootNode)
                        } else {
                            new DefaultTreeModel(new DefaultMutableTreeNode())
                        }
                    }),
                valueChanged: { evt ->
                    model.selectedProject = evt?.newLeadSelectionPath?.
                        lastPathComponent?.userObject ?: model.rootProject
                    controller.displayProject(model.selectedProject)
                },
                mouseClicked: { evt ->
                    if (evt.button == MouseEvent.BUTTON3) {
                        controller.showProjectPopup(
                            projectTree.getPathForLocation(evt.x, evt.y)?.
                                lastPathComponent?.userObject,
                            evt.x, evt.y)
                    }
                })
            projectTree.rootVisible = false

            projectTree.selectionModel.selectionMode =
                TreeSelectionModel.SINGLE_TREE_SELECTION
        }

        newProjectButton = button(newProject,
            constraints: gbc(fill: GBC.NONE, gridx: 0, gridy: 1,
                anchor: GBC.WEST))
        deleteProjectButton = button(deleteProject,
            constraints: gbc(fill: GBC.NONE, gridx: 1, gridy: 1,
                anchor: GBC.WEST))
    }

    // split between issues list and issue details
    splitPane(orientation: JSplitPane.VERTICAL_SPLIT,
        dividerLocation: 200, constraints: "right") {

        panel(constraints: "top") {
            gridBagLayout()

            scrollPane(constraints: gbc(fill: GBC.BOTH, weightx: 2,
                weighty: 2, gridx: 0, gridy: 0, gridwidth: 3)) {

                issueTable = table(
                    autoCreateRowSorter: true,
                    autoResizeMode: JTable.AUTO_RESIZE_LAST_COLUMN,
                    cellSelectionEnabled: false,
                    columnSelectionAllowed: false,
                    dragEnabled: false,
                    rowSelectionAllowed: true,
                    showHorizontalLines: false,
                    showVerticalLines: false,
                    mouseClicked: { evt ->
                        if (evt.button == MouseEvent.BUTTON3) {
                            def translatedPoint = evt.locationOnScreen.clone()
                            translatedPoint.translate(-issueTable.locationOnScreen.@x,
                                -issueTable.locationOnScreen.@y)
                            def row = issueTable.rowAtPoint(translatedPoint)
                            
                            issueTable.setRowSelectionInterval(row, row)

                            controller.showIssuePopup(
                                controller.getSelectedIssue(), evt.x, evt.y)
                        }
                    })

                issueTable.setDefaultRenderer(Object.class,
                    model.issueCellRenderer)
                issueTable.selectionModel.valueChanged = { evt ->
                    if (evt.valueIsAdjusting) return
                    controller.displayIssue(controller.getSelectedIssue())
                }

            }

            wordWrapCheckBox = checkBox('Word wrap',
                constraints: gbc(gridx: 0, gridy: 1, weightx: 2,
                    anchor: GBC.WEST), selected: true)
            button(newIssue,
                constraints: gbc(gridx: 1, gridy: 1, anchor: GBC.EAST))

            deleteIssueButton = button(deleteIssue,
                constraints: gbc(gridx: 2, gridy: 1, anchor: GBC.EAST),
                enabled: bind(source: issueTable.selectionModel,
                    sourceEvent: 'valueChanged',
                    sourceValue: { !issueTable.selectionModel.isSelectionEmpty() }))
        }

        scrollPane(constraints: "bottom",
            horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            issueTextPanel = panel {
                issueTextPanelLayout = cardLayout()

                def leavingEditorClosure = {
                    def issue = controller.getSelectedIssue()
                    if (issue == null) return
                    if (issueTextArea.text != issue.text) {
                        issue.text = issueTextArea.text
                        issueTextDisplay.text = controller.rst2html(
                            issueTextArea.text)
                    }
                    issueTextPanelLayout.show(issueTextPanel, 'display')
                }

                issueTextArea = textArea(
                    constraints: 'editor',
                    wrapStyleWord: true,
                    lineWrap: bind(source: wordWrapCheckBox,
                        sourceProperty: 'selected'),
                    editable: bind(source: issueTable.selectionModel,
                        sourceEvent: 'valueChanged',
                        sourceValue:
                            { !issueTable.selectionModel.isSelectionEmpty() }),
                    font: model.mainMVC.model.issueDetailFont,
                    focusGained: {},
                    focusLost: leavingEditorClosure,
                    mouseExited: leavingEditorClosure)

                issueTextDisplay = editorPane(contentType: 'text/html',
                    constraints: 'display',
                    editable: false,
                    preferredSize: [10, 10],
                    mouseClicked: {evt ->
                        if (evt.clickCount > 1)
                            issueTextPanelLayout.show(issueTextPanel, 'editor')
                    })
            }

            issueTextPanelLayout.show(issueTextPanel, "display")
        }
    }
}
