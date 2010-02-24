package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public abstract class Issue {

    protected String id
    protected Category category
    protected int priority
    protected String text

    Issue(String id, Category c = Category.TASK, int p = 9) {
        this.id = id
        this.category = c
        this.priority = p
    }

    public String getId() { return id; }

    public Category getCategory() { return category }

    public void setCategory(Category c) {
        if (c == null)
            throw new IAE("Category cannot be null.")

        this.category = c
    }

    public int getPriority() { return priority }

    public void setPriority(int p) { priority = Math.min(9, Math.max(0, p)) }

    public String getTitle() { return text.readLines()[0] }

    public String getText() { return text }

    public void setText(String t) { text = t }

    @Override
    public String toString() { return "${id}(${priority}): ${category} ${title}" }

    public abstract boolean delete()
}
