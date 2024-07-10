SELECT
    date_trunc('month', instance_started)::DATE AS month_started,
    avg(time_finished - instance_started) as avg_runtime,
    count(*) AS total_instances
  FROM usage_stats
  WHERE instance_started BETWEEN TIMESTAMP WITH TIME ZONE '2019-01-01 00:00:00.000-08:00' AND NOW();
  GROUP BY month_started;
