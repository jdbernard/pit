package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.FileProject
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.Status
import javax.swing.DefaultListModel
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class PITController {

    // these will be injected by Griffon
    def model
    def view

    void mvcGroupInit(Map args) {

        SwingUtilities.invokeAndWait {
            model.issueListRenderer = new IssueListCellRenderer()
            model.issueListRenderer.categoryIcons = view.categoryIcons
            model.issueListRenderer.statusIcons = view.statusIcons

            def config = new File(System.getProperty('user.home'), '.pit')
            config = new File(config, 'pit_swing.groovy')

            if (config.exists() && config.isFile()) {
                def loader = new GroovyClassLoader(PITController.classLoader)
                def configBinding = new Binding()

                // add default values
                configBinding.templates = model.templates
                configBinding.issueListRenderer = model.issueListRenderer

                def configScript = loader.parseClass(config)
                    .newInstance(configBinding)

                configScript.invokeMethod("run", null)

                model.templates = configBinding.templates ?: [:]
                if (configBinding.issueListRenderer &&
                    configBinding.issueListRenderer != model.issueListRenderer)
                    model.issueListRenderer = configBinding.issueListRenderer
            }
        }

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
            project.eachIssue(model.filter) { dlm.addElement(it) }
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
        project.eachProject(model.filter) { rootNode.add(makeNodes(it)) }
        return rootNode
    }

    def newProject = { evt ->
        def name = JOptionPane.showInputDialog(view.frame, 'Project name:',
            'New Project...', JOptionPane.QUESTION_MESSAGE)

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
        view.titleTextField.text = ""
        view.categoryComboBox.selectedItem = Category.BUG
        view.statusComboBox.selectedItem = Status.NEW
        view.prioritySpinner.setValue(5)
        view.newIssueDialog.visible = true
    }

    def createIssue = { evt = null ->
        def issueText = ""

        if (model.templates[(view.categoryComboBox.selectedItem)]) {
            issueText = model.templates[(view.categoryComboBox.selectedItem)]
            issueText = issueText.replaceFirst(/TITLE/,
                view.titleTextField.text)
        }

        def issue = model.selectedProject.createNewIssue(
            category: view.categoryComboBox.selectedItem,
            status: view.statusComboBox.selectedItem,
            priority: view.prioritySpinner.value,
            text: issueText)
        model.projectListModels[(model.selectedProject.name)] = null
        displayProject(model.selectedProject)
        view.newIssueDialog.visible = false
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
