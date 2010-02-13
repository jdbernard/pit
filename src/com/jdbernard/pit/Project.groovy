package com.jdbernard.pit

class Project {

    String name
    Map<String, Issue> issues = [:]
    Map<String, Project> projects = [:]

    Project(File dir, Filter filter = null) {
        dir.eachFile { child ->

            // add sub projects
            if (child.isDirectory())  {
                if ( child.name ==~ /\d{4}/ ||  // just an issue folder
                    (filter && !filter.accept(child.name)))
                    return

                // otherwise build and add to list
                projects[(child.name)] =  new Project(child, filter)
            } else if (child.isFile()) {
                def issue
                
                // if exception, then not an issue
                try { issue = new Issue(child) } catch (all) { return }

                if (filter && !filter.accept(issue)) return

                issues[(issue.id)] = issue
            }
        }
    }
    
    public void eachIssue(Closure c) {
        for (i in issues.values()) c.call(i)
        for (p in projects.values()) p.eachIssue(c)
    }

    public void each(Filter filter = null, Closure c) {
        def is = filter?.issueSorter ?: { it.id.toInteger() }
        def ps = filter?.projectSorter ?: { it.name }

        for (issue in issues.values().sort(is)) {
            if (filter && !filter.accept(issue))
                return

            c.call(issue)
        }

        for (project in projects.values().sort(ps)) {
            if (filter && !filter.accept(project))
                return

            c.call(project)
            project.each(c)
        }
    }

    public void list(Map options = [:]) {
        if (!options.offset) options.offset = ""
        if (!options.verbose) options.verbose = false

        each(options.filter) {
            if (it instanceof Project) {
                println "\n${options.offset}${it.name}"
                println "${options.offset}${'-'.multiply(p.name.length())}"
            } else {
                println "${options.offset}${it.id}(${it.priority}): " +
                    "${it.category} ${it.title}"
                if (options.verbose) println "\n${it.text}"
            }
        }
    }
}
