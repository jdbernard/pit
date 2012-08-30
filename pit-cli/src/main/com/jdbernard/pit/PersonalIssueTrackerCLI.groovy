/**
 * # Personal Issue Tracker Command Line Interface
 * @author Jonathan Bernard <jdbernard@gmail.com>
 * @copyright 2009-2012 Jonathan Bernard
 *
 * This is a command-line interface to my personal issue tracker system.
 */
package com.jdbernard.pit

import com.jdbernard.pit.file.*

import org.joda.time.DateMidnight
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import static java.lang.Math.max
import static java.lang.Math.min

def log = LoggerFactory.getLogger(getClass())

/// ## Command Line Options ##
/// --------------------------
/// @org cli-options
def cli = new CliBuilder(usage: 'pit-cli [options]')

/// -h,--help
/// :   Show help information
cli.h(longOpt: 'help', 'Show help information.')

/// -v,--verbose
/// :   Show verbose task information.
cli.v(longOpt: 'verbose', 'Show verbose task information')

/// -l,--list
/// :   List issues in the current project.
cli.l(longOpt: 'list', 'List issues in the current project.')

/// -i,--id
/// :   Filter issues by id. Accepts a comma-delimited list.  
/// *Example:* `pit -l -i 0001,0002`
cli.i(argName: 'id', longOpt: 'id', args: 1,
    'Filter issues by id. Accepts a comma-delimited list.')

/// -c,--category
/// :   Filter issues by category (bug, feature, task). Accepts a
///     comma-delimited list. By default all categories are selected. The full
///     category name is not required, just enough to be uniquely identifiable.  
/// *Example:* `pit -l -c bug,t # List bugs and tasks.`
cli.c(argName: 'category', longOpt: 'category', args: 1,
    'Filter issues by category (bug, feature, task). Accepts a '
    + 'comma-delimited list. By default all categories are selected.')

/// -s,--status
/// :   Filter issues by status (new, reassigned, rejected, resolved,
///     validation_required). The full status is not required, just enough to
///     uniquely identify the status.  
/// *Example:* `pit -l -s reas,rej # List Reassigned and Rejected issues.`
cli.s(argName: 'status', longOpt: 'status', args: 1,
    'Filter issues by status (new, reassigned, rejected, resolved, ' +
    'validation_required)')

/// -p,--priority
/// :   Filter issues by priority. This acts as a threshhold, listing all
///     issues greater than or equal to the given priority.  
/// *Example:* `pit -l -p 5 # List all issues with priority >= 5`
cli.p(argName: 'priority', longOpt: 'priority', args: 1,
    'Filter issues by priority. This acts as a threshhold, listing all issues '
    + 'greater than or equal to the given priority.')

/// -r,--project
/// :   Filter issues by project (relative to the current directory). Accepts a
///     comma-delimited list. This option should be used in conjunction with the
///     `R,--recursive` option.  
/// *Example:* `pit -l -R --project <project_name>`
cli.r(argName: 'project', longOpt: 'project', args: 1,
    'Filter issues by project (relative to the current directory). Accepts a '
    + 'comma-delimited list.')

/// -R,--recursive
/// :   Recursively include subprojects.
cli.R(longOpt: 'recursive', 'Include subprojects.')

/// -e,--extended-property
/// :   Filter for issues by extended property. Format is
///     `-e <propname>=<propvalue>`.
cli.e(argName: 'extended-property', args: 1, 'Filter for issues by extended ' +
    'property. Format is "-e <propname>=<propvalue>".')

/*cli.s(longOpt: 'show-subprojects',
    'Include sup projects in listing (default behaviour)')
cli.S(longOpt: 'no-subprojects', 'Do not list subprojects.')*/ // TODO: figure out better flags for these options.

/// -P,--set-priority
/// :   Modify the priority of the selected issues. Requires a value from 0-9.
cli.P(argName: 'new-priority', longOpt: 'set-priority', args: 1,
    'Modify the priority of the selected issues.')

/// -C,--set-category
/// :   Modify the category of the selected issues.
cli.C(argName: 'new-category', longOpt: 'set-category', args: 1,
    'Modify the category of the selected issues.')

/// -S,--set-status
/// :   Modify the status of the selected issues.
cli.S(argName: 'new-status', longOpt: 'set-status', args: 1,
    'Modify the status of the selected issues.')

/// -E,--new-issue
/// :   Modify the extended property of the selected issues. Format is
///     `-E <propname>=<propvalue>`
cli.E(argName: 'new-extended-property', longOpt: 'set-extended-property',
    args: 1, 'Modify the extended property of the selected issues. Format ' +
    'is "-E <propname>=<propvalue>"')

/// -n,--new-issue
/// :   Create a new issue
cli.n(longOpt: 'new-issue', 'Create a new issue.')

/// --title
/// :   Give the title for a new issue or modify the title for an existing
///     issue. By default the title for a new issue is expected on stanard
///     input.
cli._(longOpt: 'title', args: 1, argName: 'title', 'Give the title for a new' +
    ' issue or modify the title for an existing issue. By default the title' +
    ' for a new issue is expected on stanard input.')

/// --text
/// :   Give the text for a new issue or modify the text for an exising
///     issue. By default the text for a new issue is expected on standard
///     input.
cli._(longOpt: 'text', args: 1, argName: 'text', 'Give the text for a new' +
    ' issue or modify the text for an exising issue. By default the text for' +
    ' a new issue is expected on standard input.')

/** -o,--order
  * :   Order (sort) the results by the given properties. Provide a
  *     comma-seperated list of property names to sort by in order of
  *     importance. The basic properties (id, category, status, and priority)
  *     can be given using their one-letter forms (i,c,s,p) for brevity. For
  *     example: `-o Due,p,c` would sort first by the extended property `Due`,
  *     then for items that have the same `Due` value it would sort by
  *     priority, then by category. */
cli.o(longOpt: 'order', argName: 'order', args: 1, required: false,
    'Order (sort) the results by the given properties. Provide a comma-' +
    'seperated list of property names to sort by in order of importance. The' +
    ' basic properties (id, category, status, and priority) can be given' +
    ' using their one-letter forms (i,c,s,p) for brevity. For example:' +
    ' "-o Due,p,c" would sort first by the extended property "Due", then for' +
    ' items that have the same "Due" value it would sort by priority, then' +
    ' by category.')

/// -d,--dir
/// :   Use `<dir>` as the base directory (defaults to current directory).
cli.d(longOpt: 'dir', argName: 'dir', args: 1, required: false,
    'Use <dir> as the base directory (defaults to current directory).')

/// -D,--daily-list
/// :   Print a Daily Task list based on issue Due, Scheduled, and Reminder
///     extended properties.
cli.D(longOpt: 'daily-list', 'Print a Daily Task list based on issue Due and' +
    ' Reminder properties.')

/// --dl-scheduled
/// :   Show scheduled tasks in the daily list (all are shown by default).
cli._(longOpt: 'dl-scheduled', 'Show scheduled tasks in the daily list (all' +
    ' are shown by default).')

/// --dl-due
/// :   Show due tasks in the daily list (all are shown by default).
cli._(longOpt: 'dl-due', 'Show due tasks in the daily list (all are shown by' +
    ' default).')

/// --dl-upcoming
/// :   Show upcoming tasks in the daily list (all are shown by default).
cli._(longOpt: 'dl-upcoming', 'Show upcoming tasks in the daily list (all ' +
    ' are shown by default).')

/// --dl-open
/// :   Show open tasks in the daily list (all are shown  by default).
cli._(longOpt: 'dl-open', 'Show open tasks in the daily list (all are shown ' +
    ' by default).')

/// --dl-hide-scheduled
/// :   Hide scheduled tasks in the daily list (all are shown by default).
cli._(longOpt: 'dl-hide-scheduled', 'Hide scheduled tasks in the daily list' +
    ' (all are shown by default).')

/// --dl-hide-due
/// :   Show due tasks in the daily list (all are shown by default).
cli._(longOpt: 'dl-hide-due', 'Show due tasks in the daily list (all are' +
    ' shown by default).')

/// --dl-hide-upcoming
/// :   Show upcoming tasks in the daily list (all  are shown by default).
cli._(longOpt: 'dl-hide-upcoming', 'Show upcoming tasks in the daily list' +
    ' (all  are shown by default).')

/// --dl-hide-open
/// :   Show open tasks in the daily list (all are shown  by default).
cli._(longOpt: 'dl-hide-open', 'Show open tasks in the daily list (all are' +
    ' shown  by default).')

/// --dl-upcoming-days
/// :   The upcoming tasks section in the daily list includes any tasks due 
///     within the next seven days by default. This option overrides that 
///     default and allows you to specify the number of days ahead the upcoming
///     section looks.
cli._(longOpt: 'dl-upcoming-days', argName: 'num-days', args:1, required: false,
    'The upcoming tasks section in the daily list includes any tasks due ' +
    'within the next seven days by default. This option overrides that ' +
    'default and allows you to specify the number of days ahead the upcoming ' +
    'section looks.')

/// --version
/// :   Display PIT version information.
cli._(longOpt: 'version', 'Display PIT version information.')

/// ## Parse CLI Options ##
/// -----------------------

log.trace("Parsing options.")

def VERSION = "3.3.3"
def opts = cli.parse(args)
def issuedb = [:]
def workingDir = new File('.')

/// Defaults for the issue filter/selector.
def selectOpts = [
    categories: ['bug', 'feature', 'task'],
    status:     ['new', 'reassigned', 'rejected',
        'resolved', 'validation_required'],
    priority:   9,
    projects:   [],
    ids:        [],
    extendedProperties: [:],
    acceptProjects: true]

/// Defaults for changing properties of issue(s)
def assignOpts = [:]

if (!opts || opts.h) {
    cli.usage()
    System.exit(0) }

///Read the `-c` option: category filter designation(s).
if (opts.c) {
    if (opts.c =~ /all/) {} // no-op, same as defaults
    else { selectOpts.categories = opts.c.split(/[,\s]/) } }
        
/// Parse the categories names into Category objects.
try { selectOpts.categories =
    selectOpts.categories.collect { Category.toCategory(it) } }
catch (Exception e) {
    println "Invalid category option: '-c ${e.localizedMessage}'."
    println "Valid options are: \n${Category.values().join(', ')}"
    println " (abbreviations are accepted)."
    System.exit(1) }

/// Read the `-s` option: status filter designation(s).
if (opts.s) {
    // -s all
    if (opts.s =~ /all/) selectOpts.status = ['new', 'reassigned', 'rejected',
        'resolved', 'validation_required']
    // <list>
    else selectOpts.status = opts.s.split(/[,\s]/) } 

/// Parse the statuses into Status objects.
try { selectOpts.status =
    selectOpts.status.collect { Status.toStatus(it) } }
catch (Exception e) {
    println "Invalid status option: '-s ${e.localizedMessage}'."
    print "Valid options are: \n${Status.values().join(', ')}"
    println " (abbreviations are accepted.)"
    System.exit(1) }

/// Read and parse the `-p` option: priority filter.
if (opts.p) try {
    selectOpts.priority = opts.p.toInteger() }
catch (NumberFormatException nfe) {
    println "Not a valid priority value: '-p ${opts.p}'."
    println "Valid values are: 0-9"
    System.exit(1) }

/// Read and parse the `-r` option: projects filter.
if (opts.r) { selectOpts.projects =
    opts.r.toLowerCase().split(/[,\s]/).asType(List.class) }

/// Read and parse the `-i` option: id filter.
if (opts.i) { selectOpts.ids = opts.i.split(/[,\s]/).asType(List.class) }

/// Read and parse the `-o` option: sort criteria.
if (opts.o) {
    def sortProps = opts.o.split(',')
    selectOpts.issueSorter = sortProps.collect { prop ->
        switch (prop) {
            case ~/^i$/: return { issue -> issue.id }
            case ~/^p$/: return { issue -> issue.priority }
            case ~/^s$/: return { issue -> issue.status }
            case ~/^c$/: return { issue -> issue.category }
            default: return { issue -> issue[prop] } }}}
    
/// Read and parse any extended property selection criteria.
if (opts.e) {
    opts.es.each { option ->
        def parts = option.split("=")
        selectOpts.extendedProperties[parts[0]] =
            ExtendedPropertyHelp.parse(parts[1]) }}

/// Read and parse the `-C` option: category to assign.
if (opts.C) try { assignOpts.category = Category.toCategory(opts.C) }
catch (Exception e) {
    println "Invalid category option: '-C ${e.localizedMessage}'."
    println "Valid categories are: \n${Category.values().join(', ')}"
    println " (abbreviations are accepted)."
    System.exit(1) }

/// Read and parse the `-S` option: status to assign.
if (opts.S) try { assignOpts.status = Status.toStatus(opts.S) }
catch (Exception e) {
    println "Invalid status option: '-S ${e.localizedMessage}'."
    println "Valid stasus options are: \n${Status.values().join(', ')}"
    println " (abbreviations are accepted)."
    System.exit(1) }

/// Read and parse the `-P` option: priority to assign.
if (opts.P) try {assignOpts.priority = opts.P.toInteger() }
catch (NumberFormatException nfe) {
    println "Not a valid priority value: '-P ${opts.P}'."
    println "Valid values are: 0-9"
    System.exit(1) }

/// Read an parse any extended properties to be set.
if (opts.E) {
    opts.Es.each { option ->
        def parts = option.split("=")
        assignOpts[parts[0]] = ExtendedPropertyHelp.parse(parts[1]) }}

/// Read the title if given.
if (opts.title) { assignOpts.title = opts.title }

/// Read the text if given.
if (opts.text) { assignOpts.text = opts.text }

/// Set the project working directory.
if (opts.d) {
    workingDir = new File(opts.d.trim())
    if (!workingDir.exists()) {
        println "Directory '${workingDir}' does not exist."
        return -1 } }

def EOL = System.getProperty('line.separator')

log.debug("Finished parsing options:\nworkingDir: {}\nselectOpts: {}\nassignOpts: {}",
    workingDir.canonicalPath, selectOpts, assignOpts)

/// ## Actions ##
/// -------------

/// ### Version information.
if (opts.version) {

    println "PIT CLI Version ${VERSION}"
    println "Written by Jonathan Bernard\n" }


/// ----
else {

/// Build issue list.
log.trace("Building issue database.")
issuedb = new FileProject(workingDir)

/// Build filter from options.
log.trace("Defining the filter.")
def filter = new Filter(selectOpts)
 
/// ### List
if (opts.l) {

    log.trace("Listing issues.")

    /// Local function (closure) to print a single issue.
    def printIssue = { issue, offset ->
        println "${offset}${issue}"
        if (opts.v) {
            println ""
            issue.text.eachLine { println "${offset}  ${it}" }
            println ""
            issue.extendedProperties.each { name, value ->
                def formattedValue = ExtendedPropertyHelp.format(value)
                println "${offset}  * ${name}: ${formattedValue}"}
            println ""}}

    /// Local function (closure) to print a project and all visible subprojects.
    def printProject
    printProject = { project, offset ->
        println "\n${offset}${project.name}"
        println "${offset}${'-'.multiply(project.name.length())}"
        project.eachIssue(filter) { printIssue(it, offset) }
        project.eachProject(filter) { printProject(it, offset + "  ") } }

    /// Print all the issues in the root of this db.
    issuedb.eachIssue(filter) { printIssue(it, "") }

    /// If the user set the recursive flag print all projects.
    if (opts.R) {
        issuedb.eachProject(filter) { printProject(it, "") }} } 

/// ### Daily List
else if (opts.D) {

    log.trace("Showing a daily list.")

    /// Set up our time intervals.
    def today = new DateMidnight()
    def tomorrow = today.plusDays(1)

    /// #### Parse daily list specific display options.
    def visibleSections = []
    def suppressedSections
    def upcomingCutoff = today.plusDays(7)

    /// Check for a custom upcoming section cutoff date.
    if (opts.'dl-upcoming-days') {
        int numDays = opts.'dl-upcoming-days' as int
        upcomingCutoff = today.plusDays(numDays) }

    /// Parse the additive options first.
    if (opts.'dl-scheduled') { visibleSections << 'scheduled' }
    if (opts.'dl-due') { visibleSections << 'due' }
    if (opts.'dl-upcoming') { visibleSections << 'upcoming' }
    if (opts.'dl-open') { visibleSections << 'open' }

    /// If the user did not add any sections assume they want them all.
    if (visibleSections.size() == 0) {
        visibleSections = ['scheduled', 'due', 'upcoming', 'open'] }

    /// Now go through the negative options.
    if (opts.'dl-hide-scheduled') { visibleSections -= 'scheduled' }
    if (opts.'dl-hide-due') { visibleSections -= 'due' }
    if (opts.'dl-hide-upcoming') { visibleSections -= 'upcoming' }
    if (opts.'dl-hide-open') { visibleSections -= 'open' }

    /// If the user did not specifically ask for a status filter, we want a
    /// different default filter when we are doing a daily list.
    if (!opts.s) { filter.status = [Status.NEW, Status.VALIDATION_REQUIRED] }

    /// If the user did not give a specific sorting order, define our own: due
    /// date, then priority, then id.
    if (!opts.o) { filter.issueSorter = [ {it.due}, {it.priority}, {it.id} ] }

    /// #### Get all the issues involved.
    def allIssues = opts.R ?
        /// If `-R` passed, get all issues, including subprojects.
        issuedb.getAllIssues(filter) :
        /// Otherwise, just use the issues for this project.
        issuedb.issues.values().findAll { filter ? filter.accept(it) : true }

    /// We are going to sort the issues into these buckets based on when they are
    /// scheduled, when they are due and if they have a reminder set. 
    def scheduledToday = []
    def dueToday = []
    def upcoming = []
    def notDueOrReminder = []

    /// Helper closure to print an issue.
    def printIssue = { issue ->
        if (issue.due) println "${issue.due.toString('EEE, MM/dd')} -- ${issue}"
        else println "           -- ${issue}" }

    /// A sorter which sorts by date first, then by priority.
    def priorityDateSorter = { i1, i2 ->
        if (i1.priority == i2.priority) {
            def d1 = i1.due ?: new DateTime()
            def d2 = i2.due ?: new DateTime()

            return d1.compareTo(d2) }
        else { return i1.priority - i2.priority }}
            
    /// #### Categorize and sort the issues.
    /// Sort the issues into seperate lists based on their due dates and
    /// reminders.
    allIssues.each { issue ->
        /// * Find the issues that are scheduled for today.
        if (issue.scheduled && issue.scheduled < tomorrow) {
            scheduledToday << issue }

        /// * Find the issues that are due today or are past due.
        else if (issue.due && issue.due < tomorrow) { dueToday << issue }

        /// * Find the issues that are not yet due but have a reminder for
        ///   today or days past as well as issues that are due before the
        ///   `upcomingCutoff` date.
        else if ((issue.reminder && issue.reminder < tomorrow) ||
                 (issue.due && issue.due < upcomingCutoff)) {
            upcoming << issue }

        /// * All the others (not due and no reminder).
        else notDueOrReminder << issue }

    /// #### Print the issues
    if (visibleSections.contains('scheduled') && scheduledToday.size() > 0) {
        println "Tasks Scheduled for Today"
        println "-------------------------"

        scheduledToday.sort(priorityDateSorter).each { printIssue(it) }

        println "" }

    if (visibleSections.contains('due') && dueToday.size() > 0) {
        println "Tasks Due Today"
        println "---------------"

        dueToday.sort(priorityDateSorter).each { printIssue(it) }

        println ""}

    if (visibleSections.contains('upcoming') && upcoming.size() > 0) {
        println "Upcoming Tasks"
        println "--------------"

        upcoming.sort(priorityDateSorter).each { printIssue(it) }

        println ""}

    if (visibleSections.contains('open') && notDueOrReminder.size() > 0) {
        println "Other Open Issues"
        println "-----------------"

        notDueOrReminder.sort(priorityDateSorter).each { printIssue(it) }

        println "" }}

/// ### Create a New Issue.
else if (opts.n) {

    log.trace("Creating a new issue.")

    Issue issue
    def sin = System.in.newReader()

    /// Set the created extended property.
    assignOpts.created = new DateTime()

    /// Prompt for the different options if they were not given on the command
    /// line. We will loop until they have entered a valid value. How it works: 
    /// In the body of the loop we will try to read the input, parse it and
    /// assign it to a variable. If the input is invalid it will throw an
    /// exception before the assignment happens, the variable will still be
    /// null, and we will prompt the user again.

    /// Prompt for category.
    while(!assignOpts.category) {
        try {
            print "Category (bug, feature, task): "
            assignOpts.category = Category.toCategory(sin.readLine())
            break }
        catch (e) {
            println "Invalid category: " + e.getLocalizedMessage()
            println "Valid options are: \n${Category.values().join(', ')}"
            println " (abbreviations are accepted)." } }

    /// Prompt for the priority.
    while (!assignOpts.priority) {
        try {
            print "Priority (0-9): "
            assignOpts.priority = max(0, min(9, sin.readLine().toInteger()))
            break }
        catch (e) { println "Not a valid value." } }

    /// Prompt for the issue title. No need to loop as the input does not need
    /// to be validated.
    if (!assignOpts.title) {
        println "Issue title: "
        assignOpts.title = sin.readLine().trim() }

    /// Prompt for the issue text.
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

    /// Create the issue.
    issue = issuedb.createNewIssue(assignOpts)
    
    println "New issue created: "
    println issue }
    
/// ### Change Existing Issues.
else if (assignOpts.size() > 0) {

    log.trace("Changing existing issues.")

    /// We are going to add some extra properties if the status is being changed,
    /// because we are nice like that.
    if (assignOpts.status) { switch (assignOpts.status)  {
        case Status.RESOLVED: assignOpts.resolved = new DateTime(); break
        case Status.REJECTED: assignOpts.rejected = new DateTime(); break
        default: break }}

    /// #### processIssue
    /// A local function to handle the changes for one issue.
    def processIssue = { issue ->
        println issue
        /// Walk the assigned options map and set the properties on the issue.
        assignOpts.each { propName, value ->
            issue[propName] = value
            def formattedValue = ExtendedPropertyHelp.format(value)
            println "  set ${propName} to ${formattedValue}" } }

    /// If the user passed `-R`, walk the whole project, including subprojects.
    if (opts.R) { issuedb.walkProject(filter, processIssue) }
    /// Otherwise, just process the issues in this project.
    else {
        issuedb.issues.values()
            .findAll { filter ? filter.accept(it) : true }
            .each(processIssue) }}
/// ### Invalid Input
else {
    log.trace("Unknown request.")
    cli.usage(); return -1 }}
