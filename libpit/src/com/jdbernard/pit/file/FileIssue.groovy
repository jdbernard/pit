package com.jdbernard.pit.file

import com.jdbernard.pit.*

import java.lang.IllegalArgumentException as IAE

import org.joda.time.DateMidnight
import org.joda.time.DateTime

import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class FileIssue extends Issue {

    protected File source
    private Logger log = LoggerFactory.getLogger(getClass())

    public static final String fileExp = /(\d+)([bft])([ajnsv])(\d).*/

    public FileIssue(File file) {

        super(id: -1, title: 'REPLACE_ME')

        if (log.isDebugEnabled()) {
            log.debug("Loading a FileIssue from '{}'", file.canonicalPath) }

        def matcher = file.name =~ fileExp
        if (!matcher)
            throw new IllegalArgumentException("${file} " +
                "is not a valid Issue file.")

        // Read issue attributes from the filename.
        super.@id = matcher[0][1]
        super.@category = Category.toCategory(matcher[0][2])
        super.@status = Status.toStatus(matcher[0][3])
        super.@priority = matcher[0][4].toInteger()

        log.debug("id: {}\tcategory: {}\tstatus: {}\tpriority: {}",
            [super.@id, super.@category, super.@status, super.@priority])

        this.source = file

        String text = ""
        boolean parsingText = true

        // Now parse the actual file contents.
        file.text.eachLine { line, lineNumber ->

            log.trace("lineNumber: {}, line: {}", lineNumber, line)

            if (lineNumber == 0) {
                super.@title = line
                log.debug("Found title: {}", super.@title) }

            else if (lineNumber > 2) {
                // We see the horizontal rule
                if (line ==~ /\s*^\-{4}\-*\s*$/) { parsingText = false }

                if (parsingText) { text += "$line\n" }

                else {
                    def match = (line =~ /^([^:]+):(.+)$/)
                    if (match) {
                        def key = match[0][1].trim()
                        def value = parseValue(match[0][2].trim())
                        super.@extendedProperties[key] = value
                    }
                }
            }
        }

        super.@text = text
    }

    public void setCategory(Category c) throws IOException {
        boolean renamed
        renamed = source.renameTo(new File(source.canonicalFile.parentFile,
            makeFilename(id, c, status, priority)))

        if (!renamed)
            throw new IOException("I was unable to set the category. "
                + "I need to rename the file for this issue, but something is "
                + "preventing me from doing so (maybe the path to the file is "
                + "no longer valid, or maybe the file is currently open in "
                + "some other program).")
        else super.setCategory(c)
    }

    public void setStatus(Status s) throws IOException {
        boolean renamed
        renamed = source.renameTo(new File(source.canonicalFile.parentFile,
            makeFilename(id, category, s, priority)))
            
        if (!renamed)
            throw new IOException("I was unable to set the status. "
                + "I need to rename the file for this issue, but something is "
                + "preventing me from doing so (maybe the path to the file is "
                + "no longer valid, or maybe the file is currently open in "
                + "some other program).")
        else super.setStatus(s)
    }

    public void setPriority(int p) throws IOException {
        boolean renamed
        renamed = source.renameTo(new File(source.canonicalFile.parentFile,
            makeFilename(id, category, status, p)))

        if (!renamed)
            throw new IOException("I was unable to set the priority. "
                + "I need to rename the file for this issue, but something is "
                + "preventing me from doing so (maybe the path to the file is "
                + "no longer valid, or maybe the file is currently open in "
                + "some other program).")
        else super.setPriority(p)
    }

    public String getFilename() {
        return makeFilename(id, category, status, priority)
    }

    public void setTitle(String title) throws IOException {
        super.setTitle(title)
        writeFile()
    }

    public void setText(String text) throws IOException {
        super.setText(text)
        writeFile()
    }

    boolean deleteFile() { return source.deleteDir() }

    public static boolean isValidFilename(String name) {
        return name ==~ fileExp
    }

    public static String makeFilename(String id, Category category,
    Status status, int priority) {

        // bounds check priority
        priority = Math.min(9, Math.max(0, priority))

        //check for valid values of cateogry and id
        if (category == null)
            throw new IAE("Category must be non-null.")
        if (status == null)
            throw new IAE("Status must be non-null.")
        if (!(id ==~ /\d+/))
            throw new IAE( "'${id}' is not a legal value for id.")
        
        return id + category.symbol + status.symbol + priority + ".rst";
    }

    public static String formatIssue(Issue issue) {
        def result = new StringBuilder()
        result.append(issue.@title)
        result.append("\n")
        result.append("=".multiply(issue.@title.length()))
        result.append("\n")
        result.append(issue.@text)
        result.append("\n----\n")

        // If there are any extended properties, let's write those.
        if (issue.@extendedProperties.size() > 0) {
            def extOutput = [:]
            def maxKeyLen = 0
            def maxValLen = 0

            // Find the longest key and value, convert all to strings.
            issue.@extendedProperties.each { key, val ->
                def ks = key.toString(), vs = formatProperty(val)
                extOutput[ks] = vs
                if (ks.length() > maxKeyLen) { maxKeyLen = ks.length() }
                if (vs.length() > maxKeyLen) { maxValLen = vs.length() } }

            result.append("=".multiply(maxKeyLen + 1))
            result.append(" ")
            result.append("=".multiply(maxValLen))
            result.append("\n")

            extOutput.sort().each { key, val ->
                result.append(key.padRight(maxKeyLen))
                result.append(": ")
                result.append(val.padRight(maxValLen))
                result.append("\n") }

            result.append("=".multiply(maxKeyLen + 1))
            result.append(" ")
            result.append("=".multiply(maxValLen))
            result.append("\n") }}

    protected void writeFile() {
        try { source.write(formatIssue(this)) }
        catch (IOException ioe) {
            throw new IOException("I could not save the new text for this "
                + "issue. I can not write to the file for this issue. I do not"
                + " know why, I am sorry (maybe the file can not be reached).")
        }
    }

    public def parseValue(String value) {
        switch (value) {
            case ~/\d{4}-\d{2}-\d{2}/: return DateMidnight.parse(value)
            default: return value
        }
    }

    public String formatProperty(DateTime prop) {
        return prop.format("YYYY-MM-dd'T'HH:mm:ss") }

    public String formatProperty(DateMidnight prop) {
        return prop.format("YYYY-MM-dd") }
}
