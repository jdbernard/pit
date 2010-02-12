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
issuedb = buildDB(new File('.'),
    'categories': categories,
    'priority': (opts.p ? opts.p.toInteger() : 9),
    'projects': (opts.r ? opts.r.toLowerCase().split(/[,\s]/).asType(List.class) : false),
    'ids': (opts.i ? opts.i.split(/[,\s]/).asType(List.class) : false),
    'recurse': (opts.s || !opts.S))

listDB(issuedb, 'verbose': opts.v)

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

def buildDB(Map options, File dir) {
    if (!options.priority) options.priority = 9

    def newdb = ['projects':[:], 'issues':[:], 'name': dir.name]

    dir.eachFile { child ->

        // add sub projects
        if (child.isDirectory())  {
            if ( child.name ==~ /\d{4}/ ||  // just an issue folder
                !options.recurse ||         // we are not looking at subprojects
                (options.projects &&        // not in the list of sub
                !options.projects.contains(child.name.toLowerCase())))
                return

            // otherwise build and add to list
            newdb['projects'][(child.name)] =  buildDB(options, child)
        } else if (child.isFile()) {
            def issue = buildIssue(child)

            if ( issue == null ||                       // not an issue
                 issue.priority > options.priority ||   // not above threshold
                (options.categories &&                  // not in list of cats
                !options.categories.contains(issue.category)) ||
                (options.ids &&
                !options.ids.contains(issue.id)))
                return

            newdb['issues'][(issue.id)] = issue
        }
    }

    return newdb
}

def buildIssue(File file) {
    def issue = [:]

    def matcher = file.name =~ /(\d{4})([bftc])(\d).*/
    if (!matcher) return null

    issue.id = matcher[0][1]
    issue.category = Category.toCategory(matcher[0][2])
    issue.priority = matcher[0][3].toInteger()

    file.withReader { issue.title = it.readLine() }
    issue.text = file.text

    return issue
}

def listDB(Map options, def issuedb) {
    if (!options.offset) options.offset = ""
    if (!options.verbose) options.verbose = false

    for (i in issuedb.issues.values()) {
        println "${options.offset}${i.id}(${i.priority}): ${i.category} ${i.title}"
        if (options.verbose) println "\n${i.text}"
    }
    for (p in issuedb.projects.values())  {
        println ""
        println "${options.offset}${p.name}"
        println "${options.offset}${'-'.multiply(p.name.length())}"

        listDB(p, 'offset': options.offset + "  ", 'verbose': options.verbose)
    }
}
