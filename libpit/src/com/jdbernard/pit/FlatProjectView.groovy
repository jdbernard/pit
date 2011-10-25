package com.jdbernard.pit

public class FlatProjectView extends Project {

    public FlatProjectView(String name) { super(name) }

    public Issue createNewIssue(Map options) {
        throw new UnsupportedOperationException("The FlatProjectView is " +
            "read-only.")
    }

    public Project createNewProject(String name) {
        throw new UnsupportedOperationException("The FlatProjectView is " +
            "read-only.")
    }

    public boolean deleteIssue(Issue issue) { return false }
    public boolean deleteProject(Project project) { return false }

    public boolean delete() { return true }

    public void eachIssue(Filter filter = null, Closure closure) {
        def sorter = filter?.issueSorter ?: Filter.defaultIssueSorter
        def gatherIssues
        def gatheredIssues = []

        gatherIssues = { project, f -> 
            project.eachIssue(f) { gatheredIssues << it }
            project.eachProject(f) { gatherIssues(it, f) }
        }
        for (p in projects.values()) 
            if (!filter || filter.accept(p))
                gatherIssues(p, filter)

        gatheredIssues.sort(sorter).each(closure)
    }
}
