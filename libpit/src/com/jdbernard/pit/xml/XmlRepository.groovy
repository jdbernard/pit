package com.jdbernard.pit.xml

import com.jdbernard.pit.*
import groovy.xml.XmlUtil

public class XmlRepository extends Repository {

    def repository
    def projects = []
    File repoFile

    public XmlRepository(File repoFile) {

        this.repoFile = repoFile
        repository = new XmlParser().parse(repoFile)

        repository.Project.each { projectNode ->
            projects << new XmlProject(projectNode)
        }

    }

    public synchronized void persist() {
        repoFile.withOutputStream { XmlUtil.serialize(repository, it) }
    }

    public XmlProject[] getRootProjects() {
        return projects as XmlProject[]
    }

    public XmlProject createNewProject(String name) {
        def newProject = new XmlProject(name, this, null)
        repository << newProject.projectNode

        persist()
        return newProject
    }

    public boolean deleteProject(Project p) {
        if (!projects.contains(p)) return false

        projects.remove(p)
        repository.remove(p.projectNode)
        
        return true
    }
}
