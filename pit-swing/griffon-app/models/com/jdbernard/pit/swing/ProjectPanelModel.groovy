package com.jdbernard.pit.swing

import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import groovy.beans.Bindable

class ProjectPanelModel {
    def mainMVC

    @Bindable Project rootProject

    // cache the ListModels
    def projectListModels = [:]

    @Bindable Project popupProject = null

    @Bindable Project selectedProject = null

    @Bindable Issue popupIssue = null

    // filter for projects and issues
    Filter filter

    def newIssueDialogMVC

    String id

    def issueCellRenderer
}
