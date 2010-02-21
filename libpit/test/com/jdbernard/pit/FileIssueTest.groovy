package com.jdbernard.pit

import org.junit.*
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertEquals

class FileIssueTest {

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
        issues << new FileIssue(issueFile)

        issueFile = new File(testDir, '0002t5.rst')
        issueFile.write(
            "Obtain donuts.\n" +
            "==============\n\n" +
            "The office is seriously lacking in sugary donuts.\n\n" +
            "We must rectify this at once!")
        issues << new FileIssue(issueFile)
    }

    @After void deleteIssueFiles() {
        assert testDir.deleteDir()
    }

    @Test void testSetCategory() {
        
        assertEquals issues[0].category, Category.FEATURE
        assertEquals issues[1].category, Category.TASK

        issues[0].category = Category.CLOSED
        issues[1].category = Category.BUG

        assertEquals issues[0].category, Category.CLOSED
        assertEquals issues[1].category, Category.BUG

        assertTrue new File(testDir, '0001c1.rst').exists()
        assertTrue new File(testDir, '0002b5.rst').exists()
        assertFalse new File(testDir, '0001f1.rst').exists()
        assertFalse new File(testDir, '0002t5.rst').exists()
    }

    @Test void testSetPriority() {

        assertEquals issues[0].priority, 1
        assertEquals issues[1].priority, 5

        issues[0].priority = 2
        issues[1].priority = 9

        assertEquals issues[0].priority, 2
        assertEquals issues[1].priority, 9
        
        assertTrue new File(testDir, '0001f2.rst').exists()
        assertTrue new File(testDir, '0002t9.rst').exists()
        assertFalse new File(testDir, '0001f1.rst').exists()
        assertFalse new File(testDir, '0002t5.rst').exists()
    }

    @Test void testConstruction() {
        File issueFile = new File(testDir, '0001f1.rst')
        Issue issue = new FileIssue(issueFile)

        assertEquals issue.id        , "0001"
        assertEquals issue.category  , Category.FEATURE
        assertEquals issue.priority  , 1
        assertEquals issue.title     , "Add the killer feature to the killer app."
        assertEquals issue.text      , "Add the killer feature to the killer app.\n" +
                                      "=========================================\n\n" +
                                      "Make our killer app shine!."
        assertEquals issue.source    , issueFile
    }

    @Test void testMakeFilename() {
        assertEquals FileIssue.makeFilename('0001', Category.BUG, 5)    , '0001b5.rst'
        assertEquals FileIssue.makeFilename('0010', Category.FEATURE, 1), '0010f1.rst'
        assertEquals FileIssue.makeFilename('0002', Category.CLOSED, 3) , '0002c3.rst'
        assertEquals FileIssue.makeFilename('0001', Category.BUG, -2)   , '0001b0.rst'
        assertEquals FileIssue.makeFilename('0001', Category.TASK, 10)  , '0001t9.rst'
        assertEquals FileIssue.makeFilename('00101', Category.BUG, 5)   , '00101b5.rst'

        try {
            FileIssue.makeFilename('badid', Category.BUG, 5)
            assertTrue 'Issue.makeFilename() succeeded with bad id input.', false
        } catch (IllegalArgumentException iae) {}

        try {
            FileIssue.makeFilename('0002', null, 5)
            assertTrue 'Issue.makeFilename() succeeded given no Category.', false
        } catch (IllegalArgumentException iae) {}
    }
}
