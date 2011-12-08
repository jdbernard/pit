package com.jdbernard.pit.file

import com.jdbernard.pit.*

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
                if (child.name ==~ /\d+/ ||
                    child.isHidden())  return // just an issue folder

                // otherwise build and add to list
                projects[(child.name)] =  new FileProject(child) }
            else if (child.isFile() &&
                       FileIssue.isValidFilename(child.name)) {
                def issue
                
                // if exception, then not an issue
                try { issue = new FileIssue(child) } catch (all) { return }

                issues[(issue.id)] = issue } }}

    public void setName(String name) {
        super.setName(name)
        source.renameTo(new File(source.canonicalFile.parentFile, name)) }
    
    public FileIssue createNewIssue(Map options) {
        Issue issue
        File issueFile

        if (!options) options = [:]

        // We want some different defaults for issues due to the parser being
        // unable to handle empty title or text.
        if (!options.title) options.title = "Default issue title."
        if (!options.text) options.text = "Describe the issue here."

        // We are also going to find the next id based on the issues already in the
        // project.
        if (issues.size() == 0) options.id = '0000'
        else {
            def lastId = (issues.values().max { it.id.toInteger() }).id
            options.id = (lastId.toInteger() + 1).toString().padLeft(
                lastId.length(), '0') }

        // Create an Issue object from the options (we will discard it later).
        issue = new Issue(options)

        // Create the filename and File object based on the options given.
        issueFile = new File(source, FileIssue.makeFilename(
            issue.id, issue.category, issue.status, issue.priority))

        // Create the actual file on the system
        issueFile.createNewFile()

        // Write the issue to the file created.
        issueFile.write(FileIssue.formatIssue(issue))

        // Read that new file back in as a FileIssue
        issue = new FileIssue(issueFile)

        // Add the issue to our collection.
        issues[(issue.id)] = issue

        return issue }

    public FileProject createNewProject(String name) {
        def newDir = new File(source, name)
        newDir.mkdirs()

        return new FileProject(newDir) }

    public boolean deleteIssue(Issue issue) {
        if (!issues[(issue.id)]) return false

        issues.remove(issue.id)
        if (issue instanceof FileIssue)
            return issue.deleteFile()

        else return true }

    public boolean deleteProject(Project project) {
        if (!projects[(project.name)]) return false

        projects.remove(project.name)
        if (project instanceof FileProject)
            return project.source.delete()

        return true }

    @Override
    public String toString() { return name }

}
