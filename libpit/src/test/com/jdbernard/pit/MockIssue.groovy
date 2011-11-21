package com.jdbernard.pit

public class MockIssue extends Issue {
    public MockIssue(String id, Category c, Status s, int p) {
        super ([id: id, category: c, status: s, priority: p])
    }
    public boolean delete() { return true }
}
