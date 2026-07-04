package net.k74n3xz.ecal.android.work

import androidx.work.ListenableWorker

internal const val MAX_WORKER_RETRY: Int = 7

internal fun retryOrFail(runAttemptCount: Int): ListenableWorker.Result =
    if (runAttemptCount < MAX_WORKER_RETRY) ListenableWorker.Result.retry()
    else ListenableWorker.Result.failure()