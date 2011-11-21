package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public abstract class Issue {

    protected String id
    protected Category category
    protected Status status
    protected int priority
    protected String text
    protected String title

    Map extendedProperties = [:]

    Issue(Map props) {
        this.id = props.id
        this.category = props.category ?: Category.TASK
        this.status = props.status ?: Status.NEW
        this.priority = props.priority ?: 9
        this.title = props.title ?: ''
        this.text = props.text ?: ''

        def nativeProps =
            ["id", "category", "status", "priority", "title", "text"]

        props.each { key, val ->
            if (nativeProps.contains(key)) { return }
            this.extendedProperties[key] = val }}

    public String getId() { return id; }

    public Category getCategory() { return category }

    public void setCategory(Category c) throws IOException {
        if (c == null)
            throw new IAE("Category cannot be null.")

        this.category = c
    }

    public Status getStatus() { return status }

    public void setStatus(Status s) throws IOException {
        if (s == null)
            throw new IAE("Status cannot be null.")

        this.status = s
    }

    public int getPriority() { return priority }

    public void setPriority(int p) throws IOException {
        priority = Math.min(9, Math.max(0, p))
    }

    public String getTitle() { return title }

    public void setTitle(String t) throws IOException { title = t }

    public String getText() { return text }

    public void setText(String t) throws IOException { text = t }

    public def propertyMissing(String name) { extendedProperties[name] }

    public def propertyMissing(String name, def value) {
        extendedProperties[name] = value }

    @Override
    public String toString() {
        return "${id}(${priority}-${status}): ${category} ${title}"
    }

}
