SELECT t.day::date AS date
FROM generate_series(timestamp '2021-01-01', now(), interval '1 day') AS t(day)
