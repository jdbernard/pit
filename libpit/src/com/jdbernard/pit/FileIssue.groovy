package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public class FileIssue extends Issue {

    protected File source
    public static final String fileExp = /(\d+)([bft])([ajnsv])(\d).*/

    public FileIssue(File file) {

        super('REPLACE_ME')

        def matcher = file.name =~ fileExp
        if (!matcher)
            throw new IllegalArgumentException("${file} " +
                "is not a valid Issue file.")

        super.@id = matcher[0][1]
        super.@category = Category.toCategory(matcher[0][2])
        super.@status = Status.toStatus(matcher[0][3])
        super.@priority = matcher[0][4].toInteger()

        this.source = file

        super.@text = file.text
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

    public void setText(String text) throws IOException {
        try { source.write(text) }
        catch (IOException ioe) {
            throw new IOException("I could not save the new text for this "
                + "issue. I can not write to the file for this issue. I do not"
                + " know why, I am sorry (maybe the file can not be reached).")
        }

        super.setText(text)
    }

    public boolean delete() { return source.delete() }

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

}
