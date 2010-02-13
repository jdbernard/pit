package com.jdbernard.pit

public class Issue {

    String id
    Category category
    int priority
    String title
    String text

    Issue(File file) {

        def matcher = file.name =~ /(\d{4})([bftc])(\d).*/
        if (!matcher) return null

        id = matcher[0][1]
        category = Category.toCategory(matcher[0][2])
        priority = matcher[0][3].toInteger()

        file.withReader { title = it.readLine() }
        text = file.text
    }

}
