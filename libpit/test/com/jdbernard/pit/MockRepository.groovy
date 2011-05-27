package com.jdbernard.pit

class MockRepository extends Repository {

    public void persist() {}

    public Project[] getRootProjects() { return [] as Project[] }

    public Project createNewProject(String name) {
        return new MockProject(name)
    }
}
