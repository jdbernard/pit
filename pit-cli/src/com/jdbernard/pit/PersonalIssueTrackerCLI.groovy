package com.jdbernard.pit

import com.jdbernard.pit.file.*

import static java.lang.Math.max
import static java.lang.Math.min

// -------- command-line interface specification -------- //

def cli = new CliBuilder(usage: 'pit-cli [options]')
cli.h(longOpt: 'help', 'Show help information.')
cli.v(longOpt: 'verbose', 'Show verbose task information')
cli.l(longOpt: 'list', 'List issues. Unless otherwise specified it lists all '
    + 'sub projects and all unclosed issue categories.')
cli.i(argName: 'id', longOpt: 'id', args: 1,
    'Filter issues by id. Accepts a comma-delimited list.')
cli.c(argName: 'category', longOpt: 'category', args: 1,
    'Filter issues by category (bug, feature, task). Accepts a '
    + 'comma-delimited list. By default all categories are selected.')
cli.s(argName: 'status', longOpt: 'status', args: 1,
    'Filter issues by status (new, reassigned, rejected, resolved, ' +
    'validation_required)')
cli.p(argName: 'priority', longOpt: 'priority', args: 1,
    'Filter issues by priority. This acts as a threshhold, listing all issues '
    + 'greater than or equal to the given priority.')
cli.r(argName: 'project', longOpt: 'project', args: 1,
    'Filter issues by project (relative to the current directory). Accepts a '
    + 'comma-delimited list.')
/*cli.s(longOpt: 'show-subprojects',
    'Include sup projects in listing (default behaviour)')
cli.S(longOpt: 'no-subprojects', 'Do not list subprojects.')*/ // TODO: figure out better flags for these options.
cli.P(argName: 'new-priority', longOpt: 'set-priority', args: 1,
    required: false, 'Modify the priority of the selected issues.')
cli.C(argName: 'new-category', longOpt: 'set-category', args: 1,
    required: false, 'Modify the category of the selected issues.')
cli.S(argName: 'new-status', longOpt: 'set-status', args: 1,
    required: false, 'Modify the status of the selected issues.')
cli.n(longOpt: 'new-issue', 'Create a new issue.')
cli.d(longOpt: 'dir', argName: 'dir', args: 1, required: false,
    'Use <dir> as the base directory (defaults to current directory).')

// -------- parse CLI options -------- //
def opts = cli.parse(args)
def issuedb = [:]
def workingDir = new File('.')

// defaults for the issue filter/selector
def selectOpts = [
    categories: ['bug', 'feature', 'task'],
    status:     ['new', 'reassigned', 'rejected',
        'resolved', 'validation_required'],
    priority:   9,
    projects:   [],
    ids:        [],
    acceptProjects: true]

// defaults for changing properties of issue(s)
def assignOpts = [
    category:   Category.TASK,
    status:     Status.NEW,
    priority:   5,
    text:       "New issue."]

if (!opts) opts.l = true; // default to 'list'

if (opts.h) {
    cli.usage()
    System.exit(0) }

// read the category filter designation(s)
if (opts.c) {
    if (opts.c =~ /all/) {} // no-op, same as defaults
    else { selectOpts.categories = opts.c.split(/[,\s]/) } }
        
// parse the categories names into Category objects
try { selectOpts.categories =
    selectOpts.categories.collect { Category.toCategory(it) } }
catch (Exception e) {
    println "Invalid category option: '-c ${e.localizedMessage}'."
    println "Valid options are: \n${Category.values().join(', ')}"
    println " (abbreviations are accepted)."
    System.exit(1) }

// read the status filter designation(s)
if (opts.s) {
    // -s all
    if (opts.s =~ /all/) selectOpts.status = ['new', 'reassigned', 'rejected',
        'resolved', 'validation_required']
    // is <list>
    else selectOpts.status = opts.s.split(/[,\s]/) } 

// parse the statuses into Status objects
try { selectOpts.status =
    selectOpts.status.collect { Status.toStatus(it) } }
catch (Exception e) {
    println "Invalid status option: '-s ${e.localizedMessage}'."
    println "Valid options are: \b${Status.values().join(', ')}"
    println " (abbreviations are accepted.)"
    System.exit(1) }

// read and parse the priority filter
if (opts.p) try {
    selectOpts.priority = opts.p.toInteger() }
catch (NumberFormatException nfe) {
    println "Not a valid priority value: '-p ${opts.p}'."
    println "Valid values are: 0-9"
    System.exit(1) }

// read and parse the projects filter
if (opts.r) { selectOpts.projects =
    opts.r.toLowerCase().split(/[,\s]/).asType(List.class) }

// read and parse the ids filter
if (opts.i) { selectOpts.ids = opts.i.split(/[,\s]/).asType(List.class) }

// TODO: accept projects value from input

// read and parse the category to assign
if (opts.C) try { assignOpts.category = Category.toCategory(opts.C) }
catch (Exception e) {
    println "Invalid category option: '-C ${e.localizedMessage}'."
    println "Valid categories are: \n${Category.values().join(', ')}"
    println " (abbreviations are accepted)."
    System.exit(1) }

// read and parse the status to assign
if (opts.S) try { assignOpts.status = Status.toStatus(opts.S) }
catch (Exception e) {
    println "Invalid status option: '-S ${e.localizedMessage}'."
    println "Valid stasus options are: \n{Status.values().join(', ')}"
    println " (abbreviations are accepted)."
    System.exit(1) }

// read and parse the priority to assign
if (opts.P) try {assignOpts.priority = opts.P.toInteger() }
catch (NumberFormatException nfe) {
    println "Not a valid priority value: '-P ${opts.P}'."
    println "Valid values are: 0-9"
    System.exit(1) }

// look for assignment text
if (opts.getArgs().length > 0) {
    assignOpts.text = opts.getArgs()[0] }

// set the project working directory
if (opts.d) {
    workingDir = new File(opts.d.trim())
    if (!workingDir.exists()) {
        println "Directory '${workingDir}' does not exist."
        return -1 } }
def EOL = System.getProperty('line.separator')

// build issue list
issuedb = new FileProject(workingDir)

// build filter from options
def filter = new Filter(selectOpts)
 
// list first
if (opts.l) {

    // local function (closure) to print a single issue
    def printIssue = { issue, offset ->
        println "${offset}${issue}"
        if (opts.v) {
            println ""
            issue.text.eachLine { println "${offset}  ${it}" }
            println "" } }

    // local function (closure) to print a project and all visible subprojects
    def printProject
    printProject = { project, offset ->
        println "\n${offset}${project.name}"
        println "${offset}${'-'.multiply(project.name.length())}"
        project.eachIssue(filter) { printIssue(it, offset) }
        project.eachProject(filter) { printProject(it, offset + "  ") } }

    // print all the issues in the root of this db
    issuedb.eachIssue(filter) { printIssue(it, "") }
    // print all projects
    issuedb.eachProject(filter) { printProject(it, "") } } 

// new issues second
else if (opts.n) {
    def cat, priority
    String text = ""
    Issue issue
    def sin = System.in.newReader()

    if (opts.C) { cat = assignOpts.category }
    else while(true) {
            try {
                print "Category (bug, feature, task, closed): "
                cat = Category.toCategory(sin.readLine())
                break }
            catch (e) {
                println "Invalid category: " + e.getLocalizedMessage()
                println "Valid options are: \n${Category.values().join(', ')}"
                println " (abbreviations are accepted)." } }

    if (opts.P) { priority = assignOpts.priority }
    else while (true) {
        try {
            print "Priority (0-9): "
            priority = max(0, min(9, sin.readLine().toInteger()))
            break }
        catch (e) { println "Not a valid value." } }

    if (opts.getArgs().length  > 0) { text = assignOpts.text }
    else {
        println "Enter issue (use EOF): "
        try {
            def line = ""
            while(true) {
                line = sin.readLine()

                if (line =~ /EOF/) break

                text += line + EOL
            } }
        catch (e) {} }

    issue = issuedb.createNewIssue(category: cat, priority: priority, text: text)
    
    println "New issue created: "
    println issue }
    
// last, changes to existing issues
else {
    // change priority 
    if (opts.P) issuedb.walkProject(filter) { 
        it.priority = assignOpts.priority
        println "[${it}] -- set priority to ${assignOpts.priority}"}

    // change third
    else if (opts.C) issuedb.walkProject(filter) {
        it.category = assignOpts.cat
        println "[${it}] -- set category to ${assignOpts.category}"}

    // change status
    else if (opts.S) issuedb.walkProject(filter) {
        it.status = assignOpts.status 
        println "[${it}] -- set status to ${assignOpts.status}"}
}
