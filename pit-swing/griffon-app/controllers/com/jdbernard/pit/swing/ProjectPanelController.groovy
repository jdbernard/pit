package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.FileProject
import com.jdbernard.pit.FlatProjectView
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
        refreshProject()
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

        // hack because binding view.issueTextArea.font to
        // mainMVC.mode.issueDetailFont causes problems
        if (view.issueTextArea.font != model.mainMVC.model.issueDetailFont)
            view.issueTextArea.font  = model.mainMVC.model.issueDetailFont

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

    void refreshProject() {
        if (model.rootProject) {
            def rootNode = new DefaultMutableTreeNode()
            def flatview = new FlatProjectView('All Issues')
            flatview.projects[(model.rootProject.name)] = model.rootProject
            rootNode.add(new DefaultMutableTreeNode(flatview))
            rootNode.add(makeNodes(model.rootProject))
            view.projectTree.model = new DefaultTreeModel(rootNode)
        } else {
            view.projectTree.model = new DefaultTreeModel(
                new DefaultMutableTreeNode())
        }
    }

    void refreshIssues() {
        model.projectListModels.clear()
        displayProject(model.selectedProject)
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
        refreshProject()
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
        model.newIssueDialogMVC.controller.show()
        if (model.newIssueDialogMVC.model.accept) {
            def nidModel = model.newIssueDialogMVC.model
            def issueText = nidModel.text

            if (model.mainMVC.model.templates[(nidModel.category)]) {
                issueText = model.mainMVC.model.templates[(nidModel.category)]
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
