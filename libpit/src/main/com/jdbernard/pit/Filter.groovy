package com.jdbernard.pit

class Filter {

    List<Category> categories = null
    List<Status> status = null
    List<String> projects = null
    List<String> ids = null
    Map<String, Object> extendedProperties = null
    int priority = 9
    boolean acceptProjects = true
    def issueSorter = defaultIssueSorter
    def projectSorter = defaultProjectSorter

    public static Closure defaultIssueSorter = { it.id.toInteger() }
    public static Closure defaultProjectSorter = { it.name }

    public boolean accept(Issue i) {
        return (
            // Needs to meet the priority threshold.
            i.priority <= priority &&
            // Needs to be in one of the filtered categories (if given)
            (!categories || categories.contains(i.category)) &&
            // Needs to have one of the filtered statuses (if given)
            (!status || status.contains(i.status)) &&
            // Needs to be one of the filtered ids (if given)
            (!ids || ids.contains(i.id)) &&
            // Needs to have all of the extended properties (if given)
            (!extendedProperties ||
                extendedProperties.every { name, value -> i[name] == value }))
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
