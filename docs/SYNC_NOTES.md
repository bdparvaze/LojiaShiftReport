# Sync Notes

Known gaps before enabling `CLOUD_SYNC_ENABLED`:

1. **`lastUpdated` uses the shift date:** Conflict resolution compares timestamps derived from the shift report's `dateInMillis` rather than an independent wall-clock modification timestamp.
2. **No deletion propagation:** Records deleted locally are not tracked as tombstones or removed from remote storage during sync.
3. **No WorkManager retry on soft failure:** Transient or partial sync errors inside `FirebaseCloudSyncWorker` do not trigger a WorkManager `Result.retry()` backoff cycle.
