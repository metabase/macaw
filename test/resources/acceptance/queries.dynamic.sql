-- FIXTURE: generate-series
-- UNSUPPORTED
SELECT t.day::date AS date
FROM generate_series(timestamp '2021-01-01', now(), interval '1 day') AS t(day)

-- FIXTURE: format
-- UNSUPPORTED
SELECT * FROM format('%I', table_name_variable);

-- FIXTURE: prepared-stmt
EXECUTE stmt('table_name');

-- FIXTURE: variable
-- BROKEN
EXECUTE 'SELECT * FROM ' || table_name;

-- FIXTURE: call-function
CALL user_function('table_name');

-- FIXTURE: select-function
SELECT user_function('table_name');

-- FIXTURE: cursor
-- BROKEN
FETCH ALL FROM my_cursor;
