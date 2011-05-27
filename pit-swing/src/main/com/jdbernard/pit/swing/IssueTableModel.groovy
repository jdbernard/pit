package com.jdbernard.pit.swing

import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

public class IssueTableModel extends AbstractTableModel {

    def issues = []
    boolean projectsVisible = false
    def categoryIcons = [:]
    def statusIcons = [:]

    public IssueTableModel(Project p, Filter f = null) {
        p.eachIssue(f) { issues << it }
    }

    public int getRowCount() {
        return issues.size
    }

    public int getColumnCount() {
        if (projectsVisible) return 5
        else return 4
    }

    public Object getValueAt(int row, int column) {
        if (row >= getRowCount() || column > getColumnCount())
            return null

        switch(column) {
            case 0: return getIcon(issues[row]); break
            case 1: return issues[row].id; break
            case 2: return "(" + issues[row].priority + "): "; break
            case 3: return issues[row].title; break
            case 4: return issues[row].project.name; break
            default: return "Invalid row index."; break
        }
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0: return Icon.class; break
            default: return String.class; break
        }
    }

    public String getColumnName(int column) {
        switch (column) {
            case 0: return "C/S"; break
            case 1: return "ID"; break
            case 2: return "P"; break
            case 3: return "Title/Summary"; break
            case 4: return "Project"; break
            default: return "ERR"; break
        }
    }

    public boolean isCellEditable(int row, int col) { return false }

    private Icon getIcon(Issue issue) {
        def icon
        if (categoryIcons[(issue.category)]) {
            icon = categoryIcons[(issue.category)]

            if (statusIcons[(issue.status)])
                icon = new CompositeIcon([icon, statusIcons[(issue.status)]])
        }

        return icon
    }

}
