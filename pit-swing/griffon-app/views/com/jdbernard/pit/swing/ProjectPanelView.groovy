package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import com.jdbernard.pit.Project
import java.awt.Font
import java.awt.GridBagConstraints as GBC
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
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

// popup menu for isses
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
                    model.popupIssue.category = category
                    controller.refreshIssues()
                })
        }
    }

    menu('Change Status', enabled: bind { model.popupIssue != null }) {
        Status.values().each { status ->
            menuItem(status.toString(),
                icon: model.mainMVC.model.statusIcons[(status)],
                enabled: bind { model.popupIssue != null },
                actionPerformed: {
                    model.popupIssue.status = status
                    controller.refreshIssues()
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
            catch (exception) {
                JOptionPane.showMessageDialog(mainMVC.view.frame,
                    'The priority value must be an integer in [0-9].',
                    'Change Priority...', JOptionPane.ERROR_MESSAGE)
                return
            }
            controller.refreshIssues()
        })
}

// main split view
panel = splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT,
    // dividerLocation: bind(source: model.mainModel, property: dividerLocation),
    oneTouchExpandable: true,
    constraints: gbc(fill: GBC.BOTH, insets: [10,10,10,10],
        weightx: 2, weighty: 2)) {

    // left side (projects tree and buttons)
    panel(constraints: "left") {
        gridBagLayout()

        // tree view of projects
        scrollPane(constraints: gbc(fill: GBC.BOTH, gridx: 0, gridy:0,
            gridwidth: 2, weightx: 2, weighty: 2)) {
            treeCellRenderer = new DefaultTreeCellRenderer()
            treeCellRenderer.leafIcon = treeCellRenderer.closedIcon

            projectTree = tree(cellRenderer: treeCellRenderer,
                model: bind(source: model, sourceProperty: 'rootProject',
                    sourceValue: { 
                        if (model.rootProject) {
                            projectTree.rootVisible =
                                model.rootProject.issues.size()
                            new DefaultTreeModel(controller.makeNodes(
                                model.rootProject))
                        } else {
                            projectTree.rootVisible = false
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
                            projectTree.getPathForLocation(evt.x, evt.y)
                                ?.lastPathComponent?.userObject,
                            evt.x, evt.y)
                    }
                })
    
            projectTree.selectionModel.selectionMode =
                TreeSelectionModel.SINGLE_TREE_SELECTION
        }

        // project buttons
        newProjectButton = button(newProject,
            constraints: gbc(fill: GBC.NONE, gridx: 0, gridy: 1,
                anchor: GBC.WEST))
        deleteProjectButton = button(deleteProject,
            constraints: gbc(fill: GBC.NONE, gridx: 1, gridy: 1,
                anchor: GBC.WEST))
    }

    // split between issue list and issue details
    splitPane(orientation: JSplitPane.VERTICAL_SPLIT,
        dividerLocation: 200, constraints: "right") {

        panel(constraints: "top") {
            gridBagLayout()

            scrollPane(constraints: gbc(fill: GBC.BOTH, weightx: 2,
                    weighty: 2, gridx: 0, gridy: 0, gridwidth: 3)) {

                issueList = list(
                    cellRenderer: model.issueCellRenderer,
                    selectionMode: ListSelectionModel.SINGLE_SELECTION,
                    valueChanged: { evt ->
                        controller.displayIssue(issueList.selectedValue)
                    },
                    mouseClicked: { evt ->
                        if (evt.button == MouseEvent.BUTTON3) {
                            issueList.selectedIndex = issueList.locationToIndex(
                                [evt.x, evt.y] as Point)

                            controller.showIssuePopup(
                                issueList.selectedValue, evt.x, evt.y)
                        }
                    })
            }

            wordWrapCheckBox = checkBox('Word wrap',
                constraints: gbc(gridx: 0, gridy: 1, weightx: 2,
                    anchor: GBC.WEST), selected: true)
            button(newIssue,
                constraints: gbc(gridx: 1, gridy: 1, anchor: GBC.EAST))

            deleteIssueButton = button(deleteIssue,
                constraints: gbc(gridx: 2, gridy: 1, anchor: GBC.EAST),
                enabled: bind(source: issueList, sourceEvent: 'valueChanged',
                    sourceValue: { issueList.selectedValue != null }))
                    
        }

        scrollPane(constraints: "bottom") {
            issueTextArea = textArea(
                wrapStyleWord: true,
                lineWrap: bind(source: wordWrapCheckBox,
                    sourceProperty: 'selected'),
                editable: bind( source: issueList, sourceEvent: 'valueChanged',
                    sourceValue: { issueList.selectedValue != null }),
                font: model.mainMVC.model.issueDetailFont,
                focusGained: {},
                focusLost: {
                    if (!issueList?.selectedValue) return
                    if (issueTextArea.text != issueList.selectedValue.text)
                        issueList.selectedValue.text = issueTextArea.text
                },
                mouseExited: {
                    if (!issueList?.selectedValue) return
                    if (issueTextArea.text != issueList.selectedValue.text)
                        issueList.selectedValue.text = issueTextArea.text
                })
        }
    }
}
