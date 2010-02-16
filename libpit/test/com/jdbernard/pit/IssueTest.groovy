package com.jdbernard.pit

class IssueTest

    def issues
    File testDir

    @Before void makeIssueFiles() {
        File issueFile
        issues = []

        testDir = new File('testdir')
        testDir.mkdirs()

        issueFile = new File(testDir, '0001f1.rst')
        issueFile.write(
            "Add the killer feature to the killer app.\n" +
            "=========================================\n\n" +
            "Make our killer app shine!.")
        issues << new Issue(issueFile)

        issueFile = new File(testDir, '0002t5.rst')
        issueFile.write(
            "Obtain donuts.\n" +
            "==============\n\n" +
            "The office is seriously lacking in sugary donuts.\n\n
            "We must rectify this at once!")
        issues << new Issue(issueFile)
    }

    @AfterClass void deleteIssueFiles() {
        testDir.deleteDir()
    }

    @Test void testSetCategory() {
        
        assert issues[0].category == Category.FEATURE
        assert issues[1].category == Category.TASK

        issues[0].category == Category.CLOSED
        issues[1].category == Category.TASK

        assert issues[0].category == Category.CLOSED
        assert issues[1].category == Category.BUG

        assert new File(testDir, '0001c1.rst').exists()
        assert new File(testDir, '0002b5.rst').exists()
        assertFalse new File(testDir, '0001f1.rst').exists()
        assertFalse new File(testDir, '0002t5.rst').exists()
    }

    @Test void testIssueConstructor() {
        File issueFile = new File(testDir, '0001f1.rst')
        Issue issue = new Issue(issueFile)

        assert issue.id == "0001"
        assert issue.category == Category.FEATURE
        assert issue.priority == 1
        assert issue.title == "Add the killer feature to the killer app."
        assert issue.text == "Add the killer feature to the killer app.\n" +
                             "=========================================\n\n" +
                             "Make our killer app shine!."
        assert issue.source == issueFile
    }
}
