package com.jdbernard.pit.xml

import com.jdbernard.pit.*
import groovy.util.Node
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

public class XmlIssueTest {

    Node issueNode = new Node(null, 'Issue',
        [id: '0000', category: 'BUG', status: 'RESOLVED', priority: 1],
        'Test Issue')

    @Test public void testDummyTest() {}

    /*@Test public void testNodeConstructor() {
        XmlIssue issue = new XmlIssue(issueNode)

        assertEquals issue.text, 'Test Issue'
        assertEquals issue.id, '0000'
        assertEquals issue.category, Category.BUG
        assertEquals issue.status, Status.RESOLVED
        assertEquals issue.priority, 1
    }*/
}
