# Package

include "src/pitpkg/version.nim"

version       = PIT_VERSION
author        = "Jonathan Bernard"
description   = "Personal issue tracker."
license       = "MIT"
srcDir        = "src"
bin           = @["pit", "pit_api"]

# Dependencies

requires @[ "nim >= 0.19.0", "docopt 0.6.8", "jester 0.4.1", "uuids 0.1.10" ]

requires "https://git.jdb-labs.com/jdb/nim-cli-utils.git >= 0.6.1"
requires "https://git.jdb-labs.com/jdb/nim-lang-utils.git >= 0.4.0"
requires "https://git.jdb-labs.com/jdb/nim-time-utils.git >= 0.4.0"
