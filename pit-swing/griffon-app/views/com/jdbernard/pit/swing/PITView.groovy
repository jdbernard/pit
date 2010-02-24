package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.FileProject
import javax.swing.DefaultListModel
import javax.swing.JFileChooser
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel
import net.miginfocom.swing.MigLayout

// VIEW-Specific data
projectListModels = [:]

categoryIcons = [(Category.BUG):        imageIcon('/bug.png'),
                 (Category.CLOSED):     imageIcon('/closed.png'),
                 (Category.FEATURE):    imageIcon('/feature.png'),
                 (Category.TASK):       imageIcon('/task.png')]

openDialog = fileChooser(fileSelectionMode: JFileChooser.DIRECTORIES_ONLY)

// event methods
displayProject = { evt = null -> 
    def project= evt?.newLeadSelectionPath?.lastPathComponent?.userObject
    issueTextArea.text = ""
    if (!project) return

    if (!projectListModels[(project.name)]) {
        def model = new DefaultListModel()
        project.eachIssue { model.addElement(it) }
        projectListModels[(project.name)] = model
    }

    issueList.setModel(projectListModels[(project.name)])
}

displayIssue = { evt = null ->
    if (issueList.selectedValue)
        issueTextArea.text = issueList.selectedValue.text
}

frame = application(title:'Personal Issue Tracker',
  locationRelativeTo: null,
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
                valueChanged: displayProject)
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
                    valueChanged: displayIssue)
            }
            scrollPane(constraints: "bottom") {
                issueTextArea = textArea()
            }
        }
    }
}

def makeNodes(Project project) {
    def rootNode = new DefaultMutableTreeNode(project)
    project.eachProject { rootNode.add(makeNodes(it)) }
    return rootNode
}
