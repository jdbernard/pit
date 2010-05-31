package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.FileProject
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class PITController {

    // these will be injected by Griffon
    def model
    def view

    void mvcGroupInit(Map args) {

        SwingUtilities.invokeAndWait {
            model.issueListRenderer = new IssueTableCellRenderer()

            File pitHome, pitrcFile, pitswingrcFile
            Properties config = new Properties()

            // look for config directory
            pitHome = new File(System.getProperty('user.home'), '.pit')
            println "$pitHome is ${pitHome.exists() ? '' : 'not '} present."

            // look for general config options
            pitrcFile = new File(pitHome, 'pitrc')
            println "$pitrcFile is ${pitrcFile.exists() ? '' : 'not '} present."

            // load general config (if present)
            if (pitrcFile.exists()) {
                pitrcFile.withInputStream() { config.load(it) }
                println "Loaded pitrc"
            }

            // look for swing specific config
            pitswingrcFile = new File(pitHome, 'pitswingrc')
            println "$pitswingrcFile is " (pitswingrcFile.exists() ? 
                '' : 'not ') + "present."

            // load swing specific config (if present)
            if (pitswingrcFile.exists()) {
                pitswingrcFile.withInputStream() { config.load(it) }
                println "Loaded pitswingrc"
            }

            // Process Configurable Options
            // ----------------------------

            config.keySet().each { println it }

            // add custom category templates
            Category.values().each { category ->
                def expectedKey = "issue." + category.name().toLowerCase() +
                    ".template"
                println "Looking for key: $expectedKey"
                config.keySet().each { currentKey ->
                    if (currentKey == expectedKey) 
                    model.templates[(category)] = 
                        config.getProperty(expectedKey, "")
                    println "Template for category $category: '" +
                        model.templates[(category)] + "'"
                }
            }

            // load custom issueListRenderer
            // TODO: not yet supported (maybe no need)

            // load initial repositories
            if (config.containsKey('initial-repositories')) {
                def initRepos = config.getProperty('initial-repositories', '')
                initRepos = initRepos.split(/[;:,]/)
                initRepos.each { repoPath -> loadProject(new File(repoPath)) }
                println "Init repos: '$initRepos'"
            }

            // load custom issue CSS
            if (config.containsKey('issue.display.css')) {
                def issueCSS = config.getProperty('issue.display.css', "")

                // look for a file relative to the pit home directory
                def cssFile

                // use short-circuit logic to test several possible locations
                // for a css file
                if ((cssFile = new File(pitHome, issueCSS)).exists() ||
                    (cssFile = new File(pitHome.parentFile(), issueCSS)).exists() ||
                    (cssFile = new File(issueCSS).exists()))
                    issueCSS = cssFile.text
                
                println "CS for issue display: $issueCSS"
                model.issueCSS = issueCSS
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
        if (view.openDialog.showOpenDialog(view.frame) !=
            JFileChooser.APPROVE_OPTION) return

        loadProject(view.openDialog.selectedFile)

    }

    def loadProject = { File projectDir ->
        def newMVC

        // if this is not a valid directory, do nothing
        // TODO: log to the user that this is not a valid directory
        if (!projectDir.exists() || !projectDir.isDirectory()) return;

        // create new ProjectPanel MVC
        newMVC = buildMVCGroup('ProjectPanel',
            mainMVC: [model: model, view: view, controller: this],
            newIssueDialogMVC: model.newIssueDialogMVC,
            issueCellRenderer: model.issueListRenderer,
            issueCSS: model.issueCSS,
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
