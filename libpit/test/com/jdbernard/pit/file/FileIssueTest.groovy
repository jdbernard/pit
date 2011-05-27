package com.jdbernard.pit.file

import com.jdbernard.pit.*
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

        issueFile = new File(testDir, '0001fn1.rst')
        issueFile.write(
            "Add the killer feature to the killer app.\n" +
            "=========================================\n\n" +
            "Make our killer app shine!.")
        issues << new FileIssue(issueFile)

        issueFile = new File(testDir, '0002ts5.rst')
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

        try {
            issues[0].category = Category.TASK
            issues[1].category = Category.BUG
        } catch (Exception e) { 
            Assert.fail("An unexpected Exception occurred: "
                + e.getLocalizedMessage())
        }

        assertEquals issues[0].category, Category.TASK
        assertEquals issues[1].category, Category.BUG

        assertTrue new File(testDir, '0001tn1.rst').exists()
        assertTrue new File(testDir, '0002bs5.rst').exists()
        assertFalse new File(testDir, '0001fn1.rst').exists()
        assertFalse new File(testDir, '0002ts5.rst').exists()

    }

    @Test void testSetCategoryFails() {
        FileInputStream fin
        try {
            // get a lock to the file to prevent the rename
            def issueFile = new File('0001fn1.rst')
            fin = new FileInputStream(issueFile)

            // try to set the category
            issues[0].category = Category.TASK

            // should throw IOE before here
            Assert.fail()
        } catch (IOException ioe) {
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getLocalizedMessage())
        } finally {
            if (fin != null) fin.close()
        }
    }

    @Test void testSetStatus() {

        assertEquals issues[0].status, Status.NEW
        assertEquals issues[1].status, Status.RESOLVED

        try {
            issues[0].status = Status.RESOLVED
            issues[1].status = Status.REJECTED
        } catch (Exception e) {
            Assert.fail("An unexpected Exception occurred: "
                + e.getLocalizedMessage())
        }

        assertTrue new File(testDir, '0001fs1.rst').exists()
        assertTrue new File(testDir, '0002tj5.rst').exists()
        assertFalse new File(testDir, '0001fn1.rst').exists()
        assertFalse new File(testDir, '0002ts5.rst').exists()
    }

    @Test void testSetStatusFails() {
        FileInputStream fin
        try {
            // get a lock to the file to prevent the rename
            def issueFile = new File('0001fn1.rst')
            fin = new FileInputStream(issueFile)

            // try to set the status
            issues[0].status = Status.REJECTED

            // should throw IOE before here
            Assert.fail()
        } catch (IOException ioe) {
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getLocalizedMessage())
        } finally {
            if (fin != null) fin.close()
        }
    }
    
    @Test void testSetPriority() {

        assertEquals issues[0].priority, 1
        assertEquals issues[1].priority, 5

        try {
            issues[0].priority = 2
            issues[1].priority = 9
        } catch (Exception e) {
            Assert.fail("An unexpected Exception occurred: "
                + e.getLocalizedMessage())
        }

        assertEquals issues[0].priority, 2
        assertEquals issues[1].priority, 9
        
        assertTrue new File(testDir, '0001fn2.rst').exists()
        assertTrue new File(testDir, '0002ts9.rst').exists()
        assertFalse new File(testDir, '0001fn1.rst').exists()
        assertFalse new File(testDir, '0002ts5.rst').exists()
    }

    @Test void testSetPriorityFails() {
        FileInputStream fin
        try {
            // get a lock to the file to prevent the rename
            def issueFile = new File('0001fn1.rst')
            fin = new FileInputStream(issueFile)

            // try to set the priority
            issues[0].priority = 9

            // should throw IOE before here
            Assert.fail()
        } catch (IOException ioe) {
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getLocalizedMessage())
        } finally {
            if (fin != null) fin.close()
        }
    }

    @Test void testConstruction() {
        File issueFile = new File(testDir, '0001fn1.rst')
        Issue issue = new FileIssue(issueFile)

        assertEquals issue.id        , "0001"
        assertEquals issue.category  , Category.FEATURE
        assertEquals issue.status    , Status.NEW
        assertEquals issue.priority  , 1
        assertEquals issue.title     , "Add the killer feature to the killer app."
        assertEquals issue.text      , "Add the killer feature to the killer app.\n" +
                                      "=========================================\n\n" +
                                      "Make our killer app shine!."
        assertEquals issue.source    , issueFile
    }

    @Test void testSetTextFails() {
        try {
            // make the issue file un-writable
            def issueFile = new File('0001fn1.rst')
            if (issueFile.setReadOnly()) {

                // try to write something
                issues[0].text = "This should fail to be written."

                // should throw IOE before here
                Assert.fail()
            } else {
                println "Could not run testSetTextFails, unable to change " +
                    "the test isseu file's permissions."
            }
        } catch (IOException ioe) {
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getLocalizedMessage())
        }
    }

    @Test void testMakeFilename() {
        assertEquals FileIssue.makeFilename('0001', Category.BUG,
            Status.NEW, 5),         '0001bn5.rst'
        assertEquals FileIssue.makeFilename('0010', Category.FEATURE,
            Status.REASSIGNED, 1),  '0010fa1.rst'
        assertEquals FileIssue.makeFilename('0002', Category.FEATURE,
            Status.REJECTED, 3),    '0002fj3.rst'
        assertEquals FileIssue.makeFilename('0001', Category.BUG,
            Status.RESOLVED, -2),   '0001bs0.rst'
        assertEquals FileIssue.makeFilename('0001', Category.TASK,
            Status.VALIDATION_REQUIRED, 10)  , '0001tv9.rst'
        assertEquals FileIssue.makeFilename('00101', Category.BUG,
            Status.NEW, 5),         '00101bn5.rst'

        try {
            FileIssue.makeFilename('badid', Category.BUG, Status.NEW, 5)
            assertTrue 'Issue.makeFilename() succeeded with bad id input.', false
        } catch (IllegalArgumentException iae) {}

        try {
            FileIssue.makeFilename('0002', null, Status.NEW, 5)
            assertTrue 'Issue.makeFilename() succeeded given no Category.', false
        } catch (IllegalArgumentException iae) {}

        try {
            FileIssue.makeFilename('0002', Category.BUG, null, 5)
            assertTrue 'Issue.makeFilename() succeeded given no Status.', false
        } catch (IllegalArgumentException iae) {}
    }
}
