package com.jdbernard.pit

class Project {

    String name
    Map<String, Issue> issues = [:]
    Map<String, Project> projects = [:]
    File source

    Project(File dir) {
        if (!dir.isDirectory())
            throw new IllegalArgumentException(
                "${dir.name} is not a directory.")

        this.source = dir
        this.name = dir.name

        dir.eachFile { child ->

            // add sub projects
            if (child.isDirectory())  {
                if ( child.name ==~ /\d{4}/)  return // just an issue folder

                // otherwise build and add to list
                projects[(child.name)] =  new Project(child)
            } else if (child.isFile()) {
                def issue
                
                // if exception, then not an issue
                try { issue = new Issue(child) } catch (all) { return }

                issues[(issue.id)] = issue
            }
        }
    }

    public void rename(String newName) {
        this.name = newName
        source.renameTo(new File(source.canonicalFile.parentFile, newName))
    }

    public void setName(String name) { rename(name) }
    
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

    /*public void each(Filter filter = null, Closure c) {
        def is = filter?.issueSorter ?: Filter.defaultIssueSorter
        def ps = filter?.projectSorter ?: Filter.defaultProjectSorter

        for (issue in issues.values().sort(is)) {
            if (filter && !filter.accept(issue))
                return

            c.call(issue)
        }

        for (project in projects.values().sort(ps)) {
            if (filter && !filter.accept(project))
                return

            c.call(project)
        }
    }*/

    public Issue createNewIssue(Map options) {
        if (!options.category) options.category = Category.TASK
        if (!options.priority) options.priority = 5
        if (!options.text) options.text = "Default issue title.\n" +
                                          "====================\n"
        String id
        if (issues.size() == 0) id = '0000'
        else {
            id = (issues.values().max { it.id.toInteger() }).id
            id = (id.toInteger() + 1).toString().padLeft(id.length(), '0')
        }

        def issueFile = new File(source, Issue.makeFilename(id, options.category, options.priority))
        assert !issueFile.exists()
        issueFile.createNewFile()
        issueFile.write(options.text)

        return new Issue(issueFile)
    }

    @Override
    String toString() { return name }

}
