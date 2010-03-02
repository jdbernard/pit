package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.FileProject
import groovy.beans.Bindable
import java.awt.BorderLayout as BL
import java.awt.GridBagConstraints as GBC
import java.awt.Font
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel
import net.miginfocom.swing.MigLayout

import java.awt.Color

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
        id: 'createIssue',
        name: 'Create Issue',
        closure: controller.createIssue
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
        enabled: bind {model.selectedProject != null }
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

/* ****************
 *  GUI components
 * ****************/
categoryIcons = [:]
statusIcons = [:]

// initialize category-related view data
Category.values().each {
    categoryIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
    model.filter.categories.add(it)
}

Status.values().each {
    statusIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
}


openDialog = fileChooser(fileSelectionMode: JFileChooser.DIRECTORIES_ONLY)

newIssueDialog = dialog(title: 'New Task...', modal: true, pack: true,
    locationRelativeTo: null) {

    gridBagLayout()

    label('Title/Summary:',
        constraints: gbc(gridx: 0, gridy: 0, gridwidth: 3,
            insets: [5, 5, 0, 5], fill: GBC.HORIZONTAL))
    titleTextField = textField(
        constraints: gbc(gridx: 0, gridy: 1, gridwidth: 3,
            insets: [0, 10, 0, 5], fill: GBC.HORIZONTAL))

    label('Category:', 
        constraints: gbc(gridx: 0, gridy: 2, insets: [5, 5, 0, 0],
            fill: GBC.HORIZONTAL))
    categoryComboBox = comboBox(
        constraints: gbc(gridx: 1, gridy: 2, insets: [5, 5, 0, 5],
            fill: GBC.HORIZONTAL),
        model: new DefaultComboBoxModel(Category.values()))

    label('Status:',
        constraints: gbc(gridx: 0, gridy: 3, insets: [5, 5, 0, 0],
            fill: GBC.HORIZONTAL))
    statusComboBox = comboBox(
        constraints: gbc(gridx: 1, gridy: 3, insets: [5, 5, 0, 5],
            fill: GBC.HORIZONTAL),
        model: new DefaultComboBoxModel(Status.values()))

    label('Priority (0-9, 0 is highest priority):',
        constraints: gbc(gridx: 0, gridy: 4, insets: [5, 5, 0, 0],
            fill: GBC.HORIZONTAL))
    prioritySpinner = spinner(
        constraints: gbc( gridx: 1, gridy: 4, insets: [5, 5, 0, 5],
            fill: GBC.HORIZONTAL),
        model: spinnerNumberModel(maximum: 9, minimum: 0))

    button('Cancel', actionPerformed: { newIssueDialog.visible = false },
        constraints: gbc(gridx: 0, gridy: 5, insets: [5, 5, 5, 5],
            anchor: GBC.EAST))
    button(createIssue,
        constraints: gbc(gridx: 1, gridy: 5, insets: [5, 5, 5, 5],
            anchor: GBC.WEST))
}

projectPopupMenu = popupMenu() {
    menuItem(newProject)
    menuItem(deleteProjectPop)
}

issuePopupMenu = popupMenu() {
    menuItem(newIssue)
    menuItem(deleteIssuePop)
    separator()

    menu('Change Category') {
        Category.values().each { category ->
            menuItem(category.toString(),
                icon: categoryIcons[(category)],
                enabled: bind { model.popupIssue != null },
                actionPerformed: {
                    model.popupIssue.category = category
                    issueList.invalidate()
                    issueList.repaint()
                })
        }
    }

    menu('Change Status') {
        Status.values().each { status ->
            menuItem(status.toString(),
                icon: statusIcons[(status)],
                enabled: bind { model.popupIssue != null },
                actionPerformed: {
                    model.popupIssue.status = status
                    issueList.invalidate()
                    issueList.repaint()
                })
        }
    }

    menuItem('Change Priority...',
        enabled: bind { model.popupIssue != null },
        actionPerformed: {
            def newPriority = JOptionPane.showInputDialog(frame,
                'New priority (0-9)', 'Change Priority...',
                JOptionPane.QUESTION_MESSAGE)
            try { model.popupIssue.priority = newPriority.toInteger() }
            catch (exception) {
                JOptionPane.showMessage(frame, 'The priority value must ' +
                    'be an integer in [0-9].', 'Change Priority...',
                    JOptionPane.ERROR_MESSAGE)
                return
            }
            issueList.invalidate()
            issueList.repaint()
        })
}

frame = application(title:'Personal Issue Tracker',
  minimumSize: [800, 500],
  pack:true,
  locationRelativeTo: null,
  iconImage: imageIcon('/icon64x64.png').image,
  iconImages: [imageIcon('/icon64x64.png').image,
               imageIcon('/icon32x32.png').image,
               imageIcon('/icon16x16.png').image]
) {

    // main menu
    menuBar() {
        menu("File") {
            menuItem('Open...', actionPerformed: {
                def projectDir
                if (openDialog.showOpenDialog(frame) !=
                    JFileChooser.APPROVE_OPTION) return

                projectDir = openDialog.selectedFile
                model.rootProject = new FileProject(projectDir)
            })

            menuItem('Exit', actionPerformed: { app.shutdown() })
        }

        menu('View') {
            menu('Category') {
                Category.values().each { cat ->
                    checkBoxMenuItem(cat.toString(),
                        selected: model.filter.categories.contains(cat),
                        actionPerformed: { evt ->
                            if (model.filter.categories.contains(cat)) {
                                model.filter.categories.remove(cat)
                                evt.source.selected = false
                            } else {
                                model.filter.categories.add(cat)
                                evt.source.selected = true
                            }
                            model.projectListModels.clear()
                            controller.displayProject(model.selectedProject)
                        })
                }
            }

            menu('Status') {
                Status.values().each { st ->
                    checkBoxMenuItem(st.toString(),
                        selected: model.filter.status.contains(st),
                        actionPerformed: { evt ->
                            if (model.filter.status.contains(st)) {
                                model.filter.status.remove(st)
                                evt.source.selected = false
                            } else {
                                model.filter.status.add(st)
                                evt.source.selected = true
                            }
                            model.projectListModels.clear()
                            controller.displayProject(model.selectedProject)
                        })
                }
            }

        }

        menu('Sort') {
            sortMenuButtonGroup = buttonGroup()
            checkBoxMenuItem('By ID', 
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.id }
                    model.projectListModels.clear()
                    controller.displayProject(selectedProject)
                })
            checkBoxMenuItem('By Category',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.category }
                    model.projectListModels.clear()
                    controller.displayProject(selectedProject)
                })
            checkBoxMenuItem('By Status',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.status }
                    model.projectListModels.clear()
                    controller.displayProject(selectedProject)
                })
            checkBoxMenuItem('By Priority',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.priority }
                    model.projectListModels.clear()
                    controller.displayProject(selectedProject)
                })
            checkBoxMenuItem('By Title',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.title }
                    model.projectListModels.clear()
                    controller.displayProject(selectedProject)
                })
        }
    }

    gridBagLayout()

    // main split view
    splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT,
        dividerLocation: 280,
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
                projectTree.model = new DefaultTreeModel(
                    new DefaultMutableTreeNode())
                projectTree.rootVisible = false
        
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
                        cellRenderer: bind(source: model,
                            sourceProperty: 'issueListRenderer'),
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
                    font: new Font(Font.MONOSPACED, Font.PLAIN, 10),
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
}
