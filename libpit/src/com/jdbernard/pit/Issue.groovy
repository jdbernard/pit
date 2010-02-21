package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public class Issue {

    String id
    Category category
    int priority
    String title
    String text

    Issue(String id, Category c = Category.TASK, int p = 9) {
        this.id = id
        this.category = c
        this.priority = p
    }

    void setCategory(Category c) {
        if (c == null)
            throw new IAE("Category cannot be null.")

        this.category = c
    }

    void setPriority(int p) { priority = Math.min(9, Math.max(0, p)) }

    @Override
    String toString() { return "${id}(${priority}): ${category} ${title}" }
}
