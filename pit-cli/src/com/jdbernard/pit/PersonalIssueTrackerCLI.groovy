package com.jdbernard.pit

import static java.lang.Math.max
import static java.lang.Math.min

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
cli.P(argName: 'new-priority', longOpt: 'set-priority', args: 1,
    required: false, 'Modify the priority of the selected issues.')
cli.C(argName: 'new-category', longOpt: 'set-category', args: 1,
    required: false, 'Modify the category of the selected issues.')
cli.n(longOpt: 'new-issue', 'Create a new issue.')

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
if (opts.l) {
    def issuePrinter = { issue, offset ->
        println "${offset}${issue}"
        if (opts.v) {
            println ""
            issue.text.eachLine { println "${offset}  ${it}" }
            println ""
        }
    }

    def projectPrinter
    projectPrinter = { project, offset ->
        println "\n${offset}${project.name}"
        println "${offset}${'-'.multiply(project.name.length())}"
        project.eachIssue { issuePrinter.call(it, offset) }
        project.eachProject { projectPrinter.call(it, offset + "  ") }
    }

    issuedb.eachIssue { issuePrinter.call(it, "") }
    issuedb.eachProject { projectPrinter.call(it, "") }
        
}

// change priority second
else if (opts.P) {
    def priority
    try { priority = max(0, min(9, opts.P.toInteger())) }
    catch (e) { println "Invalid priority: ${opts.P}"; return 1 }

    walkProject(issuedb) { it.priority = priority }
}
// change category third
else if (opts.C) {
    def cat
    try { cat = Category.toCategory(opts.C) }
    catch (e) { println "Invalid category: ${opts.C}"; return 1 }

    walkProject(issuedb) { it.category = cat }
}
// new entry last
else if (opts.n) {
    def cat, priority
    String text = ""
    Issue ussie
    def sin = System.in.newReader()

    while(true) {
        try {
            print "Category (bug, feature, task, closed): "
            cat = Category.toCategory(sin.readLine())
            break
        } catch (e) {
            println "Invalid category: " + e.getLocalizedMessage()
            println "Valid options are: \n${Category.values().join(', ')}\n " +
                    "(abbreviations are accepted.)"
        }
    }

    while (true) {
        try {
            print "Priority (0-9): "
            priority = max(0, min(9, sin.readLine().toInteger()))
            break
        } catch (e) { println "Not a valid value." }
    }

    println "Enter issue (use EOF of ^D to end): "
    try {
        sin.eachLine { line ->
            def m = line =~ /(.*)EOF.*/
            if (m) {
                text << m[0][1]
                sin.close()
            } else text << line
        }
    } catch (e) {}

    issue = issuedb.createNewIssue(category: cat, priority: priority, text: text)
    
    println "New issue created: "
    println issue
}

def walkProject(Project p, Closure c) {
    p.eachIssue(c)
    p.eachProject { walkProject(it, c) }
}
