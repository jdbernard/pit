## Personal Issue Tracker
## ======================
## 

import cliutils, docopt, json, logging, os, ospaths, sequtils, strutils,
  tables, times, uuids

import pit/private/libpit
export libpit

type
  CliContext = ref object
    tasksDir*: string
    contexts*: TableRef[string, string]
    issues*: TableRef[IssueState, seq[Issue]]
    cfg*: CombinedConfig

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
    cfg: cfg,
    tasksDir: cfg.getVal("tasks-dir", ""),
    contexts: newTable[string,string](),
    issues: newTable[IssueState, seq[Issue]]())

  if cfgJson.hasKey("contexts"):
    for k, v in cfgJson["contexts"]:
      result.contexts[k] = v.getStr()

  if isNilOrWhitespace(result.tasksDir):
    raise newException(Exception, "no tasks directory configured")

  if not existsDir(result.tasksDir):
    raise newException(Exception, "cannot find tasks dir: " & result.tasksDir)

proc getIssueContextDisplayName(ctx: CliContext, context: string): string =
  if not ctx.contexts.hasKey(context): return context.capitalize()
  return ctx.contexts[context]

proc formatIssue(ctx: CliContext, issue: Issue, state: IssueState,
                 width: int, indent: string, topPadded: bool): string =
  var showDetails = not issue.details.isNilOrWhitespace
  var lines: seq[string] = @[]

  if showDetails and not topPadded: lines.add("")

  var wrappedSummary = issue.summary.wordWrap(width - 2).indent(2)
  wrappedSummary = "*" & wrappedSummary[1..^1]
  lines.add(wrappedSummary.indent(indent.len))

  if state == Pending and issue.properties.hasKey("pending"):
    let startIdx = "Pending: ".len
    var pendingText = issue["pending"].wordWrap(width - startIdx - 2).indent(startIdx)
    pendingText = ("Pending: " & pendingText[startIdx..^1]).indent(indent.len + 2)
    lines.add(pendingText)

  if showDetails: lines.add(issue.details.indent(indent.len + 2))

  return lines.join("\n")

proc formatSection(ctx: CliContext, issues: seq[Issue], state: IssueState,
                   width = 80, indent = "  "): string =
  let innerWidth = width - (indent.len * 2)
  var lines: seq[string] = @[]

  lines.add(indent & ".".repeat(innerWidth))
  lines.add(state.displayName.center(width))
  lines.add("")

  var topPadded = true
  var showDetails = false

  let issuesByContext = issues.groupBy("context")

  if issues.len > 5 and issuesByContext.len > 1:
    for context, ctxIssues in issuesByContext:
      lines.add(indent & ctx.getIssueContextDisplayName(context) & ":")
      lines.add("")

      for i in ctxIssues:
        lines.add(ctx.formatIssue(i, state, innerWidth - 2, indent & "  ", topPadded))
        topPadded = not i.details.isNilOrWhitespace

      if not topPadded: lines.add("")

  else:
    for i in issues:
      lines.add(ctx.formatIssue(i, state, innerWidth, indent, topPadded))
      topPadded = not i.details.isNilOrWhitespace

  lines.add("")
  return lines.join("\n")

proc loadIssues(ctx: CliContext, state: IssueState): seq[Issue] =
  result = loadIssues(joinPath(ctx.tasksDir, $state))

proc loadAllIssues(ctx: CliContext) =
  for state in IssueState:
    ctx.issues[state] = loadIssues(ctx, state)

proc sameDay(a, b: DateTime): bool =
  result = a.year == b.year and a.yearday == b.yearday

when isMainModule:

 try:
  let doc = """
Usage:
  pit new <state> <summary> [options]
  pit list [<state>] [options]
  pit today
  pit start
  pit done
  pit pending
  pit edit

Options:

  -t, --tags <tags>         Specify tags for an issue.
  -p, --properties <props>  Specify properties for an issue. Formatted as "key:val;key:val"
  -C, --config <cfgFile>    Location of the config file (defaults to $HOME/.pitrc)
  -h, --help                Print this usage information.
  -T, --today               Limit to today's issues.
  -E, --echo-args           Echo arguments (for debug purposes).
  --tasks-dir               Path to the tasks directory (defaults to the value
                            configured in the .pitrc file)
"""

  logging.addHandler(newConsoleLogger())

  # Parse arguments
  let args = docopt(doc, version = "ptk 0.12.1")

  if args["--echo-args"]: echo $args

  if args["--help"]:
    echo doc
    quit()

  let now = getTime().local

  let ctx = initContext(args)

  ## Actual command runners
  if args["list"]:

    ctx.loadAllIssues()

    let fullWidth = 80
    let innerWidth = fullWidth - 4

    # Today's items
    echo '_'.repeat(fullWidth)
    echo "Today".center(fullWidth)
    echo '~'.repeat(fullWidth)
    echo ""

    for s in [Current, TodoToday]:
      echo ctx.formatSection(ctx.issues[s], s)

    echo ctx.formatSection(
      ctx.issues[Done].filterIt(
        it.properties.hasKey("completed") and
        sameDay(now, it.getDateTime("completed"))), Done)

    # Future items
    echo '_'.repeat(fullWidth)
    echo "Future".center(fullWidth)
    echo '~'.repeat(fullWidth)
    echo ""

    for s in [Pending, Todo]:
      echo ctx.formatSection(ctx.issues[s], s)

 except:
  fatal "pit: " & getCurrentExceptionMsg()
  #raise getCurrentException()
  quit(QuitFailure)
