package com.jdbernard.pit.swing

import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import groovy.beans.Bindable

class ProjectPanelModel {

    // other GUI components
    def mainMVC
    def newIssueDialogMVC

    // data owned by this panel
    String id
    @Bindable Project rootProject
    @Bindable Project popupProject = null
    @Bindable Project selectedProject = null
    @Bindable Issue popupIssue = null

    // cache the ListModels
    def projectListModels = [:]
    def issueCellRenderer

    // local filter for projects and issues
    Filter filter
}