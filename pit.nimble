# Package

version       = "4.9.4"
author        = "Jonathan Bernard"
description   = "Personal issue tracker."
license       = "MIT"
srcDir        = "src"
bin           = @["pit", "pit_api"]

# Dependencies

requires @[
  "nim >= 1.4.0",
  "docopt 0.6.8",
  "jester 0.5.0",
  "uuids 0.1.10",

  "https://git.jdb-labs.com/jdb/nim-cli-utils.git >= 0.6.4",
  "https://git.jdb-labs.com/jdb/nim-lang-utils.git >= 0.4.0",
  "https://git.jdb-labs.com/jdb/nim-time-utils.git >= 0.4.0",
  "https://git.jdb-labs.com/jdb/update-nim-package-version"
]

task updateVersion, "Update the version of this package.":
  exec "update_nim_package_version pit 'src/pitpkg/version.nim'"
