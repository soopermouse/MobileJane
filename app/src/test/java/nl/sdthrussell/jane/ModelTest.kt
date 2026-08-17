package nl.sdthrussell.jane

import nl.sdthrussell.jane.model.ChatRequest
import nl.sdthrussell.jane.model.GoalRecord
import nl.sdthrussell.jane.model.JaneSnapshot
import nl.sdthrussell.jane.model.ProjectRequest
import nl.sdthrussell.jane.model.TextSkillRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModelTest {
    @Test
    fun defaultSnapshotStartsEmpty() {
        val snapshot = JaneSnapshot()
        assertEquals(0, snapshot.projects.size)
        assertEquals(0, snapshot.goals.size)
        assertEquals(0, snapshot.alerts.size)
        assertEquals("unknown", snapshot.lastHealth.status)
    }

    @Test
    fun currentAgentRequestModelsHaveBodyFields() {
        assertEquals("hello", ChatRequest("hello").message)
        assertEquals("English", TextSkillRequest("letter").translateTo)
        assertEquals("active", ProjectRequest("Jane Mobile").status)
    }

    @Test
    fun goalsAreOpenByDefault() {
        assertFalse(GoalRecord("1", "Finish Jane Mobile", createdAt = 1L).completed)
    }
}
