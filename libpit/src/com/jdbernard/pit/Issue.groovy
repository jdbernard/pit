package com.jdbernard.pit

import java.lang.IllegalArgumentException as IAE

public abstract class Issue {

    protected String id
    protected Category category
    protected Status status
    protected int priority
    protected String text
    protected Date deliveryDate
    protected Date creationDate

    Issue(String id, Category c = Category.TASK, Status s = Status.NEW,
    int p = 9) {
        this.id = id
        this.category = c
        this.status = s
        this.priority = p
        this.creationDate = new Date()
        this.deliveryDate = null
    }

    public String getId() { return id; }

    public Category getCategory() { return category }

    public void setCategory(Category c) {
        if (c == null)
            throw new IAE("Category cannot be null.")

        this.category = c
    }

    public Status getStatus() { return status }

    public void setStatus(Status s) {
        if (s == null)
            throw new IAE("Status cannot be null.")

        this.status = s
    }

    public int getPriority() { return priority }

    public void setPriority(int p) { priority = Math.min(9, Math.max(0, p)) }

    public String getTitle() { return text.readLines()[0] }

    public String getText() { return text }

    public void setText(String t) { text = t }

    public boolean hasDelivery() { return deliveryDate == null }

    public Date getCreationDate() { return creationDate }

    public Date getDeliveryDate() { return deliveryDate }

    public void setDeliveryDate(Date dd) { deliveryDate = dd }

    @Override
    public String toString() { return "${id}(${priority}-${status}): ${category} ${title}" }

    public abstract boolean delete()
}
