## Personal Issue Tracker API Interface
## ====================================

import asyncdispatch, cliutils, docopt, jester, json, logging, sequtils, strutils
import nre except toSeq

import pitpkg/private/libpit

include "pitpkg/version.nim"

type
  PitApiCfg* = object
    apiKeys*: seq[string]
    global*: PitConfig
    port*: int

const TXT = "text/plain"

proc raiseEx(reason: string): void = raise newException(Exception, reason)

template checkAuth(cfg: PitApiCfg) =
  ## Check this request for authentication and authorization information.
  ## If the request is not authorized, this template sets up the 401 response
  ## correctly. The calling context needs only to return from the route.

  var authed {.inject.} = false

  try:
    if not request.headers.hasKey("Authorization"):
      raiseEx "No auth token."

    let headerVal = request.headers["Authorization"]
    if not headerVal.startsWith("Bearer "):
      raiseEx "Invalid Authentication type (only 'Bearer' is supported)."

    if not cfg.apiKeys.contains(headerVal[7..^1]):
      raiseEx "Invalid API key."

    authed = true

  except:
    stderr.writeLine "Auth failed: " & getCurrentExceptionMsg()
    response.data[0] = CallbackAction.TCActionSend
    response.data[1] = Http401
    response.data[2]["WWW-Authenticate"] = "Bearer"
    response.data[2]["Content-Type"] = TXT
    response.data[3] = getCurrentExceptionMsg()

proc paramsToArgs(params: StringTableRef): tuple[stripAnsi: bool, args: seq[string]] =
  result = (false, @[])

  if params.hasKey("color"):
    if params["color"] != "true":
      result[0] = true

  for k,v in params:
    if k == "color": continue
    elif k.startsWith("arg"): result[1].add(v) # support ?arg1=val1&arg2=val2 -> cmd val1 val2
    else :
      result[1].add("--" & k)
      if v != "true": result[1].add(v) # support things like ?verbose=true -> cmd --verbose

let STRIP_ANSI_REGEX = re"\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K]"

proc stripAnsi(str: string): string =
  return str.replace(STRIP_ANSI_REGEX, "")

proc start*(cfg: PitApiCfg) =

  var stopFuture = newFuture[void]()

  settings:
    port = Port(cfg.port)
    appName = "/api"

  routes:

    get "/ping":
      resp("pong", TXT)

    get "/issues":
      checkAuth(cfg); if not authed: return true

      var (hasColor, args) = paramsToArgs(request.params)
      args = @["list"] & args

      info "args: \n" & args.join(" ")
      let execResult = execWithOutput("pit", ".", args)
      if execResult[2] != 0: resp(Http500, stripAnsi($execResult[0] & "\n" & $execResult[1]), TXT)
      else:
        if hasColor: resp(stripAnsi(execResult[0]), TXT)
        else: resp(execResult[0], TXT)

    post "/issues":
      checkAuth(cfg); if not authed: return true

    get "/issue/@issueId":
      checkAuth(cfg); if not authed: return true

      var (hasColor, args) = paramsToArgs(request.params)
      args = @["list", issueId] & args

      info "args: \n" & args.join(" ")
      let execResult = execWithOutput("pit", ".", args)
      if execResult[2] != 0: resp(Http500, stripAnsi($execResult[0] & "\n" & $execResult[1]), TXT)
      else:
        if hasColor: resp(stripAnsi(execResult[0]), TXT)
        else: resp(execResult[0], TXT)

  waitFor(stopFuture)

proc loadApiConfig(args: Table[string, Value]): PitApiCfg =
  let pitCfg = loadConfig(args)
  let apiJson =
    if pitCfg.cfg.json.hasKey("api"): pitCfg.cfg.json["api"]
    else: newJObject()

  let apiCfg = CombinedConfig(docopt: args, json: apiJson)

  let apiKeysArray =
    if apiJson.hasKey("apiKeys"): apiJson["apiKeys"]
    else: newJArray()

  result = PitApiCfg(
    apiKeys: toSeq(apiKeysArray).mapIt(it.getStr),
    global: pitCfg,
    port: parseInt(apiCfg.getVal("port", "8123")))

when isMainModule:

  let doc = """\
Usage:
  pit_api [options]

Options:

  -c, --config <cfgFile>      Path to the pit_api config file.
  -d, --tasks-dir             Path to the tasks directory.
  -p, --port                  Port to listen on (defaults to 8123)
"""

  let args = docopt(doc, version = PIT_VERSION)

  let apiCfg = loadApiConfig(args)
  start(apiCfg)
