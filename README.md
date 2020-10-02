# Personal Issue Tracker

This is [Jonathan Bernard's](mailto:jonathan@jdbernard.com) personal issue
tracker. In it's current form it is essentially a way to keep an curated list of
TODO's, organizing them by workflow category (todo, todo-today, dormant, etc.)
and context (Personal, Work, etc.).

## Categories

`pit` organizes issues into the following workflow categories:

- `current` - actively in progress
- `todo` - to be addressed in the future
- `todo-today` - chosen to be addressed today
- `pending` - blocked by some third party
- `dormant` - long-term things I don't want to forget but don't need in front
  of me every day.
- `done`

In my typical workflow the `todo` category serves as a collection point for
things I want to keep track of. Then on a a daily basis I review issues in the
`todo` category and move a selection to the `todo-today` category. I also try
to keep the total number of issues in the `todo` below about a dozen. If there
are more than a dozen things in my `todo` category I will identify the lowest
priority items and move them to the `dormant` category.

## Issue Properties

`pit` allows arbitrary properties to be attached to issues in the form of
key-value pairs. On the command line these can be provided via the `-p` or
`--properties` parameter in the form
`-p <prop1Name>:<prop1Value>;<prop2Name>:<prop2Value>[;...]`

There are a couple of properties that pit will recognize automatically:

- `context`: the context organization feature is implemented using issue
  properties.
- `created`: `pit` uses this property to timestamp an issue when it is created.
- `completed`: `pit` uses this property to timestamp an issue when it is moved
  to the `done` category.
- `pending`: `pit` looks to this property to provide extra information about
  issues in the `pending` category. Typically I use this to note who or what is
  blocking the issue and why.

Some other common properties I use are:

- `resolution`: for short notes about why an issue was moved to `done`,
  especially if it the action wasn't taken or if it is not completely clear
  that this issue was completed.

## Configuration Options

`pit` allows configuration via command-line options and via a configuration
file. There is some overlap between the two methods of configuring `pit`, but
it is not a complete mapping.

### Config File

`pit` looks for a JSON configuration file in the following places (in order):

1. From a file path passed on the command line via the `--config <cfgFile>` parameter,
2. `./.pitrc`, in the current working directory,
3. From a file path set in the `PITRC` environment variable.
4. `$HOME/.pitrc`, in the user's home directory.


#### Sample Config File

This example illustrates all of the possible configuration options.

```json
{
  "api": {
    "apiKeys": [
      "50cdcb660554e2d50fd88bd40b6579717bf00643f6ff57f108baf16c8c083f77",
      "e4fc1aac49fc1f2f7f4ca6b1f04d41a4ccdd58e13bb53b41da97703d47267ceb",
    ]
  },
  "cli": {
    "defaultContext": "personal",
    "verbose": false,
    "termWidth": 120,
    "triggerPtk": true
  },
  "contexts": {
    "nla-music": "New Life Music",
    "nla-youth-band": "New Life Youth Band",
    "acn": "Accenture",
    "hff": "Hope Family Fellowship"
  },
  "tasksDir": "/mnt/c/Users/Jonathan Bernard/synced/tasks"
}
```

#### Explanation of configurable options.

In general, options supplied on the CLI directly will override options supplied
in the configuration file. All options are optional unless stated otherwise.

* `api`: configuration options specific to the API service.

  - `apiKeys`: a list of Bearer tokens accepted by the API for the purpose of
    authenticating API requests.

* `cli`: configuration options specific to the CLI.

  - `defaultContext`: if present all invokations to the CLI will
    be in this context. This is like adding a `--context <defaultContext>`
    parameter to every CLI invocation. Any actual `--context` parameter will
    override this value.

  - `verbose`: Show issue details when listing issues (same as
    `--verbose` flag).

  - `termWidth`: Set the expected width of the terminal (for wrapping text).

  - `triggerPtk`: If set to `true`, invoke the `ptk` command to start and stop
    timers when issues move to the `current` and `done` categories
    respectively.

* `contexts`: `pit` allows issues to be organized into different contexts via
  a `context` property on the issue. The CLI groups issues according to
  context. When printing contexts the CLI will take the value from the issues'
  `context` properties and capatalize it. In some cases you may wish to have a
  different display value for a context. I like to use abbreviations for long
  context names to reduce the need to type, `hff` for "Hope Family Fellowship",
  for example. The `contexts` config option allows you to provide a map of
  context values to context display names See the sample file below for an
  example.

  Note that this mapping does not have to have entries for all contexts, only
  those you wish to provide with an alternate display form. For example, in the
  configuration sample above the default context is `personal`, a value not
  present in the `contexts` configuration. `personal` will be displayed as
  "Personal"; it does not need an alternate display name.

* `tasksDir` **required**: a file path to the root directory for the issue
  repository (same as `--tasks-dir` CLI parameter).
