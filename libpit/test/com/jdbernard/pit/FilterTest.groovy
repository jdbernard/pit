package com.jdbernard.pit

import org.junit.Test
import org.junit.Before
import org.junit.After

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

class FilterTest {

    Project proj

    @Before void setUpIssues() {

        proj = new MockProject('proj1')

        def issue = new MockIssue( '0000', Category.TASK, Status.NEW, 5)
        proj.issues['0000'] = issue

        issue = new MockIssue('0001', Category.BUG, Status.REJECTED, 3)
        proj.issues['0001'] = issue

        issue = new MockIssue('0002', Category.BUG, Status.RESOLVED, 9)
        proj.issues['0002'] = issue

        issue = new MockIssue('0003', Category.FEATURE, Status.REASSIGNED, 0)
        proj.issues['0003'] = issue

        def subProj = new MockProject('subproj1')
        proj.projects['subproj1'] = subProj

        subProj = new MockProject('subproj2')
        proj.projects['subproj2'] = subProj

    }

    @Test void testDefaultFilter() {
        Filter f = new Filter()

        proj.issues.values().each { assertTrue f.accept(it) }
        proj.projects.values().each { assertTrue f.accept(it) }
    }

    @Test void testPriorityIssueFilter() {
        Filter f = new Filter(priority: 9)
        
        proj.eachIssue { assertTrue f.accept(it) }

        f.priority = 6
        assertTrue  f.accept(proj.issues['0000'])
        assertTrue  f.accept(proj.issues['0001'])
        assertFalse f.accept(proj.issues['0002'])
        assertTrue  f.accept(proj.issues['0003'])

        f.priority = 5
        assertTrue  f.accept(proj.issues['0000'])
        assertTrue  f.accept(proj.issues['0001'])
        assertFalse f.accept(proj.issues['0002'])
        assertTrue  f.accept(proj.issues['0003'])

        f.priority = 0
        assertFalse f.accept(proj.issues['0000'])
        assertFalse f.accept(proj.issues['0001'])
        assertFalse f.accept(proj.issues['0002'])
        assertTrue  f.accept(proj.issues['0003'])

    }

    @Test void testCategoryFilter() {
        Filter f = new Filter(categories: 
            [Category.BUG, Category.FEATURE])

        assertFalse f.accept(proj.issues['0000'])
        assertTrue  f.accept(proj.issues['0001'])
        assertTrue  f.accept(proj.issues['0002'])
        assertTrue  f.accept(proj.issues['0003'])

        f.categories = [ Category.TASK ]
        assertTrue  f.accept(proj.issues['0000'])
        assertFalse f.accept(proj.issues['0001'])
        assertFalse f.accept(proj.issues['0002'])
        assertFalse f.accept(proj.issues['0003'])

        f.categories = [ Category.BUG, Category.TASK ]
        assertTrue  f.accept(proj.issues['0000'])
        assertTrue  f.accept(proj.issues['0001'])
        assertTrue  f.accept(proj.issues['0002'])
        assertFalse f.accept(proj.issues['0003'])

    }

    @Test void testStatusFilter() {
        Filter f = new Filter(status:
            [Status.NEW, Status.REASSIGNED, Status.REJECTED])

        assertTrue  f.accept(proj.issues['0000'])
        assertTrue  f.accept(proj.issues['0001'])
        assertFalse f.accept(proj.issues['0002'])
        assertTrue  f.accept(proj.issues['0003'])

        f.status = [ Status.RESOLVED ]
        assertFalse f.accept(proj.issues['0000'])
        assertFalse f.accept(proj.issues['0001'])
        assertTrue  f.accept(proj.issues['0002'])
        assertFalse f.accept(proj.issues['0003'])

        f.status = [ Status.NEW, Status.RESOLVED ]
        assertTrue  f.accept(proj.issues['0000'])
        assertFalse f.accept(proj.issues['0001'])
        assertTrue  f.accept(proj.issues['0002'])
        assertFalse f.accept(proj.issues['0003'])
    }

    @Test void testProjectFilter() {

    }

    @Test void testAcceptsProjectsFilter() {

    }

    @Test void testCompositeFilter() {

    }

}
