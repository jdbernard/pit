## Personal Issue Tracker
## ======================
##

import cliutils, docopt, json, logging, options, os, ospaths, sequtils,
  tables, terminal, times, unicode, uuids

import strutils except capitalize, toUpper, toLower
import pitpkg/private/libpit
export libpit

type
  CliContext = ref object
    autoList, triggerPtk: bool
    tasksDir*: string
    contexts*: TableRef[string, string]
    issues*: TableRef[IssueState, seq[Issue]]
    termWidth*: int

proc initContext(args: Table[string, Value]): CliContext =
  let pitrcLocations = @[
    if args["--config"]: $args["--config"] else: "",
    ".pitrc", $getEnv("PITRC"), $getEnv("HOME") & "/.pitrc"]

  var pitrcFilename: string =
    foldl(pitrcLocations, if len(a) > 0: a elif existsFile(b): b else: "")

  if not existsFile(pitrcFilename):
    warn "pit: could not find .pitrc file: " & pitrcFilename
    if isNilOrWhitespace(pitrcFilename):
      pitrcFilename = $getEnv("HOME") & "/.pitrc"
    var cfgFile: File
    try:
      cfgFile = open(pitrcFilename, fmWrite)
      cfgFile.write("{\"tasksDir\": \"/path/to/tasks\"}")
    except: warn "pit: could not write default .pitrc to " & pitrcFilename
    finally: close(cfgFile)

  var cfgJson: JsonNode
  try: cfgJson = parseFile(pitrcFilename)
  except: raise newException(IOError,
    "unable to read config file: " & pitrcFilename &
    "\x0D\x0A" & getCurrentExceptionMsg())

  let cfg = CombinedConfig(docopt: args, json: cfgJson)

  result = CliContext(
    autoList: cfgJson.getOrDefault("autoList").getBool(false),
    contexts: newTable[string,string](),
    issues: newTable[IssueState, seq[Issue]](),
    tasksDir: cfg.getVal("tasks-dir", ""),
    termWidth: parseInt(cfg.getVal("term-width", "80")),
    triggerPtk: cfgJson.getOrDefault("triggerPtk").getBool(false))

  if cfgJson.hasKey("contexts"):
    for k, v in cfgJson["contexts"]:
      result.contexts[k] = v.getStr()

  if isNilOrWhitespace(result.tasksDir):
    raise newException(Exception, "no tasks directory configured")

  if not existsDir(result.tasksDir):
    raise newException(Exception, "cannot find tasks dir: " & result.tasksDir)

proc getIssueContextDisplayName(ctx: CliContext, context: string): string =
  if not ctx.contexts.hasKey(context):
    if context.isNilOrWhitespace: return "<default>"
    else: return context.capitalize()
  return ctx.contexts[context]

proc writeIssue(ctx: CliContext, issue: Issue, state: IssueState,
                 width: int, indent: string, topPadded: bool) =
  var showDetails = not issue.details.isNilOrWhitespace

  if showDetails and not topPadded: stdout.writeLine("")

  # Wrap and write the summary.
  var wrappedSummary = (" ".repeat(5) & issue.summary).wordWrap(width - 2).indent(2 + indent.len)
  wrappedSummary = wrappedSummary[(6 + indent.len)..^1]
  stdout.setForegroundColor(fgBlack, true)
  stdout.write(indent & ($issue.id)[0..<6])
  stdout.setForegroundColor(fgCyan, false)
  stdout.write(wrappedSummary)

  if issue.tags.len > 0:
    stdout.setForegroundColor(fgGreen, false)
    let tagsStr = "(" & issue.tags.join(",") & ")"
    if (wrappedSummary.splitLines[^1].len + tagsStr.len + 1) < (width - 2):
      stdout.writeLine(" " & tagsStr)
    else:
      stdout.writeLine("\n" & indent & "  " & tagsStr)
  else: stdout.writeLine("")
  stdout.resetAttributes

  if state == Pending and issue.hasProp("pending"):
    let startIdx = "Pending: ".len
    var pendingText = issue["pending"].wordWrap(width - startIdx - 2)
                                      .indent(startIdx)
    pendingText = ("Pending: " & pendingText[startIdx..^1]).indent(indent.len + 2)
    stdout.writeLine(pendingText)

  if showDetails: stdout.writeLine(issue.details.indent(indent.len + 2))


proc writeSection(ctx: CliContext, issues: seq[Issue], state: IssueState,
                   indent = "") =
  let innerWidth = ctx.termWidth - (indent.len * 2)

  stdout.setForegroundColor(fgBlue, true)
  stdout.writeLine(indent & ".".repeat(innerWidth))
  stdout.writeLine(state.displayName.center(ctx.termWidth))
  stdout.writeLine("")
  stdout.resetAttributes

  var topPadded = true

  let issuesByContext = issues.groupBy("context")

  if issues.len > 5 and issuesByContext.len > 1:
    for context, ctxIssues in issuesByContext:
      stdout.setForegroundColor(fgYellow, false)
      stdout.writeLine(indent & ctx.getIssueContextDisplayName(context) & ":")
      stdout.writeLine("")
      stdout.resetAttributes

      for i in ctxIssues:
        ctx.writeIssue(i, state, innerWidth - 2, indent & "  ", topPadded)
        topPadded = not i.details.isNilOrWhitespace

      if not topPadded: stdout.writeLine("")

  else:
    for i in issues:
      ctx.writeIssue(i, state, innerWidth, indent, topPadded)
      topPadded = not i.details.isNilOrWhitespace

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

proc list(ctx: CliContext, filter: Option[IssueFilter], state: Option[IssueState], today, future: bool) =

  if state.isSome:
    ctx.loadIssues(state.get)
    if filter.isSome: ctx.filterIssues(filter.get)
    ctx.writeSection(ctx.issues[state.get], state.get)
    return

  ctx.loadAllIssues()
  if filter.isSome: ctx.filterIssues(filter.get)

  let indent = if today and future: "  " else: ""

  # Today's items
  if today:
    if future: ctx.writeHeader("Today")

    for s in [Current, TodoToday]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        ctx.writeSection(ctx.issues[s], s, indent)

    if ctx.issues.hasKey(Done):
        let doneIssues = ctx.issues[Done].filterIt(
          it.hasProp("completed") and
          sameDay(getTime().local, it.getDateTime("completed")))
        if doneIssues.len > 0:
          ctx.writeSection(doneIssues, Done, indent)

  # Future items
  if future:
    if today: ctx.writeHeader("Future")

    for s in [Pending, Todo]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        ctx.writeSection(ctx.issues[s], s, indent)

when isMainModule:

 try:
  let doc = """
Usage:
  pit ( new | add) <summary> [<state>] [options]
  pit list [<state>] [options]
  pit ( start | done | pending | do-today | todo ) <id>...
  pit edit <id>
  pit delete <id>...

Options:

  -h, --help                Print this usage information.

  -t, --tags <tags>         Specify tags for an issue.

  -p, --properties <props>  Specify properties. Formatted as "key:val;key:val"
                            When used with the list command this option applies
                            a filter to the issues listed, only allowing those
                            which have all of the given properties.

  -T, --today               Limit to today's issues.

  -F, --future              Limit to future issues.

  -y, --yes                 Automatically answer "yes" to any prompts.

  -C, --config <cfgFile>    Location of the config file (defaults to $HOME/.pitrc)

  -E, --echo-args           Echo arguments (for debug purposes).

  --tasks-dir               Path to the tasks directory (defaults to the value
                            configured in the .pitrc file)

  --term-width              Manually set the terminal width to use.
"""

  logging.addHandler(newConsoleLogger())

  # Parse arguments
  let args = docopt(doc, version = "pit 4.0.2")

  if args["--echo-args"]: stderr.writeLine($args)

  if args["--help"]:
    stderr.writeLine(doc)
    quit()

  let ctx = initContext(args)

  ## Actual command runners
  if args["new"] or args["add"]:
    let state =
      if args["<state>"]: parseEnum[IssueState]($args["<state>"])
      else: TodoToday

    var issue = Issue(
      id: genUUID(),
      summary: $args["<summary>"],
      properties:
        if args["--properties"]: parsePropertiesOption($args["--properties"])
        else: newTable[string,string](),
      tags:
        if args["--tags"]: ($args["tags"]).split(",").mapIt(it.strip)
        else: newSeq[string]())

    ctx.tasksDir.store(issue, state)

  elif args["edit"]:
    let issueId = $args["<id>"]

    edit(ctx.tasksDir.loadIssueById(issueId))

  elif args["start"] or args["do-today"] or args["done"] or
       args["pending"] or args["todo"]:

    var targetState: IssueState
    if args["done"]: targetState = Done
    elif args["do-today"]: targetState = TodoToday
    elif args["pending"]: targetState = Todo
    elif args["start"]: targetState = Current
    elif args["todo"]: targetState = Todo

    for id in @(args["<id>"]):
      ctx.tasksDir.loadIssueById(id).changeState(ctx.tasksDir, targetState)

    if ctx.triggerPtk:
      if targetState == Current:
        let issue = ctx.tasksDir.loadIssueById($(args["<id>"][0]))
        var cmd = "ptk start "
        if issue.tags.len > 0: cmd &= "-g \"" & issue.tags.join(",") & "\""
        cmd &= " \"" & issue.summary & "\""
        discard execShellCmd(cmd)
      elif targetState == Done: discard execShellCmd("ptk stop")

  elif args["delete"]:
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
    if args["--properties"]:
      filter.properties = parsePropertiesOption($args["--properties"])
      filterOption = some(filter)

    let stateOption =
      if args["<state>"]: some(parseEnum[IssueState]($args["<state>"]))
      else: none(IssueState)

    let showBoth = args["--today"] == args["--future"]
    ctx.list(filterOption, stateOption, showBoth or args["--today"],
                                             showBoth or args["--future"])

  if ctx.autoList and not args["list"]:
    ctx.loadAllIssues()
    ctx.list(none(IssueFilter), none(IssueState), true, true)

 except:
  fatal "pit: " & getCurrentExceptionMsg()
  #raise getCurrentException()
  quit(QuitFailure)
