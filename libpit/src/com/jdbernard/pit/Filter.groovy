package com.jdbernard.pit

class Filter {

    List<Category> categories = null
    List<String> projects = null
    List<String> ids = null
    int priority = 9
    boolean acceptProjects = true
    Closure projectSorter = defaultIssueSorter
    Closure issueSorter = defaultProjectSorter

    public static Closure defaultIssueSorter = { it.id.toInteger() }
    public static Closure defaultProjectSorter = { it.name }

    public boolean accept(Issue i) {
        return (i.priority <= priority &&
                (!categories || categories.contains(i.category)) &&
                (!ids || ids.contains(i.id)))
    }

    public boolean accept(Project p) {
        return (acceptProjects && 
                (!projects || projects.contains(p.name)))
    }

    public boolean accept(String name) {
        return (acceptProjects && 
                (!projects || projects.contains(name)))
    }
}