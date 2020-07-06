import cliutils, docopt, json, logging, langutils, options, os,
  sequtils, strutils, tables, times, timeutils, uuids

from nre import find, match, re, Regex

type
  Issue* = ref object
    id*: UUID
    filepath*: string
    summary*, details*: string
    properties*: TableRef[string, string]
    tags*: seq[string]

  IssueState* = enum
    Current = "current",
    TodoToday = "todo-today",
    Pending = "pending",
    Done = "done",
    Todo = "todo"
    Dormant = "dormant"

  IssueFilter* = ref object
    completedRange*: Option[tuple[b, e: DateTime]]
    fullMatch*, summaryMatch*: Option[Regex]
    properties*: TableRef[string, string]

  PitConfig* = ref object
    tasksDir*: string
    contexts*: TableRef[string, string]
    cfg*: CombinedConfig

const DONE_FOLDER_FORMAT* = "yyyy-MM"

let ISSUE_FILE_PATTERN = re"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}\.txt"

proc displayName*(s: IssueState): string =
  case s
  of Current: result = "Current"
  of Done: result = "Done"
  of Dormant: result = "Dormant"
  of Pending: result = "Pending"
  of Todo: result = "Todo"
  of TodoToday: result = "Todo"

## Allow issue properties to be accessed as if the issue was a table
proc `[]`*(issue: Issue, key: string): string =
  return issue.properties[key]

proc `[]=`*(issue: Issue, key: string, value: string) =
  issue.properties[key] = value

## Issue property accessors
proc hasProp*(issue: Issue, key: string): bool =
  return issue.properties.hasKey(key)

proc getDateTime*(issue: Issue, key: string): DateTime =
  return issue.properties[key].parseIso8601

proc getDateTime*(issue: Issue, key: string, default: DateTime): DateTime =
  if issue.properties.hasKey(key): return issue.properties[key].parseIso8601
  else: return default

proc setDateTime*(issue: Issue, key: string, dt: DateTime) =
  issue.properties[key] = dt.formatIso8601

## Issue filtering
proc initFilter*(): IssueFilter =
  result = IssueFilter(
    completedRange: none(tuple[b, e: DateTime]),
    fullMatch: none(Regex),
    summaryMatch: none(Regex),
    properties: newTable[string, string]())

proc propsFilter*(props: TableRef[string, string]): IssueFilter =
  if isNil(props):
    raise newException(ValueError,
      "cannot initialize property filter without properties")

  result = initFilter()
  result.properties = props

proc dateFilter*(range: tuple[b, e: DateTime]): IssueFilter =
  result = initFilter()
  result.completedRange = some(range)

proc summaryMatchFilter*(pattern: string): IssueFilter =
  result = initFilter()
  result.summaryMatch = some(re("(?i)" & pattern))

proc fullMatchFilter*(pattern: string): IssueFilter =
  result = initFilter()
  result.fullMatch = some(re("(?i)" & pattern))

proc groupBy*(issues: seq[Issue], propertyKey: string): TableRef[string, seq[Issue]] =
  result = newTable[string, seq[Issue]]()
  for i in issues:
    let key = if i.hasProp(propertyKey): i[propertyKey] else: ""
    if not result.hasKey(key): result[key] = newSeq[Issue]()
    result[key].add(i)


## Parse and format issues
proc fromStorageFormat*(id: string, issueTxt: string): Issue =
  type ParseState = enum ReadingSummary, ReadingProps, ReadingDetails

  result = Issue(
    id: parseUUID(id),
    properties: newTable[string,string](),
    tags: @[])

  var parseState = ReadingSummary
  var detailLines: seq[string] = @[]

  for line in issueTxt.splitLines():
    if line.startsWith("#"): continue   # ignore lines starting with '#'

    case parseState

    of ReadingSummary:
      result.summary = line.strip()
      parseState = ReadingProps

    of ReadingProps:
      # Ignore empty lines
      if line.isEmptyOrWhitespace: continue

      # Look for the sentinal to start parsing as detail lines
      if line == "--------":
        parseState = ReadingDetails
        continue


      let parts = line.split({':'}, 1).mapIt(it.strip())
      if parts.len != 2:
        raise newException(ValueError, "unable to parse property line: " & line)

      # Take care of special properties: `tags`
      if parts[0] == "tags": result.tags = parts[1].split({','}).mapIt(it.strip())
      else: result[parts[0]] = parts[1]

    of ReadingDetails:
      detailLines.add(line)

  result.details = if detailLines.len > 0: detailLines.join("\n") else: ""

proc toStorageFormat*(issue: Issue, withComments = false): string =
  var lines: seq[string] = @[]
  if withComments: lines.add("# Summary (one line):")
  lines.add(issue.summary)
  if withComments: lines.add("# Properties (\"key:value\" per line):")
  for key, val in issue.properties:
    if not val.isEmptyOrWhitespace: lines.add(key & ": " & val)
  if issue.tags.len > 0: lines.add("tags: " & issue.tags.join(","))
  if not isEmptyOrWhitespace(issue.details) or withComments:
    if withComments: lines.add("# Details go below the \"--------\"")
    lines.add("--------")
    lines.add(issue.details)

  result = lines.join("\n")

## Load and store from filesystem
proc loadIssue*(filePath: string): Issue =
  result = fromStorageFormat(splitFile(filePath).name, readFile(filePath))
  result.filepath = filePath

proc loadIssueById*(tasksDir, id: string): Issue =
  for path in walkDirRec(tasksDir):
    if path.splitFile.name.startsWith(id):
      return loadIssue(path)
  raise newException(KeyError, "cannot find issue for id: " & id)

proc store*(issue: Issue, withComments = false) =
  writeFile(issue.filepath, toStorageFormat(issue, withComments))

proc store*(tasksDir: string, issue: Issue, state: IssueState, withComments = false) =
  let stateDir = tasksDir / $state
  let filename = $issue.id & ".txt"
  if state == Done:
    let monthPath = issue.getDateTime("completed", getTime().local).format(DONE_FOLDER_FORMAT)
    issue.filepath = stateDir / monthPath / filename
  else:
    issue.filepath = stateDir / filename

  issue.store(withComments)

proc storeOrder*(issues: seq[Issue], path: string) =
  var orderLines = newSeq[string]()

  for context, issues in issues.groupBy("context"):
    orderLines.add("> " & context)
    for issue in issues: orderLines.add($issue.id & " " & issue.summary)
    orderLines.add("")

  let orderFile = path / "order.txt"
  orderFile.writeFile(orderLines.join("\n"))

proc loadIssues*(path: string): seq[Issue] =
  let orderFile = path / "order.txt"

  let orderedIds =
    if fileExists(orderFile):
      toSeq(orderFile.lines)
        .mapIt(it.split(' ')[0])
        .deduplicate
        .filterIt(not it.startsWith("> ") and not it.isEmptyOrWhitespace)
    else: newSeq[string]()

  type TaggedIssue = tuple[issue: Issue, ordered: bool]
  var unorderedIssues: seq[TaggedIssue] = @[]

  for path in walkDirRec(path):
    if extractFilename(path).match(ISSUE_FILE_PATTERN).isSome():
      unorderedIssues.add((loadIssue(path), false))

  result = @[]

  # Add all ordered issues in order
  for id in orderedIds:
    let idx = unorderedIssues.indexOf(($it.issue.id).startsWith(id))
    if idx > 0:
      result.add(unorderedIssues[idx].issue)
      unorderedIssues[idx].ordered = true

  # Add all remaining, unordered issues in the order they were loaded
  for taggedIssue in unorderedIssues:
    if taggedIssue.ordered: continue
    result.add(taggedIssue.issue)

  # Finally, save current order
  result.storeOrder(path)

proc changeState*(issue: Issue, tasksDir: string, newState: IssueState) =
  let oldFilepath = issue.filepath
  if newState == Done: issue.setDateTime("completed", getTime().local)
  tasksDir.store(issue, newState)
  if oldFilePath != issue.filepath: removeFile(oldFilepath)

proc delete*(issue: Issue) = removeFile(issue.filepath)

## Utilities for working with issue collections.
proc filter*(issues: seq[Issue], filter: IssueFilter): seq[Issue] =
  result = issues

  for k,v in filter.properties:
    result = result.filterIt(it.hasProp(k) and it[k] == v)

  if filter.completedRange.isSome:
    let range = filter.completedRange.get
    result = result.filterIt(
             not it.hasProp("completed") or
             it.getDateTime("completed").between(range.b, range.e))

  if filter.summaryMatch.isSome:
    let p = filter.summaryMatch.get
    result = result.filterIt(it.summary.find(p).isSome)

  if filter.fullMatch.isSome:
    let p = filter.fullMatch.get
    result = result.filterIt( it.summary.find(p).isSome or it.details.find(p).isSome)

### Configuration utilities
proc loadConfig*(args: Table[string, Value] = initTable[string, Value]()): PitConfig =
  let pitrcLocations = @[
    if args["--config"]: $args["--config"] else: "",
    ".pitrc", $getEnv("PITRC"), $getEnv("HOME") & "/.pitrc"]

  var pitrcFilename: string =
    foldl(pitrcLocations, if len(a) > 0: a elif existsFile(b): b else: "")

  if not existsFile(pitrcFilename):
    warn "pit: could not find .pitrc file: " & pitrcFilename
    if isEmptyOrWhitespace(pitrcFilename):
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

  result = PitConfig(
    cfg: cfg,
    contexts: newTable[string,string](),
    tasksDir: cfg.getVal("tasks-dir", ""))

  if cfgJson.hasKey("contexts"):
    for k, v in cfgJson["contexts"]:
      result.contexts[k] = v.getStr()

  if isEmptyOrWhitespace(result.tasksDir):
    raise newException(Exception, "no tasks directory configured")

  if not existsDir(result.tasksDir):
    raise newException(Exception, "cannot find tasks dir: " & result.tasksDir)

  # Create our tasks directory structure if needed
  for s in IssueState:
    if not existsDir(result.tasksDir / $s):
      (result.tasksDir / $s).createDir


