package com.jdbernard.pit

import com.jdbernard.pit.file.*

import static java.lang.Math.max
import static java.lang.Math.min

def cli = new CliBuilder(usage: 'pit-cli [options]')
cli.h(longOpt: 'help', 'Show help information.')
cli.v(longOpt: 'verbose', 'Show verbose task information')
cli.l(longOpt: 'list', 'List issues. Unless otherwise specified it lists all '
    + 'sub projects and all unclosed issue categories.')
cli.i(argName: 'id', longOpt: 'id', args: 1,
    'Filter issues by id. Accepts a comma-delimited list.')
cli.c(argName: 'category', longOpt: 'category', args: 1,
    'Filter issues by category (bug, feature, task). Accepts a '
    + 'comma-delimited list.')
cli.t(argName: 'status', longOpt: 'status', args: 1,
    'Filter issues by status (new, reassigned, rejected, resolved, ' +
    'validation_required)')
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
cli.T(argName: 'new-status', longOpt: 'set-status', args: 1,
    required: false, 'Modify the status of the selected issues.')
cli.n(longOpt: 'new-issue', 'Create a new issue.')
cli.d(longOpt: 'dir', argName: 'dir', args: 1, required: false,
    'Use <dir> as the base directory (defaults to current directory).')

def opts = cli.parse(args)
def issuedb = [:]
def workingDir = new File('.')

if (!opts) System.exit(1) // better solution?

if (opts.h) cli.usage()

def categories = ['bug','feature','task']
if (opts.c) categories = opts.c.split(/[,\s]/)
categories = categories.collect { Category.toCategory(it) }

def statusList = ['new', 'validation_required']
if (opts.t) statusList = opts.t.split(/[,\s]/)
statusList = statusList.collect { Status.toStatus(it) }

if (opts.d) {
    workingDir = new File(opts.d.trim())
    if (!workingDir.exists()) {
        println "Directory '${workingDir}' does not exist."
        return -1
    }
}

def EOL = System.getProperty('line.separator')

// build issue list
issuedb = new FileProject(workingDir)

// build filter from options
def filter = new Filter('categories': categories,
    'status': statusList,
    'priority': (opts.p ? opts.p.toInteger() : 9),
    'projects': (opts.r ? opts.r.toLowerCase().split(/[,\s]/).asType(List.class) : []),
    'ids': (opts.i ? opts.i.split(/[,\s]/).asType(List.class) : []),
    'acceptProjects': (opts.s || !opts.S))
 
// list first
if (opts.l) {

    def printIssue = { issue, offset ->
        println "${offset}${issue}"
        if (opts.v) {
            println ""
            issue.text.eachLine { println "${offset}  ${it}" }
            println ""
        }
    }

    def printProject
    printProject = { project, offset ->
        println "\n${offset}${project.name}"
        println "${offset}${'-'.multiply(project.name.length())}"
        project.eachIssue(filter) { printIssue(it, offset) }
        project.eachProject(filter) { printProject(it, offset + "  ") }
    }
    issuedb.eachIssue(filter) { printIssue(it, "") }
    issuedb.eachProject(filter) { printProject(it, "") }
}

// change priority second
else if (opts.P) {
    def priority
    try { priority = max(0, min(9, opts.P.toInteger())) }
    catch (e) { println "Invalid priority: ${opts.P}"; return 1 }

    walkProject(issuedb, filter) { it.priority = priority }
}
// change category third
else if (opts.C) {
    def cat
    try { cat = Category.toCategory(opts.C) }
    catch (e) { println "Invalid category: ${opts.C}"; return 1 }

    walkProject(issuedb, filter) { it.category = cat }
}
// change status fourth
else if (opts.T) {
    def status
    try { status = Status.toStatus(opts.T) }
    catch (e) { println "Invalid status: ${opts.T}"; return 1 }

    walkProject(issuedb, filter) { it.status = status }
}
// new entry last
else if (opts.n) {
    def cat, priority
    String text = ""
    Issue issue
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
                text += m[0][1] + EOL
                sin.close()
            } else text += line + EOL
        }
    } catch (e) {}


    issue = issuedb.createNewIssue(category: cat, priority: priority, text: text)
    
    println "New issue created: "
    println issue
}

def walkProject(Project p, Filter filter, Closure c) {
    p.eachIssue(filter, c)
    p.eachProject(filter) { walkProject(it, filter, c) }
}

def printProject(Project project, String offset, Filter filter, boolean verbose = false) {
}
