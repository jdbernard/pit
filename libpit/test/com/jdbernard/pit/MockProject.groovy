package com.jdbernard.pit

class MockProject extends Project {

    public MockProject(String name) { super(name) }

    public Issue createNewIssue(Map options) {
        throw new UnsupportedOperationException()
    }
}
