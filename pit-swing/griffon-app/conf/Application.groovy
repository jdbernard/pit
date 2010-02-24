application {
    title='PitSwing'
    startupGroups = ['PIT']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "com.jdbernard.pit.swing.PIT"
    'PIT' {
        model = 'com.jdbernard.pit.swing.PITModel'
        view = 'com.jdbernard.pit.swing.PITView'
        controller = 'com.jdbernard.pit.swing.PITController'
    }

}
