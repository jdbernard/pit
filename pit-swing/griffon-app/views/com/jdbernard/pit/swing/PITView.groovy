package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.FileProject
import groovy.beans.Bindable
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel
import net.miginfocom.swing.MigLayout

/* ********************
 *  VIEW-Specific data
 * ********************/

// cache the ListModels
projectListModels = [:]

// map of category -> list icon
categoryIcons = [:]

// filter for projects and issues
filter = new Filter(categories: [])

popupProject = null

popupIssue = null

// initialize category-related view data
Category.values().each {
    categoryIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
    filter.categories.add(it)
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

/* ****************
 *  GUI components
 * ****************/
openDialog = fileChooser(fileSelectionMode: JFileChooser.DIRECTORIES_ONLY)

//newIssueDialog = dialog()

projectPopupMenu = popupMenu() {
    menuItem('New Project...',
        actionPerformed: {
            def name = JOptionPane.showInputDialog(frame, 'Project name:',
                'New Project...', JOptionPane.QUESTION_MESSAGE)

            if (!popupProject) popupProject = model.rootProject
            def newProject = popupProject.createNewProject(name)

            popupProject.projects[(newProject.name)] = newProject
            projectTree.model = new DefaultTreeModel(
                makeNodes(model.rootProject))
        })
    menuItem('Delete Project',
        actionPerformed: { 
            if (!popupProject) return
            popupProject.delete()
            // do not like, tied to Project implementation
            model.rootProject = new FileProject(model.rootProject.source)
        })
}

issuePopupMenu = popupMenu() {
    menuItem('New Issue...',
        actionPerformed: { })

    menuItem('Delete Issue',
        actionPerformed: {
            if (!popupIssue) return
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
        })
}

frame = application(title:'Personal Issue Tracker',
  locationRelativeTo: null,
  minimumSize: [600, 400],
  //size:[320,480],
  pack:true,
  //location:[50,50],
  locationByPlatform:true,
  iconImage: imageIcon('/griffon-icon-48x48.png').image,
  iconImages: [imageIcon('/griffon-icon-48x48.png').image,
               imageIcon('/griffon-icon-32x32.png').image,
               imageIcon('/griffon-icon-16x16.png').image]
) {

    borderLayout()

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
                        displayProject(projectTree.leadSelectionPath
                            ?.lastPathComponent?.userObject)
                    })
            }

        }
    }

    // main split view
    splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT,
        dividerLocation: 200) {

        // tree view of projects
        scrollPane(constraints: "left") {
            treeCellRenderer = new DefaultTreeCellRenderer()
            treeCellRenderer.leafIcon = treeCellRenderer.closedIcon

            projectTree = tree(cellRenderer: treeCellRenderer,
                model: bind(source: model, sourceProperty: 'rootProject',
                    sourceValue: { 
                        if (model.rootProject) {
                            projectTree.rootVisible = model.rootProject.issues.size()
                            new DefaultTreeModel(makeNodes(model.rootProject))
                        } else new DefaultTreeModel()
                    }),
                valueChanged: { evt ->
                    displayProject(evt?.newLeadSelectionPath
                        ?.lastPathComponent?.userObject)
                },
                mouseClicked: { evt ->
                    if (evt.button == MouseEvent.BUTTON3) {
                        showProjectPopup(
                            projectTree.getPathForLocation(evt.x, evt.y)
                                ?.lastPathComponent?.userObject,
                            evt.x, evt.y)
                    }
                })
    
            projectTree.selectionModel.selectionMode =
                TreeSelectionModel.SINGLE_TREE_SELECTION
        }

        // split between issue list and issue details
        splitPane(orientation: JSplitPane.VERTICAL_SPLIT,
            dividerLocation: 200, constraints: "") {

            scrollPane(constraints: "top") {
                issueList = list(
                    cellRenderer: new IssueListCellRenderer(
                        issueIcons: categoryIcons),
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
            scrollPane(constraints: "bottom") {
                issueTextArea = textArea()
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
