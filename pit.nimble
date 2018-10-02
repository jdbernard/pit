# Package

include "src/pitpkg/version.nim"

version       = PIT_VERSION
author        = "Jonathan Bernard"
description   = "Personal issue tracker."
license       = "MIT"
srcDir        = "src"
bin           = @["pit", "pit_api"]

# Dependencies

requires @[ "nim >= 0.18.0", "cliutils 0.5.0", "docopt 0.6.5", "jester 0.2.0",
  "langutils >= 0.4.0", "timeutils 0.3.0", "uuids 0.1.9" ]
