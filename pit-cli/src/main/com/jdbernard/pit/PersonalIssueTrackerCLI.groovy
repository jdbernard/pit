package com.jdbernard.pit

import com.jdbernard.pit.file.*

import org.joda.time.DateMidnight

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
cli.o(longOpt: 'order', argName: 'order', args: 1, required: false,
    'Order (sort) the results by the given properties. Provide a comma-' +
    'seperated list of property names to sort by in order of importance. The' +
    ' basic properties (id, category, status, and priority) can be given' +
    ' using their one-letter forms (i,c,s,p) for brevity. For example:' +
    ' "-o Due,p,c" would sort first by the extended property "Due", then for' +
    ' items that have the same "Due" value it would sort by priority, then' +
    ' by category.')
cli.d(longOpt: 'dir', argName: 'dir', args: 1, required: false,
    'Use <dir> as the base directory (defaults to current directory).')
cli.D(longOpt: 'daily-list', 'Print a Daily Task list based on issue Due and' +
    ' Reminder properties.')
cli._(longOpt: 'dl-scheduled', 'Show scheduled tasks in the daily list (all' +
    ' are shown by default).')
cli._(longOpt: 'dl-due', 'Show due tasks in the daily list (all are shown by' +
    ' default).')
cli._(longOpt: 'dl-reminder', 'Show upcoming tasks in the daily list (all ' +
    ' are shown by default).')
cli._(longOpt: 'dl-open', 'Show open tasks in the daily list (all are shown ' +
    ' by default).')
cli._(longOpt: 'dl-hide-scheduled', 'Hide scheduled tasks in the daily list' +
    ' (all are shown by default).')
cli._(longOpt: 'dl-hide-due', 'Show due tasks in the daily list (all are' +
    ' shown by default).')
cli._(longOpt: 'dl-hide-reminder', 'Show upcoming tasks in the daily list' +
    ' (all  are shown by default).')
cli._(longOpt: 'dl-hide-open', 'Show open tasks in the daily list (all are' +
    ' shown  by default).')
cli._(longOpt: 'version', 'Display PIT version information.')

// =================================== //
// ======== Parse CLI Options ======== //
// =================================== //

def VERSION = "3.1.0"
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
    print "Valid options are: \n${Status.values().join(', ')}"
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

// read and parse sort criteria
if (opts.o) {
    def sortProps = opts.o.split(',')
    selectOpts.issueSorter = sortProps.collect { prop ->
        switch (prop) {
            case ~/^i$/: return { issue -> issue.id }
            case ~/^p$/: return { issue -> issue.priority }
            case ~/^s$/: return { issue -> issue.status }
            case ~/^c$/: return { issue -> issue.category }
            default: return { issue -> issue[prop] } }}}
    
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


// ========================= //
// ======== Actions ======== //
// ========================= //

// list version information first
if (opts.version) {

    println "PIT CLI Version ${VERSION}"
    println "Written by Jonathan Bernard\n" }

else {

// build issue list
issuedb = new FileProject(workingDir)

// build filter from options
def filter = new Filter(selectOpts)
 
// list second
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

// daily list second
else if (opts.D) {

    // Parse daily list specific display options
    def visibleSections = []
    def suppressedSections

    // Parse the additive options first.
    if (opts.'dl-scheduled') { visibleSections << 'scheduled' }
    if (opts.'dl-due') { visibleSections << 'due' }
    if (opts.'dl-reminder') { visibleSections << 'reminder' }
    if (opts.'dl-open') { visibleSections << 'open' }

    // If the user did not add any sections assume they want them all.
    if (visibleSections.size() == 0) {
        visibleSections = ['scheduled', 'due', 'reminder', 'open'] }

    // Now go through the negative options.
    if (opts.'dl-hide-scheduled') { visibleSections -= 'scheduled' }
    if (opts.'dl-hide-due') { visibleSections -= 'due' }
    if (opts.'dl-hide-reminder') { visibleSections -= 'reminder' }
    if (opts.'dl-hide-open') { visibleSections -= 'open' }

    // If the user did not specifically ask for a status filter, we want a
    // different filter for the default when we are doing a daily list.
    if (!opts.s) { filter.status = [Status.NEW, Status.VALIDATION_REQUIRED] }

    // If the user did not give a specific sorting order, define our own.
    if (!opts.o) { filter.issueSorter = [ {it.due}, {it.priority}, {it.id} ] }

    // Get our issues
    def allIssues = issuedb.getAllIssues(filter)

    // Set up our time interval.
    def today = new DateMidnight()
    def tomorrow = today.plusDays(1)

    def scheduledToday = []
    def dueToday = []
    def reminderToday = []
    def notDueOrReminder = []

    def printIssue = { issue ->
        if (issue.due) println "${issue.due.toString('EEE, MM/dd')} -- ${issue}"
        else println "           -- ${issue}" }

    // Sort the issues into seperate lists based on their due dates and
    // reminders.
    allIssues.each { issue ->
        // Find the issues that are scheduled for today.
        if (issue.scheduled && issue.scheduled < tomorrow) {
            scheduledToday << issue }

        // Find the issues that are due today or are past due.
        else if (issue.due && issue.due < tomorrow) { dueToday << issue }

        // Find the issues that are not yet due but have a reminder for today or
        // days past.
        else if (issue.reminder && issue.reminder < tomorrow) {
            reminderToday << issue }

        // All the others (not due and no reminder).
        else notDueOrReminder << issue }

    // Print the issues
    if (visibleSections.contains('scheduled') && scheduledToday.size() > 0) {
        println "Tasks Scheduled for Today"
        println "-------------------------"

        scheduledToday.each { printIssue(it) }

        println "" }

    if (visibleSections.contains('due') && dueToday.size() > 0) {
        println "Tasks Due Today"
        println "---------------"

        dueToday.each { printIssue(it) }

        println ""}

    if (visibleSections.contains('reminder') && reminderToday.size() > 0) {
        println "Upcoming Tasks"
        println "--------------"

        reminderToday.each { printIssue(it) }

        println ""}

    if (visibleSections.contains('open') && notDueOrReminder.size() > 0) {
        println "Other Open Issues"
        println "-----------------"

        notDueOrReminder.each { printIssue(it) }

        println "" }}

// new issues fourth
else if (opts.n) {
    def cat, priority
    String text = ""
    Issue issue
    def sin = System.in.newReader()

    if (opts.C) { cat = assignOpts.category }
    else while(true) {
            try {
                print "Category (bug, feature, task): "
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

    // change category
    else if (opts.C) issuedb.walkProject(filter) {
        it.category = assignOpts.cat
        println "[${it}] -- set category to ${assignOpts.category}"}

    // change status
    else if (opts.S) issuedb.walkProject(filter) {
        it.status = assignOpts.status 
        println "[${it}] -- set status to ${assignOpts.status}"} }}
