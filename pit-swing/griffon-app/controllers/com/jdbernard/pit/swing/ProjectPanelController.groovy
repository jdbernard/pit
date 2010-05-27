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
import org.dom4j.Document
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.nuiton.jrst.JRSTGenerator
import org.nuiton.jrst.JRSTReader

class ProjectPanelController {
    // these will be injected by Griffon
    def model
    def view

    def jrstReader
    def jrstGen

    static URL rst2htmlXSL =
        ProjectPanelController.class.getResource("/rst2xhtml.xsl")

    void mvcGroupInit(Map args) {
        jrstReader = new JRSTReader()
        jrstGen = new JRSTGenerator()

        refreshProject()
    }

    /** 
     * displayProject
     * @param project Project to display.
     * 
     */
    void displayProject(Project project) { 
        view.issueTextArea.text = ""
        view.issueTextDisplay.text = ""
        view.issueTextPanelLayout.show(view.issueTextPanel, "display")
        if (!project) return

        if (!model.projectTableModels[(project.name)]) {
            def itm = new IssueTableModel(project,
                model.filter ?: model.mainMVC.model.filter)
            itm.categoryIcons = model.mainMVC.model.categoryIcons
            itm.statusIcons = model.mainMVC.model.statusIcons
            model.projectTableModels[(project.name)] = itm
        }

        view.issueTable.setModel(model.projectTableModels[(project.name)])

        def tcm = view.issueTable.columnModel
        tcm.getColumn(0).maxWidth = 24
        tcm.getColumn(1).maxWidth = 40
        tcm.getColumn(2).maxWidth = 35
        if (view.issueTable.model.columnCount == 5)
            tcm.getColumn(4).maxWidth = 150
    }

    void displayIssue(Issue issue) {
        if (!issue) return

        // hack because binding view.issueTextArea.font to
        // mainMVC.mode.issueDetailFont causes problems
        if (view.issueTextArea.font != model.mainMVC.model.issueDetailFont)
            view.issueTextArea.font  = model.mainMVC.model.issueDetailFont

        view.issueTextArea.text = issue.text
        view.issueTextArea.caretPosition = 0
        view.issueTextDisplay.text = rst2html(issue.text)
        view.issueTextDisplay.caretPosition = 0
        view.issueTextPanelLayout.show(view.issueTextPanel, "display")
    }

    void showProjectPopup(Project project, def x, def y) {
        model.popupProject = project
        view.projectPopupMenu.show(view.projectTree, x, y)
    }

    void showIssuePopup(Issue issue, def x, def y) {
        model.popupIssue = issue
        view.issuePopupMenu.show(view.issueTable, x, y)
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
        model.projectTableModels.clear()
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
            model.projectTableModels[(model.selectedProject.name)] = null
            displayProject(model.selectedProject)
        }
    }

    def deleteIssue = { evt ->
        def issue
        if (evt.source == view.deleteIssueButton)
            issue = getSelectedIssue()
        else issue = model.popupIssue

        model.selectedProject.issues.remove(issue.id)
        view.issueTable.model.issues.remove(issue)

        issue.delete()
        view.issueTable.invlidate()
    }

    def getSelectedIssue() {
        if (view.issueTable.selectionModel.isSelectionEmpty())
            return null

        return view.issueTable.model.issues[view.issueTable.
            convertRowIndexToModel(view.issueTable.selectedRow)]
    }

    String rst2html(String rst) {
        Document doc // memory model of document
        StringWriter outString
        StringBuilder result = new StringBuilder()

        // read the RST in with the RST parser
        new StringReader(rst).withReader { doc = jrstReader.read(it) }

        // transform to XHTML
        doc = jrstGen.transform(doc, rst2htmlXSL)

        // write to the StringWriter
        outString = new StringWriter()
        outString.withWriter { new XMLWriter(it, new OutputFormat("", true)).write(doc) }

        // java's embeded html is primitive, we need to massage the results
        outString.toString().eachLine { line ->

            // remove the XML version and encoding, title element,
            // meta elements
            if (line =~ /<\?.*\?>/ || line =~ /<meta.*$/ || line =~ /<title.*$/) { return }

            // all other elements, remove all class,xmlns attributes
            def m = (line =~ /(<\S+)(\s*(class|xmlns)=".*"\s*)*(\/?>.*)/)
            if (m) line = m[0][1] + m[0][4]

            result.append(line)
            
            // add in the CSS information to the head
            if (line =~/<head>/) result.append(model.issueCSS)
        }
        println result.toString()

        return result.toString()
    }
}
