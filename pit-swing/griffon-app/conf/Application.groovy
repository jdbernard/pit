application {
	title="PitSwing"
	startupGroups=["PIT"]
	autoShutdown=true
}
mvcGroups {
	NewIssueDialog {
		model="com.jdbernard.pit.swing.NewIssueDialogModel"
		controller="com.jdbernard.pit.swing.NewIssueDialogController"
		view="com.jdbernard.pit.swing.NewIssueDialogView"
	}
	ProjectPanel {
		model="com.jdbernard.pit.swing.ProjectPanelModel"
		view="com.jdbernard.pit.swing.ProjectPanelView"
		controller="com.jdbernard.pit.swing.ProjectPanelController"
	}
	PIT {
		model="com.jdbernard.pit.swing.PITModel"
		view="com.jdbernard.pit.swing.PITView"
		controller="com.jdbernard.pit.swing.PITController"
	}
}
