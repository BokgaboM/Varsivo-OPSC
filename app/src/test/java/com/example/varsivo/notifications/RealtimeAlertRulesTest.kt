package com.example.varsivo.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeAlertRulesTest {

    @Test fun aChangedStatus_triggersAnAlert() {
        assertTrue(RealtimeAlertRules.isNewStatus("SUBMITTED", "OFFER"))
    }

    @Test fun theFirstReadOfAnApplication_doesNotAlert() {
        assertFalse(RealtimeAlertRules.isNewStatus(null, "SUBMITTED"))
    }

    @Test fun anUnchangedStatus_doesNotAlert() {
        assertFalse(RealtimeAlertRules.isNewStatus("OFFER", "OFFER"))
    }

    @Test fun aMissingStatus_doesNotAlert() {
        assertFalse(RealtimeAlertRules.isNewStatus("OFFER", null))
    }
}
