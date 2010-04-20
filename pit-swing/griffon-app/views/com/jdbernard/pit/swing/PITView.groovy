package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.FileProject
import groovy.beans.Bindable
import java.awt.BorderLayout as BL
import java.awt.GridBagConstraints as GBC
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import net.miginfocom.swing.MigLayout

import java.awt.Color

actions {
    action(
        id: 'openProject',
        name: 'Open...',
        icon: imageIcon('/folder.png'),
        accelerator: shortcut('O'),
        closure: controller.openProject
    )

    action(
        id: 'closeProject',
        name: 'Close',
        enabled: bind { projectPanelMVCs.size() > 0 },
        closure: controller.closeProject
    )

    action(
        id: 'shutdown',
        name: 'Exit',
        icon: imageIcon('/shutdown.png'),
        accelerator: shortcut('x'),
        closure: controller.shutdown
    )
}

// initialize category-related view data
Category.values().each {
    model.categoryIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
    model.filter.categories.add(it)
}

Status.values().each {
    model.statusIcons[(it)] = imageIcon("/${it.name().toLowerCase()}.png")
}


openDialog = fileChooser(fileSelectionMode: JFileChooser.DIRECTORIES_ONLY)

frame = application(title:'Personal Issue Tracker',
  minimumSize: [800, 500],
  pack:true,
  locationRelativeTo: null,
  iconImage: imageIcon('/icon64x64.png').image,
  iconImages: [imageIcon('/icon64x64.png').image,
               imageIcon('/icon32x32.png').image,
               imageIcon('/icon16x16.png').image]
) {

    // main menu
    menuBar() {
        menu("File") {
            menuItem(openProject)
            menuItem(closeProject)
            separator()
            menuItem(shutdown)
        }

        menu('View') {
            menu('Category') {
                Category.values().each { cat ->
                    checkBoxMenuItem(cat.toString(),
                        selected: model.filter.categories.contains(cat),
                        actionPerformed: { evt ->
                            if (model.filter.categories.contains(cat)) {
                                model.filter.categories.remove(cat)
                                evt.source.selected = false
                            } else {
                                model.filter.categories.add(cat)
                                evt.source.selected = true
                            }
                            controller.refreshIssues()
                        })
                }
            }

            menu('Status') {
                Status.values().each { st ->
                    checkBoxMenuItem(st.toString(),
                        selected: model.filter.status.contains(st),
                        actionPerformed: { evt ->
                            if (model.filter.status.contains(st)) {
                                model.filter.status.remove(st)
                                evt.source.selected = false
                            } else {
                                model.filter.status.add(st)
                                evt.source.selected = true
                            }
                            controller.refreshIssues()
                        })
                }
            }

            separator()

            menuItem('Detail Text Size...',
                actionPerformed: {
                    def newSize = JOptionPane.showInputDialog(frame,
                        'New text size: ', 'Change Issue Detail Text Size...',
                        JOptionPane.QUESTION_MESSAGE)
                    if (newSize == null || !newSize.isFloat())
                        JOptionPane.showMessageDialog(frame,
                            "$newSize is not a valid size.",
                            'Change Issue Detail Text Size...',
                            JOptionPane.ERROR_MESSAGE)
                    else model.issueDetailFont = model.issueDetailFont
                        .deriveFont(newSize.toFloat())
                }) 
        }

        menu('Sort') {
            sortMenuButtonGroup = buttonGroup()
            checkBoxMenuItem('By ID', 
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.id }
                    controller.refreshIssues()
                })
            checkBoxMenuItem('By Category',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.category }
                    controller.refreshIssues()
                })
            checkBoxMenuItem('By Status',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.status }
                    controller.refreshIssues()
                })
            checkBoxMenuItem('By Priority',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.priority }
                    controller.refreshIssues()
                })
            checkBoxMenuItem('By Title',
                buttonGroup: sortMenuButtonGroup,
                actionPerformed: {
                    model.filter.issueSorter = { it.title }
                    controller.refreshIssues()
                })
        }
    }

    mainTabbedPane = tabbedPane() {

    }
}
