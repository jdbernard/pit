package com.jdbernard.pit

public class MockIssue extends Issue {
    public MockIssue(String id, Category c, Status s, int p) {
        super (id, c, s, p)
    }
    public boolean delete() { return true }
}
