package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import java.awt.Component
import javax.swing.Icon
import javax.swing.JList
import javax.swing.DefaultListCellRenderer

public class IssueListCellRenderer extends DefaultListCellRenderer {

    Map<Category, Icon> categoryIcons
    Map<Status, Icon> statusIcons
    
    public Component getListCellRendererComponent(JList list, Object value,
    int index, boolean selected, boolean hasFocus) {
        def component = super.getListCellRendererComponent(list, value, index,
            selected, hasFocus)
        def icon
        if (categoryIcons[(value.category)]) {
            icon = categoryIcons[(value.category)]

            if (statusIcons[(value.status)])
                icon = new CompositeIcon([icon, statusIcons[(value.status)]])
        }

        if (icon) setIcon(icon)

        component.text = "<html><tt>${value.id} (${value.priority}): </tt>" +
            "${value.title}</html>"
        return component
    }
}
