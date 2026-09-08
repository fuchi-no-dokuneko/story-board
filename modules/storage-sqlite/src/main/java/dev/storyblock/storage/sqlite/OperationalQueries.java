package dev.storyblock.storage.sqlite;

final class OperationalQueries {
  static final String COUNTS = """
                    SELECT
                      (SELECT COUNT(*) FROM analysis_jobs WHERE status = 'queued'),
                      CAST(COALESCE((julianday(?) - julianday(
                        (SELECT MIN(created_at) FROM analysis_jobs WHERE status = 'queued')
                      )) * 86400, 0) AS INTEGER),
                      CAST(COALESCE((SELECT AVG(
                        (julianday(r.completed_at) - julianday(j.created_at)) * 86400000
                      ) FROM analysis_runs r JOIN analysis_jobs j ON j.job_id = r.job_id), 0)
                        AS INTEGER),
                      (SELECT COUNT(*) FROM rewrite_proposals WHERE state = 'stale'),
                      (SELECT COALESCE(SUM(size_bytes), 0) FROM artifacts)
                    """;
}
