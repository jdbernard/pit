package com.jdbernard.pit.util

import com.jdbernard.pit.*

if (args.size() != 1) {
    println "Usage: Convert1_2 [dir]"
    System.exit(1)
}

File rootDir = new File(args[0])
Scanner scan = new Scanner(System.in)

rootDir.eachFileRecurse { file ->
    def m = file.name =~ /(\d+)([bcft])(\d).*/
    if (m && file.isFile()) {
        println m[0][0]
        def parentFile = file.canonicalFile.parentFile
        def c
        def s
        switch(m[0][2]) {
            case "c":
                println file.readLines()[0]
                print "Issue was closed, was category does it belong in?"
                c = Category.toCategory(scan.nextLine())
                s = Status.RESOLVED
                break
            default:
                c = Category.toCategory(m[0][2])
                s = Status.NEW
                break
        }
        println "${m[0][2]}: ${c}"
        file.renameTo(new File(parentFile,
            FileIssue.makeFilename(m[0][1], c, s, m[0][3].toInteger())))
    }
}
