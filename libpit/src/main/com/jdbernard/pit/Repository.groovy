package com.jdbernard.pit

public abstract class Repository {

    public abstract void persist()
    public abstract Project[] getRootProjects()
    public abstract Project createNewProject(String name)
}
