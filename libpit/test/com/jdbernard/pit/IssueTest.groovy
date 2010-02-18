package com.jdbernard.pit

import org.junit.*
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

class IssueTest {

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
            "The office is seriously lacking in sugary donuts.\n\n" +
            "We must rectify this at once!")
        issues << new Issue(issueFile)
    }

    @After void deleteIssueFiles() {
        testDir.deleteDir()
    }

    @Test void testSetCategory() {
        
        assertTrue issues[0].category == Category.FEATURE
        assertTrue issues[1].category == Category.TASK

        issues[0].category = Category.CLOSED
        issues[1].category = Category.BUG

        assertTrue issues[0].category == Category.CLOSED
        assertTrue issues[1].category == Category.BUG

        assertTrue new File(testDir, '0001c1.rst').exists()
        assertTrue new File(testDir, '0002b5.rst').exists()
        assertFalse new File(testDir, '0001f1.rst').exists()
        assertFalse new File(testDir, '0002t5.rst').exists()
    }

    @Test void testSetPriority() {

        assertTrue issues[0].priority == 1
        assertTrue issues[1].priority == 5

        issues[0].priority = 2
        issues[1].priority = 9

        assertTrue issues[0].priority == 2
        assertTrue issues[1].priority == 9
        
        assertTrue new File(testDir, '0001f2.rst').exists()
        assertTrue new File(testDir, '0002t9.rst').exists()
        assertFalse new File(testDir, '0001f1.rst').exists()
        assertFalse new File(testDir, '0002t5.rst').exists()
    }

    @Test void testConstruction() {
        File issueFile = new File(testDir, '0001f1.rst')
        Issue issue = new Issue(issueFile)

        assertTrue issue.id == "0001"
        assertTrue issue.category == Category.FEATURE
        assertTrue issue.priority == 1
        assertTrue issue.title == "Add the killer feature to the killer app."
        assertTrue issue.text == "Add the killer feature to the killer app.\n" +
                             "=========================================\n\n" +
                             "Make our killer app shine!."
        assertTrue issue.source == issueFile
    }
}
