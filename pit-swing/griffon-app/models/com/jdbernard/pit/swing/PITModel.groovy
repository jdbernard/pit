package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.Status
import groovy.beans.Bindable
import java.awt.Font

class PITModel {
    // filter for projects and issues
    Filter filter = new Filter(categories: [],
        status: [Status.NEW, Status.VALIDATION_REQUIRED])

    def issueListRenderer

    // map of category -> issue template
    def templates = [:]

    def categoryIcons = [:]
    def statusIcons = [:]

    def newIssueDialogMVC
    def projectPanelMVCs = [:]

    def projectIdMap = [:]

    @Bindable issueDetailFont = new Font(Font.MONOSPACED, Font.PLAIN, 10)
}
