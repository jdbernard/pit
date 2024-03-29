## Personal Issue Tracker CLI interface
## ====================================

import algorithm, cliutils, data_uri, docopt, json, logging, options, os,
  sequtils, std/wordwrap, tables, terminal, times, timeutils, unicode, uuids

from nre import re
import strutils except alignLeft, capitalize, strip, toUpper, toLower
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

let EDITOR =
  if existsEnv("EDITOR"): getEnv("EDITOR")
  else: "vi"


proc initContext(args: Table[string, Value]): CliContext =
  let pitCfg = loadConfig(args)

  let cliJson =
    if pitCfg.cfg.json.hasKey("cli"): pitCfg.cfg.json["cli"]
    else: newJObject()

  let cliCfg = CombinedConfig(docopt: args, json: cliJson)

  result = CliContext(
    cfg: pitCfg,
    contexts: pitCfg.contexts,
    defaultContext:
      if not cliJson.hasKey("defaultContext"): none(string)
      else: some(cliJson["defaultContext"].getStr()),
    verbose: parseBool(cliCfg.getVal("verbose", "false")) and not args["--quiet"],
    issues: newTable[IssueState, seq[Issue]](),
    tasksDir: pitCfg.tasksDir,
    termWidth: parseInt(cliCfg.getVal("termWidth", "80")),
    triggerPtk: cliJson.getOrDefault("triggerPtk").getBool(false))

proc getIssueContextDisplayName(ctx: CliContext, context: string): string =
  if not ctx.contexts.hasKey(context):
    if context.isEmptyOrWhitespace: return "<default>"
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
  if not issue.details.isEmptyOrWhitespace:
    result &= issue.details.strip.withColor(fgCyan) & "\n"

  result &= termReset

proc formatSectionIssue(ctx: CliContext, issue: Issue, width: int, indent = "",
                verbose = false): string =

  result = ""

  var showDetails = not issue.details.isEmptyOrWhitespace and verbose

  var prefixLen = 0
  var summaryIndentLen = indent.len + 7

  if issue.hasProp("delegated-to"): prefixLen += issue["delegated-to"].len + 2 # space for the ':' and ' '

  # Wrap and write the summary.
  var wrappedSummary = ("+".repeat(prefixLen) & issue.summary).wrapWords(width - summaryIndentLen).indent(summaryIndentLen)

  wrappedSummary = wrappedSummary[(prefixLen + summaryIndentLen)..^1]

  result = (indent & ($issue.id)[0..<6]).withColor(fgBlack, true) & " "

  if issue.hasProp("delegated-to"):
    result &= (issue["delegated-to"] & ": ").withColor(fgGreen)

  result &= wrappedSummary.withColor(fgWhite)

  if issue.tags.len > 0:
    let tagsStr = "(" & issue.tags.join(", ") & ")"
    if (result.splitLines[^1].len + tagsStr.len + 1) > (width - 2):
      result &= "\n" & indent
    result &= " " & tagsStr.withColor(fgGreen)


  if issue.hasProp("pending"):
    let startIdx = "Pending: ".len
    var pendingText = issue["pending"].wrapWords(width - startIdx - summaryIndentLen)
                                      .indent(startIdx)
    pendingText = ("Pending: " & pendingText[startIdx..^1]).indent(summaryIndentLen)
    result &= "\n" & pendingText.withColor(fgCyan)

  if showDetails:
    result &= "\n" & issue.details.strip.indent(indent.len + 2).withColor(fgCyan)

  result &= termReset

proc formatSectionIssueList(ctx: CliContext, issues: seq[Issue], width: int,
        indent: string, verbose: bool): string =

  result = ""
  for i in issues:
    var issueText = ctx.formatSectionIssue(i, width, indent, verbose)
    result &= issueText & "\n"

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

proc loadOpenIssues(ctx: CliContext) =
  ctx.issues = newTable[IssueState, seq[Issue]]()
  for state in [Current, TodoToday, Todo, Pending, Todo]: ctx.loadIssues(state)

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

proc reorder(ctx: CliContext, state: IssueState) =

  # load the issues to make sure the order file contains all issues in the state.
  ctx.loadIssues(state)
  discard os.execShellCmd(EDITOR & " '" & (ctx.tasksDir / $state / "order.txt") & "' </dev/tty >/dev/tty")

proc edit(issue: Issue) =

  # Write format comments (to help when editing)
  writeFile(issue.filepath, toStorageFormat(issue, true))

  discard os.execShellCmd(EDITOR & " '" & issue.filepath & "' </dev/tty >/dev/tty")

  try:
    # Try to parse the newly-edited issue to make sure it was successful.
    let editedIssue = loadIssue(issue.filepath)
    editedIssue.store()
  except:
    fatal "updated issue is invalid (ignoring edits): \n\t" &
      getCurrentExceptionMsg()
    issue.store()

proc list(ctx: CliContext, filter: Option[IssueFilter], states: Option[seq[IssueState]], showToday, showFuture, verbose: bool) =

  if states.isSome:
    trace "listing issues for " & $states.get
    for state in states.get:
      ctx.loadIssues(state)
      if filter.isSome: ctx.filterIssues(filter.get)

      # Show Done for just today if requested
      if state == Done and showToday:
        ctx.issues[Done] = ctx.issues[Done].filterIt(
          it.hasProp("completed") and
          sameDay(getTime().local, it.getDateTime("completed")))

      stdout.write ctx.formatSection(ctx.issues[state], state, "", verbose)
    trace "listing complete"
    return

  ctx.loadOpenIssues()
  if filter.isSome:
    ctx.filterIssues(filter.get)
    trace "filtered issues"

  let today = showToday and [Current, TodoToday, Pending].anyIt(
    ctx.issues.hasKey(it) and ctx.issues[it].len > 0)

  let future = showFuture and [Pending, Todo].anyIt(
    ctx.issues.hasKey(it) and ctx.issues[it].len > 0)

  let indent = if today and future: "  " else: ""

  # Today's items
  if today:
    if future: ctx.writeHeader("Today")

    for s in [Current, TodoToday, Pending]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        stdout.write ctx.formatSection(ctx.issues[s], s, indent, verbose)

  # Future items
  if future:
    if today: ctx.writeHeader("Future")

    for s in [Pending, Todo]:
      if ctx.issues.hasKey(s) and ctx.issues[s].len > 0:
        let visibleIssues = ctx.issues[s].filterIt(
          not (it.hasProp("hide-until") and
               it.getDateTime("hide-until") > getTime().local))

        stdout.write ctx.formatSection(visibleIssues, s, indent, verbose)

  trace "listing complete"

when isMainModule:
 try:
  const usage = readFile("src/usage.txt")
  const onlineHelp = readFile("src/online-help.txt")

  let consoleLogger = newConsoleLogger(
    levelThreshold=lvlInfo,
    fmtStr="$app - $levelname: ")
  logging.addHandler(consoleLogger)

  # Parse arguments
  let args = docopt(usage, version = PIT_VERSION)

  if args["--debug"]:
    consoleLogger.levelThreshold = lvlDebug

  if args["--echo-args"]: stderr.writeLine($args)

  if args["help"]:
    stderr.writeLine(usage & "\n")
    stderr.writeLine(onlineHelp)
    quit()

  let ctx = initContext(args)

  trace "context initiated"

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

  elif args["reorder"]:
    ctx.reorder(parseEnum[IssueState]($args["<state>"]))

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

  elif args["tag"]:
    if not args["--tags"]: raise newException(Exception, "no tags given")

    let newTags = ($args["--tags"]).split(",").mapIt(it.strip)

    for id in @(args["<id>"]):
      var issue = ctx.tasksDir.loadIssueById(id)
      issue.tags = deduplicate(issue.tags & newTags)
      issue.store()

  elif args["untag"]:
    let tagsToRemove: seq[string] =
      if args["--tags"]: ($args["--tags"]).split(",").mapIt(it.strip)
      else: @[]

    for id in @(args["<id>"]):
      var issue = ctx.tasksDir.loadIssueById(id)
      if tagsToRemove.len > 0:
        issue.tags = issue.tags.filter(
          proc (tag: string): bool = not tagsToRemove.anyIt(it == tag))
      else: issue.tags = @[]
      issue.store()

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
      if targetState == Done:
        issue["completed"] = getTime().local.formatIso8601
        if issue.hasProp("recurrence") and issue.getRecurrence.isSome:
          let nextIssue = ctx.tasksDir.nextRecurrence(issue.getRecurrence.get, issue)
          ctx.tasksDir.store(nextIssue, Todo)
          info "created the next recurrence:"
          stdout.writeLine ctx.formatIssue(nextIssue)


      issue.changeState(ctx.tasksDir, targetState)

    if ctx.triggerPtk or args["--ptk"]:
      if targetState == Current:
        let issue = ctx.tasksDir.loadIssueById($(args["<id>"][0]))
        var cmd = "ptk start"
        if issue.tags.len > 0 or issue.hasProp("context"):
          let tags = concat(
            issue.tags,
            if issue.hasProp("context"): @[issue.properties["context"]]
            else: @[]
          )
          cmd &= " -g \"" & tags.join(",") & "\""
        cmd &= " -n \"pit-id: " & $issue.id & "\""
        cmd &= " \"" & issue.summary & "\""
        discard execShellCmd(cmd)
      elif targetState == Done or targetState == Pending:
        discard execShellCmd("ptk stop")

  elif args["hide-until"]:

    let issue = ctx.tasksDir.loadIssueById($(args["<id>"]))
    issue.setDateTime("hide-until", parseDate($args["<date>"]))

    issue.store()

  elif args["delegate"]:

    let issue = ctx.tasksDir.loadIssueById($(args["<id>"]))
    issue["delegated-to"] = $args["<delegated-to>"]

    issue.store()

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

    # If they supplied text matches, add that to the filter.
    if args["--match"]:
      filter.summaryMatch = some(re("(?i)" & $args["--match"]))
      filterOption = some(filter)

    if args["--match-all"]:
      filter.fullMatch = some(re("(?i)" & $args["--match-all"]))
      filterOption = some(filter)

    # If no "context" property is given, use the default (if we have one)
    if ctx.defaultContext.isSome and not filter.properties.hasKey("context"):
      stderr.writeLine("Limiting to default context: " & ctx.defaultContext.get)
      filter.properties["context"] = ctx.defaultContext.get
      filterOption = some(filter)

    if args["--tags"]:
      filter.hasTags = ($args["--tags"]).split(',')
      filterOption = some(filter)

    # Finally, if the "context" is "all", don't filter on context
    if filter.properties.hasKey("context") and
       filter.properties["context"] == "all":

      filter.properties.del("context")

    var listContexts = false
    var statesOption = none(seq[IssueState])
    var issueIdsOption = none(seq[string])

    if args["contexts"]: listContexts = true
    elif args["<stateOrId>"]:
      try: statesOption = some(args["<stateOrId>"].mapIt(parseEnum[IssueState]($it)))
      except: issueIdsOption = some(args["<stateOrId>"].mapIt($it))

    # List the known contexts
    if listContexts:
      var uniqContexts = toSeq(ctx.contexts.keys)
      ctx.loadAllIssues()
      for state, issueList in ctx.issues:
        for issue in issueList:
          if issue.hasProp("context") and not uniqContexts.contains(issue["context"]):
            uniqContexts.add(issue["context"])

      let maxLen = foldl(uniqContexts,
        if a.len > b.len: a
        else: b
      ).len

      for c in uniqContexts.sorted:
        stdout.writeLine(c.alignLeft(maxLen+2) & ctx.getIssueContextDisplayName(c))

    # List a specific issue
    elif issueIdsOption.isSome:
      for issueId in issueIdsOption.get:
        let issue = ctx.tasksDir.loadIssueById(issueId)
        stdout.writeLine ctx.formatIssue(issue)

    # List all issues
    else:
      trace "listing all issues"
      let showBoth = args["--today"] == args["--future"]
      ctx.list(filterOption, statesOption, showBoth or args["--today"],
                                          showBoth or args["--future"],
                                          ctx.verbose)

  elif args["add-binary-property"]:
    let issue = ctx.tasksDir.loadIssueById($(args["<id>"]))

    let propIn =
      if $(args["<propSource>"]) == "-": stdin
      else: open($(args["<propSource>"]))

    try: issue[$(args["<propName>"])] = encodeAsDataUri(readAll(propIn))
    finally: close(propIn)

    issue.store()

  elif args["get-binary-property"]:
    let issue = ctx.tasksDir.loadIssueById($(args["<id>"]))

    if not issue.hasProp($(args["<propName>"])):
      raise newException(Exception,
        "issue " & ($issue.id)[0..<6] & " has no property name '" &
        $(args["<propName>"]) & "'")

    let propOut =
      if $(args["<propDest>"]) == "-": stdout
      else: open($(args["<propDest>"]), fmWrite)

    try: write(propOut, decodeDataUri(issue[$(args["<propName>"])]))
    finally: close(propOut)

 except:
  fatal getCurrentExceptionMsg()
  #raise getCurrentException()
  quit(QuitFailure)
