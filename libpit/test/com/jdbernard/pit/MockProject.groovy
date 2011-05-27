package com.jdbernard.pit

class MockProject extends Project {

    public MockProject(String name) { super(name) }

    public Issue createNewIssue(Map options) {
        return new MockIssue(options.id ?: 'n/a',
            options.c ?: Category.TASK, options.s ?: Status.NEW,
            options.p ?: 5)
    }

    public Project createNewProject(String name) {
        return new MockProject(name)
    }

    public boolean delete() { return true }
}
