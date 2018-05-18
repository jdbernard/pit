## Personal Issue Tracker CLI interface
## ====================================

import cliutils, docopt, json, logging, options, os, ospaths, sequtils,
  tables, terminal, times, timeutils, unicode, uuids

import strutils except capitalize, toUpper, toLower
import pitpkg/private/libpit
export libpit

include "pitpkg/private/version.nim"

type
  CliContext = ref object
    autoList, triggerPtk, verbose: bool
    cfg*: PitConfig
    contexts*: TableRef[string, string]
    defaultContext*, tasksDir*: string
    issues*: TableRef[IssueState, seq[Issue]]
    termWidth*: int

proc initContext(args: Table[string, Value]): CliContext =
  let pitCfg = loadConfig(args)

  let cliJson =
    if pitCfg.cfg.json.hasKey("cli"): pitCfg.cfg.json["cli"]
    else: newJObject()

  let cliCfg = CombinedConfig(docopt: args, json: cliJson)

  result = CliContext(
    autoList: cliJson.getOrDefault("autoList").getBool(false),
    contexts: pitCfg.contexts,
    defaultContext: cliJson.getOrDefault("defaultContext").getStr(""),
    verbose: parseBool(cliCfg.getVal("verbose", "false")) and not args["--quiet"],
    issues: newTable[IssueState, seq[Issue]](),
    tasksDir: pitCfg.tasksDir,
    termWidth: parseInt(cliCfg.getVal("term-width", "80")),
    triggerPtk: cliJson.getOrDefault("triggerPtk").getBool(false))

proc getIssueContextDisplayName(ctx: CliContext, context: string): string =
  if not ctx.contexts.hasKey(context):
    if context.isNilOrWhitespace: return "<default>"
    else: return context.capitalize()
  return ctx.contexts[context]

proc writeIssue(ctx: CliContext, issue: Issue, width: int, indent = "",
                verbose = false, topPadded = false) =
  var showDetails = not issue.details.isNilOrWhitespace and verbose

  if showDetails and not topPadded: stdout.writeLine("")

  # Wrap and write the summary.
  var wrappedSummary = (" ".repeat(5) & issue.summary).wordWrap(width - 2).indent(2 + indent.len)
  wrappedSummary = wrappedSummary[(6 + indent.len)..^1]
  stdout.setForegroundColor(fgBlack, true)
  stdout.write(indent & ($issue.id)[0..<6])
  stdout.setForegroundColor(fgWhite, false)
  stdout.write(wrappedSummary)

  if issue.tags.len > 0:
    stdout.setForegroundColor(fgGreen, false)
    let tagsStr = "(" & issue.tags.join(",") & ")"
    if (wrappedSummary.splitLines[^1].len + tagsStr.len + 1) < (width - 2):
      stdout.writeLine(" " & tagsStr)
    else:
      stdout.writeLine("\n" & indent & "  " & tagsStr)
  else: stdout.writeLine("")

  if issue.hasProp("pending"):
    let startIdx = "Pending: ".len
    var pendingText = issue["pending"].wordWrap(width - startIdx - 2)
                                      .indent(startIdx)
    pendingText = ("Pending: " & pendingText[startIdx..^1]).indent(indent.len + 2)
    stdout.setForegroundColor(fgCyan, false)
    stdout.writeLine(pendingText)

  if showDetails:
    stdout.setForegroundColor(fgCyan, false)
    stdout.writeLine(issue.details.indent(indent.len + 2))

  stdout.resetAttributes

proc writeSection(ctx: CliContext, issues: seq[Issue], state: IssueState,
                   indent = "", verbose = false) =
  let innerWidth = ctx.termWidth - (indent.len * 2)

  stdout.setForegroundColor(fgBlue, true)
  stdout.writeLine(indent & ".".repeat(innerWidth))
  stdout.writeLine(state.displayName.center(ctx.termWidth))
  stdout.writeLine("")
  stdout.resetAttributes

  let issuesByContext = issues.groupBy("context")

  var topPadded = true

  if issues.len > 5 and issuesByContext.len > 1:
    for context, ctxIssues in issuesByContext:
      topPadded = true
      stdout.setForegroundColor(fgYellow, false)
      stdout.writeLine(indent & ctx.getIssueContextDisplayName(context) & ":")
      stdout.writeLine("")
      stdout.resetAttributes

      for i in ctxIssues:
        ctx.writeIssue(i, innerWidth - 2, indent & "  ", verbose, topPadded)
        topPadded = not i.details.isNilOrWhitespace and verbose

      if not topPadded: stdout.writeLine("")

  else:
    for i in issues:
      ctx.writeIssue(i, innerWidth, indent, verbose, topPadded)
      topPadded = not i.details.isNilOrWhitespace and verbose

  stdout.writeLine("")

proc loadIssues(ctx: CliContext, state: IssueState) =
  ctx.issues[state] = loadIssues(ctx.tasksDir / $state)

proc loadAllIssues(ctx: CliContext) =
  ctx.issues = newTable[IssueState, seq[Issue]]()
  for state in IssueState: ctx.loadIssues(state)

proc filterIssues(ctx: CliContext, filter: IssueFilter) =
  for state, issueList in ctx.issues:
    ctx.issues[state] = issueList.filter(filter)

proc parsePropertiesOption(propsOpt: string): TableRef[string, string] =
  result = newTable[string, string]()
  for propText in propsOpt.split(";"):
    let pair = propText.split(":", 1)
    if pair.len == 1: result[pair[0]] = "true"
    else: result[pair[0]] = pair[1]

proc sameDay(a, b: DateTime): bool =
  result = a.year == b.year and a.yearday == b.yearday

proc writeHeader(ctx: CliContext, header: string) =
  stdout.setForegroundColor(fgRed, true)
  stdout.writeLine('_'.repeat(ctx.termWidth))
  stdout.writeLine(header.center(ctx.termWidth))
  stdout.writeLine('~'.repeat(ctx.termWidth))
  stdout.resetAttributes

proc edit(issue: Issue) =

  # Write format comments (to help when editing)
  writeFile(issue.filepath, toStorageFormat(issue, true))

  let editor =
    if existsEnv("EDITOR"): getEnv("EDITOR")
    else: "vi"

  discard os.execShellCmd(editor & " " & issue.filepath & " </dev/tty >/dev/tty")

  try:
    # Try to parse the newly-edited issue to make sure it was successful.
    let editedIssue = loadIssue(issue.filepath)
    editedIssue.store()
  except:
    fatal "pit: updated issue is invalid (ignoring edits): \n\t" &
      getCurrentExceptionMsg()
    issue.store()

proc list(ctx: CliContext, filter: Option[IssueFilter], state: Option[IssueState], today, future, verbose: bool) =

  if state.isSome:
    ctx.loadIssues(state.get)
    if filter.isSome: ctx.filterIssues(filter.get)
    ctx.writeSection(ctx.issues[state.get], state.get, "", verbose)
    return

  ctx.loadAllIssues()
  if filter.isSome: ctx.filterIssues(filter.get)

  let indent = if today and future: "  " else: ""

  # Today's items
  if today:
    if future: ctx.writeHeader("Today")

    for s in [Current, TodoToday]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        ctx.writeSection(ctx.issues[s], s, indent, verbose)

    if ctx.issues.hasKey(Done):
        let doneIssues = ctx.issues[Done].filterIt(
          it.hasProp("completed") and
          sameDay(getTime().local, it.getDateTime("completed")))
        if doneIssues.len > 0:
          ctx.writeSection(doneIssues, Done, indent, verbose)

  # Future items
  if future:
    if today: ctx.writeHeader("Future")

    for s in [Pending, Todo]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        ctx.writeSection(ctx.issues[s], s, indent, verbose)

when isMainModule:

 try:
  let doc = """
Usage:
  pit ( new | add) <summary> [<state>] [options]
  pit list [<listable>] [options]
  pit ( start | done | pending | do-today | todo | suspend ) <id>... [options]
  pit edit <id>
  pit ( delete | rm ) <id>...

Options:

  -h, --help                Print this usage information.

  -p, --properties <props>  Specify properties. Formatted as "key:val;key:val"
                            When used with the list command this option applies
                            a filter to the issues listed, only allowing those
                            which have all of the given properties.

  -c, --context <ctxName>   Shorthand for '-p context:<ctxName>'

  -g, --tags <tags>         Specify tags for an issue.

  -T, --today               Limit to today's issues.

  -F, --future              Limit to future issues.

  -v, --verbose             Show issue details when listing issues.

  -q, --quiet               Suppress verbose output.

  -y, --yes                 Automatically answer "yes" to any prompts.

  -C, --config <cfgFile>    Location of the config file (defaults to $HOME/.pitrc)

  -E, --echo-args           Echo arguments (for debug purposes).

  -d, --tasks-dir           Path to the tasks directory (defaults to the value
                            configured in the .pitrc file)

  --term-width <width>      Manually set the terminal width to use.
"""

  logging.addHandler(newConsoleLogger())

  # Parse arguments
  let args = docopt(doc, version = PIT_VERSION)

  if args["--echo-args"]: stderr.writeLine($args)

  if args["--help"]:
    stderr.writeLine(doc)
    quit()

  let ctx = initContext(args)

  var propertiesOption = none(TableRef[string,string])

  if args["--properties"] or args["--context"] or
     not ctx.defaultContext.isNilOrWhitespace:

    var props =
      if args["--properties"]: parsePropertiesOption($args["--properties"])
      else: newTable[string,string]()

    if args["--context"] and $args["--context"] != "all":
      props["context"] = $args["--context"]
    elif not args["--context"] and not ctx.defaultContext.isNilOrWhitespace:
      stderr.writeLine("Limiting to default context: " & ctx.defaultContext)
      props["context"] = ctx.defaultContext

    propertiesOption = some(props)

  ## Actual command runners
  if args["new"] or args["add"]:
    let state =
      if args["<state>"]: parseEnum[IssueState]($args["<state>"])
      else: TodoToday

    var issueProps = propertiesOption.get(newTable[string,string]())
    if not issueProps.hasKey("created"): issueProps["created"] = getTime().local.formatIso8601

    var issue = Issue(
      id: genUUID(),
      summary: $args["<summary>"],
      properties: issueProps,
      tags:
        if args["--tags"]: ($args["--tags"]).split(",").mapIt(it.strip)
        else: newSeq[string]())

    ctx.tasksDir.store(issue, state)

  elif args["edit"]:
    let issueId = $args["<id>"]

    edit(ctx.tasksDir.loadIssueById(issueId))

  elif args["start"] or args["do-today"] or args["done"] or
       args["pending"] or args["todo"] or args["suspend"]:

    var targetState: IssueState
    if args["done"]: targetState = Done
    elif args["do-today"]: targetState = TodoToday
    elif args["pending"]: targetState = Pending
    elif args["start"]: targetState = Current
    elif args["todo"]: targetState = Todo
    elif args["suspend"]: targetState = Dormant

    for id in @(args["<id>"]):
      var issue = ctx.tasksDir.loadIssueById(id)
      if propertiesOption.isSome:
        for k,v in propertiesOption.get:
          issue[k] = v
      issue.changeState(ctx.tasksDir, targetState)

    if ctx.triggerPtk:
      if targetState == Current:
        let issue = ctx.tasksDir.loadIssueById($(args["<id>"][0]))
        var cmd = "ptk start "
        if issue.tags.len > 0: cmd &= "-g \"" & issue.tags.join(",") & "\""
        cmd &= " \"" & issue.summary & "\""
        discard execShellCmd(cmd)
      elif targetState == Done or targetState == Pending:
        discard execShellCmd("ptk stop")

  elif args["delete"] or args["rm"]:
    for id in @(args["<id>"]):

      let issue = ctx.tasksDir.loadIssueById(id)

      if not args["--yes"]:
        stderr.write("Delete '" & issue.summary & "' (y/n)? ")
        if not "yes".startsWith(stdin.readLine.toLower):
          continue

      issue.delete

  elif args["list"]:

    let filter = initFilter()
    var filterOption = none(IssueFilter)
    if propertiesOption.isSome:
      filter.properties = propertiesOption.get
      filterOption = some(filter)

    var stateOption = none(IssueState)
    var issueIdOption = none(string)
    if args["<listable>"]:
      try: stateOption = some(parseEnum[IssueState]($args["<listable>"]))
      except: issueIdOption = some($args["<listable>"])

    # List a specific issue
    if issueIdOption.isSome:
      let issue = ctx.tasksDir.loadIssueById(issueIdOption.get)
      ctx.writeIssue(issue, ctx.termWidth, "", true, true)

    # List all issues
    else:
      let showBoth = args["--today"] == args["--future"]
      ctx.list(filterOption, stateOption, showBoth or args["--today"],
                                          showBoth or args["--future"],
                                          ctx.verbose)

  if ctx.autoList and not args["list"]:
    ctx.loadAllIssues()
    ctx.list(none(IssueFilter), none(IssueState), true, true, false)

 except:
  fatal "pit: " & getCurrentExceptionMsg()
  #raise getCurrentException()
  quit(QuitFailure)
