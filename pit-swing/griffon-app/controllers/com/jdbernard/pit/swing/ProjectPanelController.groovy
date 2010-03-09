package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.FileProject
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.Status
import javax.swing.DefaultListModel
import javax.swing.JOptionPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ProjectPanelController {
    // these will be injected by Griffon
    def model
    def view

    void mvcGroupInit(Map args) {
        view.projectTree.model = new DefaultTreeModel(makeNodes(model.rootProject))
    }

    /** 
     * displayProject
     * @param project Project to display.
     * 
     */
    void displayProject(Project project) { 
        view.issueTextArea.text = ""
        if (!project) return

        if (!model.projectListModels[(project.name)]) {
            def dlm = new DefaultListModel()
            project.eachIssue(model.filter ?: model.mainMVC.model.filter)
                { dlm.addElement(it) }
            model.projectListModels[(project.name)] = dlm
        }

        view.issueList.setModel(model.projectListModels[(project.name)])
    }

    void displayIssue(Issue issue) {
        if (!issue) return
        view.issueTextArea.text = issue.text
        view.issueTextArea.caretPosition = 0
    }

    void showProjectPopup(Project project, def x, def y) {
        model.popupProject = project
        view.projectPopupMenu.show(view.projectTree, x, y)
    }

    void showIssuePopup(Issue issue, def x, def y) {
        model.popupIssue = issue
        view.issuePopupMenu.show(view.issueList, x, y)
    }


    def makeNodes(Project project) {
        def rootNode = new DefaultMutableTreeNode(project)
        project.eachProject(model.filter ?: model.mainMVC.model.filter)
            { rootNode.add(makeNodes(it)) }
        return rootNode
    }

    def newProject = { evt ->
        def name = JOptionPane.showInputDialog(model.mainMVC.view.frame,
            'Project name:', 'New Project...', JOptionPane.QUESTION_MESSAGE)

        def project

        if (evt.source == view.newProjectButton) 
            project = model.selectedProject ?: model.rootProject
        else project = model.popupProject ?: model.rootProject
        def newProject = project.createNewProject(name)

        project.projects[(newProject.name)] = newProject
        view.projectTree.model = new DefaultTreeModel(
            makeNodes(model.rootProject))
    }

    def deleteProject = { evt ->
        def project

        if (evt.source == view.deleteProjectButton)
            project = model.selectedProject ?: model.rootProject
        else project = model.popupProject ?: model.rootModel

        project.delete()

        model.rootProject = new FileProject(model.rootProject.source)
    }

    def newIssue = { evt = null ->
        newIssueDialogMVC.controller.show()
        if (newIssueDialogMVC.model.accept) {
            def nidmodel = newIssueDialodMVC.model
            def issueText = ""

            if (model.templates[(nidModel.category)]) {
                issueText = model.templates[(nidModel.category)]
                issueText = issueText.replaceFirst(/TITLE/,
                    nidModel.text)
            }

            def issue = model.selectedProject.createNewIssue(
                category: nidModel.category,
                status: nidModel.status,
                priority: nidModel.priority,
                text: issueText)
            model.projectListModels[(model.selectedProject.name)] = null
            displayProject(model.selectedProject)
        }
    }

    def deleteIssue = { evt ->
        def issue
        if (evt.source == view.deleteIssueButton)
            issue = view.issueList.selectedValue
        else issue = model.popupIssue

        model.selectedProject.issues.remove(issue.id)
        model.projectListModels[(model.selectedProject.name)]
            .removeElement(issue)

        issue.delete()
    }

    def changeCategory = { evt ->
        model.popupIssue.status = status
        view.issueList.invalidate()
        view.issueList.repaint()
    }

}
