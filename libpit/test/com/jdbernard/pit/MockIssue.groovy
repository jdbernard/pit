package com.jdbernard.pit

public class MockIssue extends Issue {
    public MockIssue(String id, Category c, int p) { super (id, c, p) }
    public boolean delete() { return true }
}
