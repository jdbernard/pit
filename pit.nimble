# Package

include "src/pitpkg/version.nim"

version       = PIT_VERSION
author        = "Jonathan Bernard"
description   = "Personal issue tracker."
license       = "MIT"
srcDir        = "src"
bin           = @["pit", "pit_api"]

# Dependencies

requires @[ "nim >= 0.19.0", "cliutils 0.6.0", "docopt 0.6.8", "jester 0.4.1",
  "langutils >= 0.4.0", "timeutils 0.3.0", "uuids 0.1.10" ]
