package com.jdbernard.pit.xml

import com.jdbernard.pit.*

public class XmlProject extends Project {

    def projectNode
    XmlRepository repository

    XmlProject(def projectNode, XmlRepository repository) {
        super(projectNode.@name)
        
        this.projectNode = projectNode
        this.repository = repository
    }

    XmlProject(String name, def parentProject, XmlRepository repository) {
        super(name)

        // Node constructor adds the node to the parent node
        projectNode = new Node(parentProject.projectNode, "Project",
            [name: name])
    
        repository.persist()
    }

    public void setName(String name) {
        super.setName(name)

        projectNode.@name = name
        repository.persist()
    }

    public XmlIssue createNewIssue(Map options) {
        if (!options) options = [:]
        if (!options.category) options.category = Category.TASK
        if (!options.status) options.status = Status.NEW
        if (!options.priority) options.priority = 5
        if (!options.text) options.text = "Default issue title.\n" +
                                          "====================\n"

        String id
        if (issues.size() == 0) id = "0000"
        else {
            id = (issues.values().max { it.id.toInteger() }).id
            id = (id.toInteger() + 1).toString().padLeft(id.length(), '0')
        }

        // XmlIssue constructor will persist XML data
        issues[(id)] = new XmlIssue(id, options.category, options.status,
            options.priority, options.text, repository, this)

        return issues[(id)]
    }

    public XmlProject createNewProject(String name) {
        // XmlProject constructor persists the XML data
        projects[(name)] = new XmlProject(name, this, repository)
        return projects[(name)]
    }

    public boolean deleteIssue(Issue issue) {
        if (!issues[(issue.id)]) return false

        issues.remove(issue.id)
        if (issue instanceof XmlIssue)
            projectNode.remove(issue.issueNode)

        repository.persist()

        return true
    }

    public boolean deleteProject(Project project) {
        if (!projects[(project.name)]) return false

        projects.remove(project.name)
        if (project instanceof XmlProject)
            projectNode.remove(project.projectNode)

        repository.persist()
    }
}
