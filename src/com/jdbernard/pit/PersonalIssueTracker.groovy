package com.jdbernard.pit

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
