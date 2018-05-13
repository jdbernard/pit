# Package

version       = "4.0.0"
author        = "Jonathan Bernard"
description   = "Personal issue tracker."
license       = "MIT"
srcDir        = "src"
bin           = @["pit"]

# Dependencies

requires @["nim >= 0.18.0", "uuids 0.1.9", "docopt 0.6.5", "cliutils 0.3.4", "timeutils 0.3.0"]
