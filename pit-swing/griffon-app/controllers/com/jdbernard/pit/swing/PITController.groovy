package com.jdbernard.pit.swing

import com.jdbernard.pit.FileProject
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class PITController {

    // these will be injected by Griffon
    def model
    def view

    void mvcGroupInit(Map args) {

        SwingUtilities.invokeAndWait {
            model.issueListRenderer = new IssueListCellRenderer()
            model.issueListRenderer.categoryIcons = model.categoryIcons
            model.issueListRenderer.statusIcons = model.statusIcons

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

        //
        model.newIssueDialogMVC = buildMVCGroup('NewIssueDialog')
    }

    void refreshIssues() {
        model.projectPanelMVCs.each { title, mvc ->
            mvc.controller.refreshIssues()
        }
    }

    def openProject = { evt = null -> 
        def projectDir
        def newMVC
        if (view.openDialog.showOpenDialog(view.frame) !=
            JFileChooser.APPROVE_OPTION) return

        projectDir = view.openDialog.selectedFile

        // create new ProjectPanel MVC
        newMVC = buildMVCGroup('ProjectPanel',
            mainMVC: [model: model, view: view, controller: this],
            newIssueDialogMVC: model.newIssueDialogMVC,
            issueCellRenderer: model.issueListRenderer,
            rootProject: new FileProject(projectDir))
        newMVC.model.id = projectDir.name
        
        // if we already have a tab with this id
        if (model.projectPanelMVCs[(newMVC.model.id)]) {
            // try using the canonical path
            newMVC.model.id = projectDir.canonicalPath

            // still not unique?
            if (model.projectPanelMVCs[(newMVC.model.id)]) {
                
                // first time this has happened?
                if (!model.projectIdMap[(newMVC.model.id)]) 
                    model.projectIdMap[(newMVC.model.id)] = 0
                // no? increment
                else model.projectIdMap[(newMVC.model.id)] = 
                    model.projectIdMap[(newMVC.model.id)] + 1

                // use our new, unique id
                newMVC.model.id += "-" + model.projectIdMap[(newMVC.model.id)] 
            }
        }

        model.projectPanelMVCs[newMVC.model.id] = newMVC
        view.mainTabbedPane.addTab(newMVC.model.id, newMVC.view.panel)
    }

    def closeProject = { evt = null ->
        model.projectPanelMVCs.remove(view.mainTabbedPane.getTitleAt(
            view.mainTabbedPane.selectedIndex))
        view.mainTabbedPane.remove(view.mainTabbedPane.selectedComponent)
    }

    def shutdown = { evt = null ->
        app.shutdown()
    }

}
