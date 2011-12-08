package com.jdbernard.pit

public abstract class Project {

    protected String name
    Map<String, Issue> issues = [:]
    Map<String, Project> projects = [:]

    Project(String name) { this.name = name }

    public void eachIssue(Filter filter = null, Closure c) {
        def sorter = filter?.issueSorter ?: Filter.defaultIssueSorter
        for (i in sort(issues.values(), sorter)) 
            if (!filter || filter.accept(i))
                c.call(i) }

    public void eachProject(Filter filter = null, Closure c) {
        def sorter = filter?.projectSorter ?: Filter.defaultProjectSorter
        for (p in sort(projects.values(), sorter))
            if (!filter || filter.accept(p))
                c.call(p) }

    // walk every issue and project in this project recursively and execute the
    // given closure on each issue that meets the filter criteria
    public void walkProject(Filter filter, Closure c) {
        this.eachIssue(filter, c)
        this.eachProject(filter) { p -> p.walkProject(filter, c) } }

    // This get all issues, including subissues
    public List getAllIssues(Filter filter = null) {
        def sorter = filter?.issueSorter ?: Filter.defaultIssueSorter

        List allIssues = this.issues.values().findAll {
            filter ? filter.accept(it) : true }

        this.eachProject(filter) { p -> allIssues += p.getAllIssues(filter) }

        return sort(allIssues, sorter) }

    public void setName(String name) { this.name = name }

    public String getName() { return name }

    @Override
    String toString() { return name }

    public abstract Issue createNewIssue(Map options)

    public abstract Project createNewProject(String name)

    public abstract boolean deleteIssue(Issue issue)

    public abstract boolean deleteProject(Project project)

    protected List sort(def collection, def sorter) {
        if (sorter instanceof Closure) {
            return collection.sort(sorter) }
        else if (sorter instanceof List) {
            return sorter.reverse().inject(collection) { c, s -> c.sort(s) }}}
}
