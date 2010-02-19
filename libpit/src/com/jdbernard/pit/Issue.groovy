package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public class Issue {

    final String id
    Category category
    int priority
    String title
    String text
    File source

    Issue(File file) {

        def matcher = file.name =~ /(\d{4})([bftc])(\d).*/
        if (!matcher) return null

        this.source = file

        id = matcher[0][1]
        category = Category.toCategory(matcher[0][2])
        priority = matcher[0][3].toInteger()

        file.withReader { title = it.readLine() }
        text = file.text
    }

    /**
     */
    void setCategory(Category c) {

        if (category == null)
            throw new IAE("Category cannot be null.")

        this.category = c
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    void setPriority(int p) {

        // bounds check priority
        priority = Math.min(9, Math.max(0, priority))

        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    String getFilename() { return makeFilename(id, category, priority) }

    static String makeFilename(String id, Category category, int priority) {

        // bounds check priority
        priority = Math.min(9, Math.max(0, priority))

        //check for valid values of cateogry and id
        if (category == null)
            throw new IAE("Category must be non-null.")
        if (!(/\d+/ ==~ id))
            throw new IAE( "'${id}' is not a legal value for id.")
        
        return id + category.symbol + priority + ".rst";
    }

    @Override
    String toString() { return "${id}(${priority}): ${category} ${title}" }
}
