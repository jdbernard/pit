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

        text = file.text
    }

    public void setCategory(Category c) {
        super.setCategory(c)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    public void setStatus(Status s) {
        super.setStatus(s)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    public void setPriority(int p) {
        super.setPriority(p)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    public String getFilename() {
        return makeFilename(id, category, status, priority)
    }

    public void setText(String text) {
        super.setText(text)
        source.write(text)
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
