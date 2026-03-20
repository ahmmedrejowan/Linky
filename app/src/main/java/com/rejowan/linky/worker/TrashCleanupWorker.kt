package com.rejowan.linky.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rejowan.linky.data.local.database.dao.LinkDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically cleans up expired trash items.
 * Items in trash for more than 30 days are permanently deleted.
 */
class TrashCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val linkDao: LinkDao by inject()

    override suspend fun doWork(): Result {
        return try {
            Timber.d("TrashCleanupWorker: Starting cleanup...")

            // Calculate expiry timestamp (30 days ago)
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

            // Delete expired items
            val deletedCount = linkDao.deleteExpiredTrashItems(thirtyDaysAgo)

            if (deletedCount > 0) {
                Timber.i("TrashCleanupWorker: Deleted $deletedCount expired trash items")
            } else {
                Timber.d("TrashCleanupWorker: No expired items to delete")
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "TrashCleanupWorker: Failed to cleanup trash")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "trash_cleanup_work"
    }
}
