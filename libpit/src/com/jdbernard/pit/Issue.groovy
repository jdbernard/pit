package com.jdbernard.pit

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

    void setCategory(Category c) {
        this.category = c
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    void setPriority(int p) {
        if (p < 0) priority = 0
        else if (p > 9) priority = 9
        else priority = p
        source.renameTo(new File(source.canonicalFile.parentFile, getFilename()))
    }

    String getFilename() { return makeFilename(id, category, priority) }

    static String makeFilename(String id, Category category, int priority) {
        return id + category.symbol + priority + ".rst";
    }

    @Override
    String toString() { return "${id}(${priority}): ${category} ${title}" }
}
