## Personal Issue Tracker CLI interface
## ====================================

import cliutils, docopt, json, logging, options, os, ospaths, sequtils,
  tables, terminal, times, timeutils, unicode, uuids

import strutils except capitalize, toUpper, toLower
import pitpkg/private/libpit
export libpit

include "pitpkg/version.nim"

type
  CliContext = ref object
    cfg*: PitConfig
    contexts*: TableRef[string, string]
    defaultContext*: Option[string]
    tasksDir*: string
    issues*: TableRef[IssueState, seq[Issue]]
    termWidth*: int
    triggerPtk*, verbose*: bool

proc initContext(args: Table[string, Value]): CliContext =
  let pitCfg = loadConfig(args)

  let cliJson =
    if pitCfg.cfg.json.hasKey("cli"): pitCfg.cfg.json["cli"]
    else: newJObject()

  let cliCfg = CombinedConfig(docopt: args, json: cliJson)

  result = CliContext(
    contexts: pitCfg.contexts,
    defaultContext:
      if not cliJson.hasKey("defaultContext"): none(string)
      else: some(cliJson["defaultContext"].getStr()),
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

proc formatIssue(ctx: CliContext, issue: Issue): string =
  result = ($issue.id).withColor(fgBlack, true) & "\n"&
           issue.summary.withColor(fgWhite) & "\n"

  if issue.tags.len > 0:
    result &= "tags: ".withColor(fgMagenta) &
              issue.tags.join(",").withColor(fgGreen, true) & "\n"

  if issue.properties.len > 0:
    result &= termColor(fgMagenta)
    for k, v in issue.properties: result &= k & ": " & v & "\n"


  result &= "--------".withColor(fgBlack, true) & "\n"
  if not issue.details.isNilOrWhitespace:
    result &= issue.details.strip.withColor(fgCyan) & "\n"

proc formatSectionIssue(ctx: CliContext, issue: Issue, width: int, indent = "",
                verbose = false): string =

  result = ""

  var showDetails = not issue.details.isNilOrWhitespace and verbose

  # Wrap and write the summary.
  var wrappedSummary = (" ".repeat(5) & issue.summary).wordWrap(width - 2).indent(2 + indent.len)
  wrappedSummary = wrappedSummary[(6 + indent.len)..^1]

  result = (indent & ($issue.id)[0..<6]).withColor(fgBlack, true)
  result &= wrappedSummary.withColor(fgWhite)

  if issue.tags.len > 0:
    let tagsStr = "(" & issue.tags.join(", ") & ")"
    if (result.splitLines[^1].len + tagsStr.len + 1) > (width - 2):
      result &= "\n" & indent
    result &= " " & tagsStr.withColor(fgGreen)


  if issue.hasProp("pending"):
    let startIdx = "Pending: ".len
    var pendingText = issue["pending"].wordWrap(width - startIdx - 2)
                                      .indent(startIdx)
    pendingText = ("Pending: " & pendingText[startIdx..^1]).indent(indent.len + 2)
    result &= "\n" & pendingText.withColor(fgCyan)

  if showDetails:
    result &= "\n" & issue.details.strip.indent(indent.len + 2).withColor(fgCyan)

  result &= termReset

proc formatSectionIssueList(ctx: CliContext, issues: seq[Issue], width: int,
        indent: string, verbose: bool): string =

  result = ""
  var topPadded = true
  for i in issues:
    var issueText = ctx.formatSectionIssue(i, width, indent, verbose)
    if issueText.splitLines.len > 1:
      if topPadded: result &= issueText & "\n\n"
      else: result &= "\n" & issueText & "\n\n"
      topPadded = true
    else:
      result &= issueText & "\n"
      topPadded = false

proc formatSection(ctx: CliContext, issues: seq[Issue], state: IssueState,
                   indent = "", verbose = false): string =
  let innerWidth = ctx.termWidth - (indent.len * 2)

  result = termColor(fgBlue) &
    (indent & ".".repeat(innerWidth)) & "\n" &
    state.displayName.center(ctx.termWidth) & "\n\n" &
    termReset

  let issuesByContext = issues.groupBy("context")

  if issues.len > 5 and issuesByContext.len > 1:
    for context, ctxIssues in issuesByContext:

      result &= termColor(fgYellow) &
        indent & ctx.getIssueContextDisplayName(context) & ":" &
        termReset & "\n\n"

      result &= ctx.formatSectionIssueList(ctxIssues, innerWidth - 2, indent & "  ", verbose)
      result &= "\n"

  else: result &= ctx.formatSectionIssueList(issues, innerWidth, indent, verbose)

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
    stdout.write ctx.formatSection(ctx.issues[state.get], state.get, "", verbose)
    return

  ctx.loadAllIssues()
  if filter.isSome: ctx.filterIssues(filter.get)

  let indent = if today and future: "  " else: ""

  # Today's items
  if today:
    if future: ctx.writeHeader("Today")

    for s in [Current, TodoToday]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        stdout.write ctx.formatSection(ctx.issues[s], s, indent, verbose)

    if ctx.issues.hasKey(Done):
        let doneIssues = ctx.issues[Done].filterIt(
          it.hasProp("completed") and
          sameDay(getTime().local, it.getDateTime("completed")))
        if doneIssues.len > 0:
          stdout.write ctx.formatSection(doneIssues, Done, indent, verbose)

  # Future items
  if future:
    if today: ctx.writeHeader("Future")

    for s in [Pending, Todo]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        stdout.write ctx.formatSection(ctx.issues[s], s, indent, verbose)

when isMainModule:

 try:
  let doc = """
Usage:
  pit ( new | add) <summary> [<state>] [options]
  pit list [<listable>] [options]
  pit ( start | done | pending | todo-today | todo | suspend ) <id>... [options]
  pit edit <ref>...
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
  var tagsOption = none(seq[string])

  if args["--properties"] or args["--context"]:

    var props =
      if args["--properties"]: parsePropertiesOption($args["--properties"])
      else: newTable[string,string]()

    if args["--context"]: props["context"] = $args["--context"]

    propertiesOption = some(props)

  if args["--tags"]: tagsOption = some(($args["--tags"]).split(",").mapIt(it.strip))

  ## Actual command runners
  if args["new"] or args["add"]:
    let state =
      if args["<state>"]: parseEnum[IssueState]($args["<state>"])
      else: TodoToday

    var issueProps = propertiesOption.get(newTable[string,string]())
    if not issueProps.hasKey("created"): issueProps["created"] = getTime().local.formatIso8601
    if not issueProps.hasKey("context") and ctx.defaultContext.isSome():
      stderr.writeLine("Using default context: " & ctx.defaultContext.get)
      issueProps["context"] = ctx.defaultContext.get

    var issue = Issue(
      id: genUUID(),
      summary: $args["<summary>"],
      properties: issueProps,
      tags:
        if args["--tags"]: ($args["--tags"]).split(",").mapIt(it.strip)
        else: newSeq[string]())

    ctx.tasksDir.store(issue, state)

    stdout.writeLine ctx.formatIssue(issue)

  elif args["edit"]:
    for editRef in @(args["<ref>"]):

      var stateOption = none(IssueState)

      try: stateOption = some(parseEnum[IssueState](editRef))
      except: discard

      if stateOption.isSome:
        let state = stateOption.get
        ctx.loadIssues(state)
        for issue in ctx.issues[state]: edit(issue)

      else: edit(ctx.tasksDir.loadIssueById(editRef))

  elif args["start"] or args["todo-today"] or args["done"] or
       args["pending"] or args["todo"] or args["suspend"]:

    var targetState: IssueState
    if args["done"]: targetState = Done
    elif args["todo-today"]: targetState = TodoToday
    elif args["pending"]: targetState = Pending
    elif args["start"]: targetState = Current
    elif args["todo"]: targetState = Todo
    elif args["suspend"]: targetState = Dormant

    for id in @(args["<id>"]):
      var issue = ctx.tasksDir.loadIssueById(id)
      if propertiesOption.isSome:
        for k,v in propertiesOption.get:
          issue[k] = v
      if targetState == Done: issue["completed"] = getTime().local.formatIso8601
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

    # Initialize filter with properties (if given)
    if propertiesOption.isSome:
      filter.properties = propertiesOption.get
      filterOption = some(filter)

    # If no "context" property is given, use the default (if we have one)
    if ctx.defaultContext.isSome and not filter.properties.hasKey("context"):
      stderr.writeLine("Limiting to default context: " & ctx.defaultContext.get)
      filter.properties["context"] = ctx.defaultContext.get
      filterOption = some(filter)

    # Finally, if the "context" is "all", don't filter on context
    if filter.properties.hasKey("context") and
       filter.properties["context"] == "all":

      filter.properties.del("context")

    var listContexts = false
    var stateOption = none(IssueState)
    var issueIdOption = none(string)

    if args["<listable>"]:
      if $args["<listable>"] == "contexts": listContexts = true
      else:
        try: stateOption = some(parseEnum[IssueState]($args["<listable>"]))
        except: issueIdOption = some($args["<listable>"])

    # List the known contexts
    if listContexts:
      var uniqContexts = toSeq(ctx.contexts.keys)
      ctx.loadAllIssues()
      for state, issueList in ctx.issues:
        for issue in issueList:
          if issue.hasProp("context") and not uniqContexts.contains(issue["context"]):
            uniqContexts.add(issue["context"])

      for c in uniqContexts: stdout.writeLine(c)

    # List a specific issue
    elif issueIdOption.isSome:
      let issue = ctx.tasksDir.loadIssueById(issueIdOption.get)
      stdout.writeLine ctx.formatIssue(issue)

    # List all issues
    else:
      let showBoth = args["--today"] == args["--future"]
      ctx.list(filterOption, stateOption, showBoth or args["--today"],
                                          showBoth or args["--future"],
                                          ctx.verbose)

 except:
  fatal "pit: " & getCurrentExceptionMsg()
  #raise getCurrentException()
  quit(QuitFailure)
