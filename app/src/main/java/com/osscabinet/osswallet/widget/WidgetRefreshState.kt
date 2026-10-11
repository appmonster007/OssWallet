package com.osscabinet.ossWallet.widget

/**
 * Pure bookkeeping behind [WidgetRefreshCoordinator]: what still needs refreshing, and whether a
 * worker is already responsible for draining it. No Android types, so it is unit-testable.
 *
 * Data changes bump [requestedVersion]; a full pass snapshots that version *before* it reads the
 * database and only advances [servedVersion] to the snapshot once it finishes. A write that lands
 * mid-pass therefore leaves requested > served, and the worker loops once more with fresh data.
 * The newest pass always renders last, so an older run can never overwrite a newer one.
 *
 * Single-widget requests (resize, config save, onUpdate) are tracked separately and never
 * invalidate a data pass.
 */
class WidgetRefreshState {

    sealed interface Pass {
        /** Refresh every placed widget; [version] is the data version read for this pass. */
        data class All(val version: Long) : Pass

        /** Refresh only these appWidgetIds. */
        data class Widgets(val ids: Set<Int>) : Pass
    }

    private val lock = Any()
    private var requestedVersion = 0L
    private var servedVersion = 0L
    private val pendingWidgets = linkedSetOf<Int>()
    private var workerClaimed = false

    /** Marks a data/settings change. Returns true if the caller must enqueue a worker. */
    fun requestAll(): Boolean = synchronized(lock) {
        requestedVersion++
        claimWorker()
    }

    /** Marks specific widgets dirty. Returns true if the caller must enqueue a worker. */
    fun requestWidgets(ids: Collection<Int>): Boolean = synchronized(lock) {
        if (ids.isEmpty()) return false
        pendingWidgets.addAll(ids)
        claimWorker()
    }

    /** Called when a worker starts, including a WorkManager retry that never went through a request. */
    fun attach() = synchronized(lock) { workerClaimed = true }

    /**
     * Next unit of work, or null when nothing is pending. Returning null and releasing the worker
     * claim happen under one lock, so a request can never land in a gap where nobody drains it.
     */
    fun beginPass(): Pass? = synchronized(lock) {
        when {
            requestedVersion > servedVersion -> {
                // A full pass re-renders every widget, so queued single-widget requests are covered.
                pendingWidgets.clear()
                Pass.All(requestedVersion)
            }
            pendingWidgets.isNotEmpty() -> {
                Pass.Widgets(pendingWidgets.toSet()).also { pendingWidgets.clear() }
            }
            else -> {
                workerClaimed = false
                null
            }
        }
    }

    fun complete(pass: Pass) = synchronized(lock) {
        if (pass is Pass.All && pass.version > servedVersion) servedVersion = pass.version
    }

    /** The pass did not finish. Keep its work pending and release the claim so a later request re-enqueues. */
    fun abort(pass: Pass?) = synchronized(lock) {
        if (pass is Pass.Widgets) pendingWidgets.addAll(pass.ids)
        // An aborted Pass.All never advanced servedVersion, so it is still owed.
        workerClaimed = false
    }

    private fun claimWorker(): Boolean {
        if (workerClaimed) return false
        workerClaimed = true
        return true
    }
}
