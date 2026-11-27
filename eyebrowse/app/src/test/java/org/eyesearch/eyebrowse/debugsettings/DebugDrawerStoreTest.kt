package org.eyesearch.eyebrowse.debugsettings

import mozilla.components.support.test.ext.joinBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.eyesearch.eyebrowse.debugsettings.store.DebugDrawerAction
import org.eyesearch.eyebrowse.debugsettings.store.DebugDrawerState
import org.eyesearch.eyebrowse.debugsettings.store.DebugDrawerStore
import org.eyesearch.eyebrowse.debugsettings.store.DrawerStatus

class DebugDrawerStoreTest {

    @Test
    fun `GIVEN the drawer is closed WHEN the drawer is opened THEN the state should be set to open`() {
        val expected = DrawerStatus.Open
        val store = createStore()

        store.dispatch(DebugDrawerAction.DrawerOpened).joinBlocking()

        assertEquals(expected, store.state.drawerStatus)
    }

    @Test
    fun `GIVEN the drawer is opened WHEN the drawer is closed THEN the state should be set to closed`() {
        val expected = DrawerStatus.Closed
        val store = createStore(
            drawerStatus = DrawerStatus.Open,
        )

        store.dispatch(DebugDrawerAction.DrawerClosed).joinBlocking()

        assertEquals(expected, store.state.drawerStatus)
    }

    private fun createStore(
        drawerStatus: DrawerStatus = DrawerStatus.Closed,
    ) = DebugDrawerStore(
        initialState = DebugDrawerState(
            drawerStatus = drawerStatus,
        ),
    )
}
