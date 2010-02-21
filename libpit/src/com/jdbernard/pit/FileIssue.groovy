package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public class FileIssue extends Issue {

    File source

    FileIssue(File file) {

        /* I do not like this construction, but groovy automatically
         * calls obj.setProperty(...) when you type obj.property = ...
         * There is an exception for fields accessed withing the class
         * that defines them, it does not catt eh setter/getter, but
         * this exception does not extend to subclasses accessing member
         * variables of their parent class. So instead of using Issue's
         * default constructor and setting the id, category, and priority
         * fields here, we have to let Issue's constructor initialize
         * those values.*/

        super((file.name =~ /(\d+)([bcft])(\d).*/)[0][1],
            Category.toCategory((file.name =~ /(\d+)([bcft])(\d).*/)[0][2]),
            (file.name =~ /(\d+)([bcft])(\d).*/)[0][3].toInteger())

        //def matcher = file.name =~ /(\d{4})([bftc])(\d).*/
        /*if (!matcher) return null

        id = matcher[0][1]
        category = Category.toCategory(matcher[0][2])
        priority = matcher[0][3].toInteger()*/

        this.source = file

        file.withReader { title = it.readLine() }
        text = file.text
    }

    void setCategory(Category c) {
        super.setCategory(c)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    void setPriority(int p) {
        super.setPriority(p)
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    String getFilename() { return makeFilename(id, category, priority) }

    static boolean isValidFilename(String name) {
        return name ==~ /(\d+)([bcft])(\d).*/
    }

    static String makeFilename(String id, Category category, int priority) {

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
