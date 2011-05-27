package com.jdbernard.pit.xml

import com.jdbernard.pit.*

public class XmlIssue extends Issue {

    def issueNode
    XmlProject project
    XmlRepository repository

    XmlIssue(def issueNode, XmlRepository repository, XmlProject project) {
        super(issueNode.@id, issueNode.@category ?: Category.TASK,
            issueNode.@status ?: Status.NEW, issueNode.@priority ?: 9)

        this.issueNode = issueNode
        this.project = project
        this.repository = repository
    }

    XmlIssue(String id, Category c = Category.TASK, Status s = Status.NEW,
    int p = 9, String text, XmlRepository repository, XmlProject project) {
        super(id, c, s, p)

        this.project = project
        this.repository = repository

        // Node constructor adds the node to the parent node
        issueNode = new Node(project.projectNode, "Issue", 
            [id: id, category: c, status: s, priority: p])

        this.text = text
        issueNode.value = text

        repository.persist()
    }

    public void setCategory(Category c) {
        super.setCategory(c)

        issueNode.@category = c.name()
        repository.persist()
    }

    public void setStatus(Status s) {
        super.setStatus(s)

        issueNode.@status = s.name()
        repository.persist()
    }

    public void setPriority(int p) {
        super(p)

        issueNode.@priority = p
        repository.persist()

    public void setText(String t) {
        super.setText(t)

        issueNode.value = t
        repository.persist()
    }

}
