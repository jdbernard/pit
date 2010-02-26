package com.jdbernard.pit

import org.junit.Test
import static org.junit.Assert.assertEquals

import static com.jdbernard.pit.Status.toStatus

public class StatusTest {

    @Test void testToStatus() {

        assertEquals Status.REASSIGNED, toStatus('REASSIGNED')
        assertEquals Status.REJECTED,   toStatus('REJECTED')
        assertEquals Status.NEW,        toStatus('NEW')
        assertEquals Status.RESOLVED  , toStatus('RESOLVED')
        assertEquals Status.VALIDATION_REQUIRED,
            toStatus('VALIDATION_REQUIRED')

        assertEquals Status.REASSIGNED, toStatus('REA')
        assertEquals Status.REJECTED,   toStatus('REJ')
        assertEquals Status.NEW,        toStatus('NEW')
        assertEquals Status.RESOLVED  , toStatus('RES')
        assertEquals Status.VALIDATION_REQUIRED,
            toStatus('VAL')

        assertEquals Status.REASSIGNED, toStatus('reassigned')
        assertEquals Status.REJECTED,   toStatus('rejected')
        assertEquals Status.NEW,        toStatus('new')
        assertEquals Status.RESOLVED  , toStatus('resolved')
        assertEquals Status.VALIDATION_REQUIRED,
            toStatus('validation_required')

        assertEquals Status.REASSIGNED, toStatus('rea')
        assertEquals Status.REJECTED,   toStatus('rej')
        assertEquals Status.NEW,        toStatus('new')
        assertEquals Status.RESOLVED  , toStatus('res')
        assertEquals Status.VALIDATION_REQUIRED,
            toStatus('val')

        assertEquals Status.REASSIGNED, toStatus('A')
        assertEquals Status.REJECTED,   toStatus('J')
        assertEquals Status.NEW,        toStatus('N')
        assertEquals Status.RESOLVED  , toStatus('S')
        assertEquals Status.VALIDATION_REQUIRED, toStatus('V')

        assertEquals Status.REASSIGNED, toStatus('a')
        assertEquals Status.REJECTED,   toStatus('j')
        assertEquals Status.NEW,        toStatus('n')
        assertEquals Status.RESOLVED  , toStatus('s')
        assertEquals Status.VALIDATION_REQUIRED, toStatus('v')

    }

}
