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
/* ********************
 *  VIEW-Specific data
 * ********************/

// cache the ListModels
projectListModels = [:]

// map of category -> list icon
categoryIcons = [:]

statusIcons = [:]

// filter for projects and issues
filter = new Filter(categories: [],
    status: [Status.NEW, Status.VALIDATION_REQUIRED])

popupProject = null
selectedProject = model.rootProject

popupIssue = null

// initialize category-related view data
Category.values().each {
    categoryIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
    filter.categories.add(it)
}

Status.values().each {
    statusIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
}

/* ***************
 *  event methods
 * ***************/

/** 
 * displayProject
 * @param project Project to display.
 * 
 */
displayProject = { project = null -> 
    issueTextArea.text = ""
    if (!project) return

    if (!projectListModels[(project.name)]) {
        def model = new DefaultListModel()
        project.eachIssue(filter) { model.addElement(it) }
        projectListModels[(project.name)] = model
    }

    issueList.setModel(projectListModels[(project.name)])
}

displayIssue = { issue = null ->
    if (issue) issueTextArea.text = issue.text
}

showProjectPopup = { project, x, y ->
    popupProject = project
    projectPopupMenu[1].enabled = project != null
    projectPopupMenu.show(projectTree, x, y)
}

showIssuePopup = { issue, x, y ->
    popupIssue = issue
    issuePopupMenu.eachWithIndex { menuItem, idx ->
        if (idx != 0) menuItem.enabled = issue != null }
    issuePopupMenu.show(issueList, x, y)
}

newIssue = { evt = null ->
    titleTextField.text = ""
    categoryComboBox.selectedItem = Category.BUG
    statusComboBox.selectedItem = Status.NEW
    prioritySpinner.setValue(5)
    newIssueDialog.visible = true
}

/* ****************
 *  GUI components
 * ****************/
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
    button('Create Issue',
        constraints: gbc(gridx: 1, gridy: 5, insets: [5, 5, 5, 5],
            anchor: GBC.WEST),
        actionPerformed: {
            def issue = selectedProject.createNewIssue(
                category: categoryComboBox.selectedItem,
                status: statusComboBox.selectedItem,
                priority: prioritySpinner.value,
                text: titleTextField.text)
            projectListModels[(selectedProject.name)] = null
            displayProject(selectedProject)
            newIssueDialog.visible = false
        })
}

projectPopupMenu = popupMenu() {
    menuItem('New Project...', icon: imageIcon("/add.png"),
        actionPerformed: {
            def name = JOptionPane.showInputDialog(frame, 'Project name:',
                'New Project...', JOptionPane.QUESTION_MESSAGE)

            if (!popupProject) popupProject = model.rootProject
            def newProject = popupProject.createNewProject(name)

            popupProject.projects[(newProject.name)] = newProject
            projectTree.model = new DefaultTreeModel(
                makeNodes(model.rootProject))
        })
    menuItem('Delete Project', icon: imageIcon("/delete.png"),
        actionPerformed: { 
            if (!popupProject) return
            popupProject.delete()
            // do not like, tied to Project implementation
            model.rootProject = new FileProject(model.rootProject.source)
        })
}

issuePopupMenu = popupMenu() {
    menuItem('New Issue...', icon: imageIcon("/add.png"),
        actionPerformed: newIssue)

    menuItem('Delete Issue', icon: imageIcon("/delete.png"),
        actionPerformed: {
            if (!popupIssue) return
            selectedProject.issues.remove(popupIssue.id)
            projectListModels[(selectedProject.name)].removeElement(popupIssue)
            popupIssue.delete()
        })

    separator()

    menu('Change Category') {
        Category.values().each { category ->
            menuItem(category.toString(),
                icon: categoryIcons[(category)],
                actionPerformed: {
                    if (!popupIssue) return
                    popupIssue.category = category
                    issueList.invalidate()
                    issueList.repaint()
                })
        }
    }

    menu('Change Status') {
        Status.values().each { status ->
            menuItem(status.toString(),
                icon: statusIcons[(status)],
                actionPerformed: {
                    if (!popupIssue) return
                    popupIssue.status = status
                    issueList.invalidate()
                    issueList.repaint()
                })
        }
    }

    menuItem('Change Priority...',
        actionPerformed: {
            if (!popupIssue) return
            def newPriority = JOptionPane.showInputDialog(frame,
                'New priority (0-9)', 'Change Priority...',
                JOptionPane.QUESTION_MESSAGE)
            try { popupIssue.priority = newPriority.toInteger() }
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
                Category.values().each {
                    checkBoxMenuItem(it.toString(),
                        selected: filter.categories.contains(it),
                        actionPerformed: { evt ->
                            def cat = Category.toCategory(evt.source.text)
                            if (filter.categories.contains(cat)) {
                                filter.categories.remove(cat)
                                evt.source.selected = false
                            } else {
                                filter.categories.add(cat)
                                evt.source.selected = true
                            }
                            projectListModels.clear()
                            displayProject(selectedProject)
                        })
                }
            }

            menu('Status') {
                Status.values().each {
                    checkBoxMenuItem(it.toString(),
                        selected: filter.status.contains(it),
                        actionPerformed: { evt ->
                            def st = Status.toStatus(evt.source.text[0..5])
                            if (filter.status.contains(st)) {
                                filter.status.remove(st)
                                evt.source.selected = false
                            } else {
                                filter.status.add(st)
                                evt.source.selected = true
                            }
                            projectListModels.clear()
                            displayProject(selectedProject)
                        })
                }
            }

        }

        menu('Sort') {
            sortMenuButtonGroup = buttonGroup()
            checkBoxMenuItem('By ID', 
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    filter.issueSorter = { it.id }
                    projectListModels.clear()
                    displayProject(selectedProject)
                })
            checkBoxMenuItem('By Category',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    filter.issueSorter = { it.category }
                    projectListModels.clear()
                    displayProject(selectedProject)
                })
            checkBoxMenuItem('By Status',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    filter.issueSorter = { it.status }
                    projectListModels.clear()
                    displayProject(selectedProject)
                })
            checkBoxMenuItem('By Priority',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    filter.issueSorter = { it.priority }
                    projectListModels.clear()
                    displayProject(selectedProject)
                })
            checkBoxMenuItem('By Title',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    filter.issueSorter = { it.title }
                    projectListModels.clear()
                    displayProject(selectedProject)
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
                                new DefaultTreeModel(makeNodes(model.rootProject))
                            } else {
                                projectTree.rootVisible = false
                                new DefaultTreeModel(new DefaultMutableTreeNode())
                            }
                        }),
                    valueChanged: { evt ->
                        selectedProject = evt?.newLeadSelectionPath?.
                            lastPathComponent?.userObject ?: model.rootProject
                        displayProject(selectedProject)
                        //deleteProjectButton.enabled = selectedProject != null
                    },
                    mouseClicked: { evt ->
                        if (evt.button == MouseEvent.BUTTON3) {
                            showProjectPopup(
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
            button('New Project', icon: imageIcon("/add.png"),
                constraints: gbc(fill: GBC.NONE, gridx: 0, gridy: 1,
                    anchor: GBC.WEST),
                actionPerformed: {
                    def name = JOptionPane.showInputDialog(frame,
                        'Project name:', 'New Project...',
                        JOptionPane.QUESTION_MESSAGE)

                    if (!selectedProject) selectedProject = model.rootProject
                    def newProject = selectedProject.createNewProject(name)

                    selectedProject.projects[(newProject.name)] = newProject
                    projectTree.model = new DefaultTreeModel(
                        makeNodes(model.rootProject))

                })
            deleteProjectButton = button('Delete Project',
                icon: imageIcon("/delete.png"),
                constraints: gbc(fill: GBC.NONE, gridx: 1, gridy: 1,
                    anchor: GBC.WEST),
                enabled: bind(source: projectTree, sourceEvent: 'valueChanged',
                    sourceValue: { projectTree?.lastSelectedPathComponent != null}),
                actionPerformed: {
                    if (!selectedProject) return
                    selectedProject.delete()
                    // do not like, tied to Project implementation
                    model.rootProject = new FileProject(
                        model.rootProject.source)
                })
        }

        // split between issue list and issue details
        splitPane(orientation: JSplitPane.VERTICAL_SPLIT,
            dividerLocation: 200, constraints: "right") {

            panel(constraints: "top") {
                gridBagLayout()

                scrollPane(constraints: gbc(fill: GBC.BOTH, weightx: 2,
                        weighty: 2, gridx: 0, gridy: 0, gridwidth: 3)) {

                    issueList = list(
                        cellRenderer: new IssueListCellRenderer(
                            categoryIcons: categoryIcons,
                            statusIcons: statusIcons),
                        selectionMode: ListSelectionModel.SINGLE_SELECTION,
                        valueChanged: { displayIssue(issueList.selectedValue) },
                        mouseClicked: { evt ->
                            if (evt.button == MouseEvent.BUTTON3) {
                                issueList.selectedIndex = issueList.locationToIndex(
                                    [evt.x, evt.y] as Point)

                                showIssuePopup(issueList.selectedValue,
                                    evt.x, evt.y)
                            }
                        })
                }

                wordWrapCheckBox = checkBox('Word wrap',
                    constraints: gbc(gridx: 0, gridy: 1, weightx: 2,
                        anchor: GBC.WEST),
                    selected: true)
                button('New Issue',
                    constraints: gbc(gridx: 1, gridy: 1, anchor: GBC.EAST),
                    icon: imageIcon("/add.png"), actionPerformed: newIssue)

                deleteIssueButton = button('Delete Issue',
                    constraints: gbc(gridx: 2, gridy: 1, anchor: GBC.EAST),
                    enabled: bind(source: issueList, sourceEvent: 'valueChanged',
                        sourceValue: { issueList.selectedValue != null }),
                    icon: imageIcon("/delete.png"),
                    actionPerformed: {
                        if (!issueList?.selectedIssue) return
                        selectedProject.issues.remove(issueList.selectedValue)
                        projectListModels[(selectedProject.name)]
                            .removeElement(issueList.selectedValue)
                        issueList.selectedIssue.delete()
                    })
                        
            }
            scrollPane(constraints: "bottom") {
                issueTextArea = textArea(
                    wrapStyleWord: true,
                    lineWrap: bind(source: wordWrapCheckBox,
                        sourceProperty: 'selected'),
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

/* ******************
 *  Auxilary methods
 * ******************/
def makeNodes(Project project) {
    def rootNode = new DefaultMutableTreeNode(project)
    project.eachProject(filter) { rootNode.add(makeNodes(it)) }
    return rootNode
}
