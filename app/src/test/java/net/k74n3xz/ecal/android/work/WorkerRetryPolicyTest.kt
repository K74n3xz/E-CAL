package net.k74n3xz.ecal.android.work

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkerRetryPolicyTest {
    @Test
    fun beforeRetryLimit_returnsRetry() {
        assertEquals("Retry", retryOrFail(MAX_WORKER_RETRY - 1).toString())
    }

    @Test
    fun atRetryLimit_returnsFailure() {
        assertEquals("Failure {mOutputData=Data {}}", retryOrFail(MAX_WORKER_RETRY).toString())
    }
}