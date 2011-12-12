package com.jdbernard.pit

import com.jdbernard.pit.file.*

import org.joda.time.DateMidnight
import org.joda.time.DateTime

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
cli.e(argName: 'extended-property', args: 1, 'Filter for issues by extended ' +
    'property. Format is "-e <propname>=<propvalue>".')
/*cli.s(longOpt: 'show-subprojects',
    'Include sup projects in listing (default behaviour)')
cli.S(longOpt: 'no-subprojects', 'Do not list subprojects.')*/ // TODO: figure out better flags for these options.
cli.P(argName: 'new-priority', longOpt: 'set-priority', args: 1,
    'Modify the priority of the selected issues.')
cli.C(argName: 'new-category', longOpt: 'set-category', args: 1,
    'Modify the category of the selected issues.')
cli.S(argName: 'new-status', longOpt: 'set-status', args: 1,
    'Modify the status of the selected issues.')
cli.E(argName: 'new-extended-property', args: 1, 'Modify the extended ' +
    'property of the selected issues. Format is "-E <propname>=<propvalue>"')
cli.n(longOpt: 'new-issue', 'Create a new issue.')
cli._(longOpt: 'title', args: 1, argName: 'title', 'Give the title for a new' +
    ' issue or modify the title for an existing issue. By default the title' +
    ' for a new issue is expected on stanard input.')
cli._(longOpt: 'text', args: 1, argName: 'text', 'Give the text for a new' +
    ' issue or modify the text for an exising issue. By default the text for' +
    ' a new issue is expected on standard input.')
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

def VERSION = "3.2.3"
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
    extendedProperties: [:],
    acceptProjects: true]

// options for changing properties of issue(s)
def assignOpts = [:]

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
    
// read and parse extended property selection criteria
if (opts.e) {
    opts.es.each { option ->
        def parts = option.split("=")
        selectOpts.extendedProperties[parts[0]] =
            ExtendedPropertyHelp.parse(parts[1]) }}

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

if (opts.E) {
    opts.Es.each { option ->
        def parts = option.split("=")
        assignOpts[parts[0]] = ExtendedPropertyHelp.parse(parts[1]) }}

// Read the title if given.
if (opts.title) { assignOpts.title = opts.title }

// Read the text if given
if (opts.text) { assignOpts.text = opts.text }

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
            issue.extendedProperties.each { name, value ->
                def formattedValue = ExtendedPropertyHelp.format(value)
                println "${offset}  * ${name}: ${formattedValue}"}
            println ""}}

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
    Issue issue
    def sin = System.in.newReader()

    // Set the created extended property
    assignOpts.created = new DateTime()

    // Prompt for the different options if they were not given on the command
    // line. We will loop until they have entered a valid value. How it works: 
    // In the body of the loop we will try to read the input, parse it and
    // assign it to a variable. If the input is invalid it will throw as
    // exception before the assignment happens, the variable will still be
    // null, and we will prompt the user again.

    // Prompt for category.
    while(!assignOpts.category) {
        try {
            print "Category (bug, feature, task): "
            assignOpts.category = Category.toCategory(sin.readLine())
            break }
        catch (e) {
            println "Invalid category: " + e.getLocalizedMessage()
            println "Valid options are: \n${Category.values().join(', ')}"
            println " (abbreviations are accepted)." } }

    // Prompt for the priority.
    while (!assignOpts.priority) {
        try {
            print "Priority (0-9): "
            assignOpts.priority = max(0, min(9, sin.readLine().toInteger()))
            break }
        catch (e) { println "Not a valid value." } }

    // Prompt for the issue title. No need to loop as the input does not need
    // to be validated.
    if (!assignOpts.title) {
        println "Issue title: "
        assignOpts.title = sin.readLine().trim() }

    // Prompt for the issue text.
    if (!assignOpts.text) {
        assignOpts.text = ""
        println "Enter issue text (use EOF to stop): "
        try {
            def line = ""
            while(true) {
                line = sin.readLine()

                // Stop when they enter EOF
                if (line ==~ /^EOF$/) break

                assignOpts.text += line + EOL } }
        catch (e) {} }

    issue = issuedb.createNewIssue(assignOpts)
    
    println "New issue created: "
    println issue }
    
// last, changes to existing issues
else if (assignOpts.size() > 0) {

    // We are going to add some extra properties if the status is being changed,
    // because we are nice like that.
    if (assignOpts.status) { switch (assignOpts.status)  {
        case Status.RESOLVED: assignOpts.resolved = new DateTime(); break
        case Status.REJECTED: assignOpts.rejected = new DateTime(); break
        default: break }}

    issuedb.walkProject(filter) { issue ->
        println issue
        assignOpts.each { propName, value ->
            issue[propName] = value
            def formattedValue = ExtendedPropertyHelp.format(value)
            println "  set ${propName} to ${formattedValue}" } }}
            
else { cli.usage(); return -1 }}
