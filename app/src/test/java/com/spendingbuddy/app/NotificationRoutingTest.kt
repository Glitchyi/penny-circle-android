package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRoutingTest {
    @Test fun postingTodayAndCircleRemindersCannotReplaceTheAddActionIdentity() {
        val destinations = listOf(Destinations.ADD, Destinations.TODAY, Destinations.CIRCLE, Destinations.BUDDY)
        val identities = destinations.map(::notificationActivityAction)
        assertEquals(destinations.size, identities.toSet().size)
        assertNotEquals(notificationActivityAction(Destinations.ADD), notificationActivityAction(Destinations.TODAY))
        // Keep the established action namespace used by automatic-payment notifications.
        assertEquals("com.spendingbuddy.app.OPEN.today", notificationActivityAction(Destinations.TODAY))
    }

    @Test fun warmTodayAndAddSelectTodayBeforeAnySheetIsOpened() {
        assertEquals(Destinations.TODAY, notificationTopLevelRoute(Destinations.TODAY))
        assertEquals(Destinations.TODAY, notificationTopLevelRoute(Destinations.ADD))
        assertEquals(Destinations.ACTIVITY, notificationTopLevelRoute(Destinations.ACTIVITY))
    }

    @Test fun preRenameNotificationsStillSelectCircleAndOtherRequestsKeepTheirOwnFlow() {
        assertEquals(Destinations.CIRCLE, notificationTopLevelRoute(Destinations.BUDDY))
        assertEquals(Destinations.CIRCLE, notificationTopLevelRoute(Destinations.CIRCLE))
        assertNull(notificationTopLevelRoute(Destinations.SETTINGS))
        assertNull(notificationTopLevelRoute(Destinations.IMPORT))
        assertNull(notificationTopLevelRoute(Destinations.EDIT_PREFIX + "purchase-1"))
        assertNull(notificationTopLevelRoute(null))
        assertNull(notificationTopLevelRoute("unrecognized"))
    }
}
