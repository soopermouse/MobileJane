package nl.sdthrussell.jane

import nl.sdthrussell.jane.model.JaneSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {
    @Test
    fun defaultSnapshotIsJane() {
        assertEquals("Jane", JaneSnapshot().status.name)
    }
}
