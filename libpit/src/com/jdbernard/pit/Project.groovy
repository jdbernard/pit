package com.jdbernard.pit

public abstract class Project {

    protected String name
    Map<String, Issue> issues = [:]
    Map<String, Project> projects = [:]

    Project(String name) { this.name = name }

    public void eachIssue(Filter filter = null, Closure c) {
        def sorter = filter?.issueSorter ?: Filter.defaultIssueSorter
        for (i in issues.values().sort(sorter)) 
            if (!filter || filter.accept(i))
                c.call(i)
    }

    public void eachProject(Filter filter = null, Closure c) {
        def sorter = filter?.projectSorter ?: Filter.defaultProjectSorter
        for (p in projects.values().sort(sorter))
            if (!filter || filter.accept(p))
                c.call(p)
    }

    // walk every issue and project in this project recursively and execute the
    // given closure on each issue that meets the filter criteria
    public void walkProject(Filter filter, Closure c) {
        this.eachIssue(filter, c)
        this.eachProject(filter) { p -> p.walkProject(filter, c) }
    }

    // This get all issues, including subissues
    public List getAllIssues(Filter filter = null) {
        List result = this.issues.findAll { filter.accept(it) }
        this.eachProject(filter) { p -> result += p.getAllIssues(filter) }
    }

    public void setName(String name) { this.name = name }

    public String getName() { return name }

    @Override
    String toString() { return name }

    public abstract Issue createNewIssue(Map options)

    public abstract Project createNewProject(String name)

    public abstract boolean deleteIssue(Issue issue)

    public abstract boolean deleteProject(Project project)
}
