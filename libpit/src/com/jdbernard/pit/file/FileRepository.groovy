package com.jdbernard.pit.file

import com.jdbernard.pit.*

public class FileRepository extends Repository {

    @Delegate FileProject fileProject

    public FileRepository(File dir) {
        assert dir.isDirectory()
        fileProject = new FileProject(dir)
    }
    
    public void persist() {} // nothing to do
    public Project[] getRootProjects() {
        return [fileProject] as Project[]
    }

    public FileProject createNewProject(String name) {
        return fileProject.createNewProject()
    }
}
