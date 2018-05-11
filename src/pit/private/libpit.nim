import cliutils, options, os, ospaths, sequtils, strutils, tables, times, uuids

from nre import re, match
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

const ISO8601Format* = "yyyy:MM:dd'T'HH:mm:sszzz"
let ISSUE_FILE_PATTERN = re"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}\.txt"

proc displayName*(s: IssueState): string =
  case s
  of Current: result = "Current"
  of Pending: result = "Pending"
  of Done: result = "Done"
  of Todo: result = "Todo"
  of TodoToday: result = "Todo"

## Allow issue properties to be accessed as if the issue was a table
proc `[]`*(issue: Issue, key: string): string =
  return issue.properties[key]

proc `[]=`*(issue: Issue, key: string, value: string) =
  issue.properties[key] = value

proc getDateTime*(issue: Issue, key: string): DateTime =
  return parse(issue.properties[key], ISO8601Format)

proc setDateTime*(issue: Issue, key: string, dt: DateTime) =
  issue.properties[key] = format(dt, ISO8601Format)

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
      if line.isNilOrWhitespace: continue

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

proc toStorageFormat*(issue: Issue): string =
  var lines = @[issue.summary]
  for key, val in issue.properties: lines.add(key & ": " & val)
  if issue.tags.len > 0: lines.add("tags: " & issue.tags.join(","))
  if not isNilOrWhitespace(issue.details):
    lines.add("--------")
    lines.add(issue.details)

  result = lines.join("\n")
  
## Load and store from filesystem
proc loadIssue*(filePath: string): Issue =
  result = fromStorageFormat(splitFile(filePath).name, readFile(filePath))
  result.filepath = filePath

proc storeIssue*(dirPath: string, issue: Issue) =
  issue.filepath = joinPath(dirPath, $issue.id & ".txt")
  writeFile(issue.filepath, toStorageFormat(issue))

proc loadIssues*(path: string): seq[Issue] =
  result = @[]
  for path in walkDirRec(path):
    if extractFilename(path).match(ISSUE_FILE_PATTERN).isSome():
      result.add(loadIssue(path))

## Utilities for working with issue collections.
proc groupBy*(issues: seq[Issue], propertyKey: string): TableRef[string, seq[Issue]] =
  result = newTable[string, seq[Issue]]()
  for i in issues:
    let key = if i.properties.hasKey(propertyKey): i[propertyKey] else: ""
    if not result.hasKey(key): result[key] = newSeq[Issue]()
    result[key].add(i)


