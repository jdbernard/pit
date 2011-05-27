package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.Status
import groovy.beans.Bindable
import java.awt.Font
import javax.swing.ImageIcon

class PITModel {

    // filter for projects and classes
    Filter filter = new Filter(categories: [],
        status: [Status.NEW, Status.VALIDATION_REQUIRED])

    def issueListRenderer

    // map of category -> issue template
    Map<Category, String> templates = [:]

    String issueCSS = getClass().getResourceAsStream("/default-issue.css").text
    
    Map<Category, ImageIcon> categoryIcons = [:]
    Map<Category, ImageIcon> statusIcons = [:]

    def newIssueDialogMVC
    Map projectPanelMVCs = [:]

    Map projectIdMap = [:]

    @Bindable Font issueDetailFont = new Font(Font.MONOSPACED, Font.PLAIN, 10)
}
