package com.jdbernard.pit

class FileProject extends Project {

    protected File source

    public FileProject(File dir) {
        super(dir.canonicalFile.name)

        if (!dir.isDirectory())
            throw new IllegalArgumentException(
                "${dir.name} is not a directory.")

        this.source = dir

        dir.eachFile { child ->

            // add sub projects
            if (child.isDirectory())  {
                if ( child.name ==~ /\d{4}/)  return // just an issue folder

                // otherwise build and add to list
                projects[(child.name)] =  new FileProject(child)
            } else if (child.isFile() &&
                       FileIssue.isValidFilename(child.name)) {
                def issue
                
                // if exception, then not an issue
                try { issue = new FileIssue(child) } catch (all) { return }

                issues[(issue.id)] = issue
            }
        }
    }

    public void setName(String name) {
        super.setName(name)
        source.renameTo(new File(source.canonicalFile.parentFile, name))
    }
    
    public FileIssue createNewIssue(Map options) {
        if (!options) options = [:]
        if (!options.category) options.category = Category.TASK
        if (!options.priority) options.priority = 5
        if (!options.text) options.text = "Default issue title.\n" +
                                          "====================\n"
        String id
        if (issues.size() == 0) id = '0000'
        else {
            id = (issues.values().max { it.id.toInteger() }).id
            id = (id.toInteger() + 1).toString().padLeft(id.length(), '0')
        }

        def issueFile = new File(source, FileIssue.makeFilename(id,
            options.category, options.priority))

        assert !issueFile.exists()

        issueFile.createNewFile()
        issueFile.write(options.text)

        def issue = new FileIssue(issueFile)
        issues[(issue.id)] = issue

        return issue
    }

    public FileProject createNewProject(String name) {
        def newDir = new File(source, name)
        newDir.mkdirs()

        return new FileProject(newDir)
    }

    public boolean delete() { return source.delete() }

    @Override
    public String toString() { return name }

}
