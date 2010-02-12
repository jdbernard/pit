#!/usr/bin/groovy

def cli = new CliBuilder(usage: '')
cli.h(longOpt: 'help', 'Show help information.')
cli.v(longOpt: 'verbose', 'Show verbose task information')
cli.l(longOpt: 'list', 'List issues. Unless otherwise specified it lists all '
    + 'sub projects and all unclosed issue categories.')
cli.i(argName: 'id', longOpt: 'id', args: 1,
    'Filter issues by id. Accepts a comma-delimited list.')
cli.c(argName: 'category', longOpt: 'category', args: 1,
    'Filter issues by category (bug, feature, task, closed). Accepts a '
    + 'comma-delimited list.')
cli.p(argName: 'priority', longOpt: 'priority', args: 1,
    'Filter issues by priority. This acts as a threshhold, listing all issues '
    + 'greater than or equal to the given priority.')
cli.r(argName: 'project', longOpt: 'project', args: 1,
    'Filter issues by project (relative to the current directory). Accepts a '
    + 'comma-delimited list.')
cli.s(longOpt: 'show-subprojects',
    'Include sup projects in listing (default behaviour)')
cli.S(longOpt: 'no-subprojects', 'Do not list subprojects.')

def opts = cli.parse(args)
def issuedb = [:]

if (!opts) System.exit(1) // better solution?

if (opts.h) cli.usage()

def categories = ['bug','feature','task']
if (opts.c) categories = opts.c.split(/[,\s]/)
categories = categories.collect { Category.toCategory(it) }

// build issue list
issuedb = new Project(new File('.'),
    new Filter('categories': categories,
    'priority': (opts.p ? opts.p.toInteger() : 9),
    'projects': (opts.r ? opts.r.toLowerCase().split(/[,\s]/).asType(List.class) : []),
    'ids': (opts.i ? opts.i.split(/[,\s]/).asType(List.class) : []),
    'acceptProjects': (opts.s || !opts.S)))

// list first
if (opts.l) issuedb.list('verbose': opts.v)

// change priority second
//else if (opts.cp)

// change category third
//else if (opts.cc)

// new entry last

enum Category {
    BUG,
    FEATURE,
    TASK,
    CLOSED

    public static Category toCategory(String s) {
        for(c in Category.values())
            if (c.toString().startsWith(s.toUpperCase())) return c
        throw new IllegalArgumentException("No category matches ${s}.")
    }
}

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

class Issue {

    String id
    Category category
    int priority
    String title
    String text

    Issue(File file) {

        def matcher = file.name =~ /(\d{4})([bftc])(\d).*/
        if (!matcher) return null

        id = matcher[0][1]
        category = Category.toCategory(matcher[0][2])
        priority = matcher[0][3].toInteger()

        file.withReader { title = it.readLine() }
        text = file.text
    }
}

class Filter {

    List<Category> categories = null
    List<String> projects = null
    List<String> ids = null
    int priority = 9
    boolean acceptProjects = true
    Closure projectSorter
    Closure issueSorter

    public boolean accept(Issue i) {
        return (i.priority <= priority &&
                (!categories || categories.contains(i.category)) &&
                (!ids || ids.contains(i.id)))
    }

    public boolean accept(Project p) {
        return (acceptProjects && 
                (!projects || projects.contains(p.name)))
    }

    public boolean accept(String name) {
        return (acceptProjects && 
                (!projects || projects.contains(name)))
    }
}
