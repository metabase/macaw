SELECT date_trunc('month', instance_started)::date as month_started, count(*) AS "total_instances",
    avg(ts-instance_started) as avg_runtime,
    avg((stats->'user'->'users'->>'active')::int) as avg_active_users,
    percentile_disc(0.9) within group (order by (stats->'user'->'users'->>'active')::int) as p90_active_users,
    avg((stats->'question'->'questions'->>'total')::int) as avg_questions,
    percentile_disc(0.9) within group (order by (stats->'question'->'questions'->>'total')::int) as p90_questions,
    max((stats->'question'->'questions'->>'total')::int) as max_questions
  FROM "public"."usage_stats"
  WHERE (("public"."usage_stats"."instance_started" BETWEEN timestamp
    with time zone '2019-01-01 00:00:00.000-08:00' AND NOW()) AND ts-instance_started < INTERVAL '30 days')
  GROUP BY 1
  ORDER BY 1 ASC
