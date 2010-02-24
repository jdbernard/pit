package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public class FileIssue extends Issue {

    protected File source

    public FileIssue(File file) {

        super('REPLACE_ME')

        def matcher = file.name =~ /(\d{4})([bftc])(\d).*/
        if (!matcher)
            throw new IllegalArgumentException("${file} " +
                "is not a valid Issue file.")

        super.@id = matcher[0][1]
        super.@category = Category.toCategory(matcher[0][2])
        super.@priority = matcher[0][3].toInteger()

        this.source = file

        text = file.text
    }

    public void setCategory(Category c) {
        super.setCategory(c)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    public void setPriority(int p) {
        super.setPriority(p)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    public String getFilename() { return makeFilename(id, category, priority) }

    public void setText(String text) {
        super.setText(text)
        source.write(text)
    }

    public boolean delete() { return source.delete() }

    public static boolean isValidFilename(String name) {
        return name ==~ /(\d+)([bcft])(\d).*/
    }

    public static String makeFilename(String id, Category category,
    int priority) {

        // bounds check priority
        priority = Math.min(9, Math.max(0, priority))

        //check for valid values of cateogry and id
        if (category == null)
            throw new IAE("Category must be non-null.")
        if (!(id ==~ /\d+/))
            throw new IAE( "'${id}' is not a legal value for id.")
        
        return id + category.symbol + priority + ".rst";
    }

}
