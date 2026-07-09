package net.k74n3xz.ecal.utils

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentUidGeneratorTest {
    @Test
    fun generateEventUid_returnsUuidV7WithEventSuffix() {
        val uid = generateEventUid()
        val uuid = UUID.fromString(uid.removeSuffix("-ECAL_event"))

        assertTrue(uid.endsWith("-ECAL_event"))
        assertEquals(7, uuid.version())
    }

    @Test
    fun generateEventUid_isUniqueAcrossBatch() {
        val generated = List(1_000) { generateEventUid() }

        assertEquals(generated.size, generated.toSet().size)
    }
}
