application {
    title='PitSwing'
    startupGroups = ['PIT']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "com.jdbernard.pit.swing.NewIssueDialog"
    'NewIssueDialog' {
        model = 'com.jdbernard.pit.swing.NewIssueDialogModel'
        controller = 'com.jdbernard.pit.swing.NewIssueDialogController'
        view = 'com.jdbernard.pit.swing.NewIssueDialogView'
    }

    // MVC Group for "com.jdbernard.pit.swing.ProjectPanel"
    'ProjectPanel' {
        model = 'com.jdbernard.pit.swing.ProjectPanelModel'
        view = 'com.jdbernard.pit.swing.ProjectPanelView'
        controller = 'com.jdbernard.pit.swing.ProjectPanelController'
    }

    // MVC Group for "com.jdbernard.pit.swing.PIT"
    'PIT' {
        model = 'com.jdbernard.pit.swing.PITModel'
        view = 'com.jdbernard.pit.swing.PITView'
        controller = 'com.jdbernard.pit.swing.PITController'
    }

}
