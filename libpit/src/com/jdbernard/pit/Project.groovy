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

    public void setName(String name) { this.name = name }

    public String getName() { return name }

    @Override
    String toString() { return name }

    public abstract Issue createNewIssue(Map options)
}
