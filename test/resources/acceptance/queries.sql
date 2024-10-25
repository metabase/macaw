-- FIXTURE: between/working
SELECT count(*)
FROM t
WHERE CAST(created_at AS date) BETWEEN DATE '2021-08-01' AND DATE '2021-09-30';

-- FIXTURE: bigquery/table-wildcard
SELECT
  *
FROM
  `project_id.dataset_id.table_*`
WHERE
  _TABLE_SUFFIX BETWEEN '20230101' AND '20230131'
LIMIT 1000;

-- FIXTURE: broken/between
SELECT
    date_trunc('month', instance_started)::DATE AS month_started,
    avg(time_finished - instance_started) as avg_runtime,
    count(*) AS total_instances
  FROM usage_stats
  WHERE instance_started BETWEEN TIMESTAMP WITH TIME ZONE '2019-01-01 00:00:00.000-08:00' AND NOW();
  GROUP BY month_started;

-- FIXTURE: broken/filter-where
select
    e.instance_id
    , percentile_cont(0.75) within group (order by e.running_time) as p75_time
    -- the parser trips up on the opening bracket in the `filter (where ...)` clause
    , percentile_cont(0.75) within group (order by e.running_time) filter (where e.error = '') as p75_success_time
  from execution e
  group by 1

-- FIXTURE: compound/correlated-subquery
SELECT
    e.department as department_name,
    (SELECT AVG(salary) FROM employees WHERE department = e.department) AS avg_salary,
    (SELECT COUNT(*) FROM employees WHERE department = e.department) AS total_employees,
    (SELECT COUNT(*) FROM employees WHERE department = e.department AND salary > 9000) AS high_earners
FROM employees e;

-- FIXTURE: compound/cte
WITH department_stats AS (
    SELECT
        department,
        AVG(salary) AS average_salary,
        COUNT(*) AS total_employees
    FROM employees
    GROUP BY department
),
high_earners AS (
    SELECT
        department,
        COUNT(*) AS high_earners_count
    FROM employees
    WHERE salary > 9000
    GROUP BY department
)
SELECT
    ds.department as department_name,
    ds.average_salary as avg_summary,
    ds.total_employees,
    COALESCE(he.high_earners_count, 0) AS high_earners
FROM department_stats ds
LEFT JOIN high_earners he ON ds.department = he.department;

-- FIXTURE: compound/cte-deadscope
WITH cte AS (
  SELECT x FROM t1
)
SELECT x, y FROM t2

-- FIXTURE: compound/cte-masking
WITH c AS (SELECT x FROM b),
     b AS (SELECT y FROM a),
     a AS (SELECT x FROM c)
SELECT a.x, b.y FROM a, b

-- FIXTURE: compound/cte-nonambiguous
WITH cte AS (
  SELECT x FROM t1
)
SELECT x, y FROM t2 JOIN cte

-- FIXTURE: compound/cte-pun
with q as (
  select
    id
  from
    report_card
  where
    created_at > '2024-01-01'
  limit
    1
)
select
  created_at
from
  report_dashboardcard dc
  join q on dc.card_id = q.id;

-- FIXTURE: compound/cte-recursive
WITH RECURSIVE hierarchy AS (
    SELECT id, manager_id, name
    FROM employees
    WHERE manager_id IS NULL
    UNION ALL
    SELECT e.id, e.manager_id, e.name
    FROM employees e
    JOIN hierarchy eh ON e.manager_id = eh.id
)
SELECT * FROM hierarchy;

-- FIXTURE: compound/cte-simple
WITH cte AS (SELECT x FROM t1) SELECT cte.x, y FROM t2 JOIN cte;

-- FIXTURE: compound/subselect
SELECT
    e.department as department_name,
    e.average_salary as avg_salary,
    e.total_employees,
    (SELECT COUNT(*) FROM employees WHERE department = e.department AND salary > 9000) AS high_earners
FROM (
    SELECT
        department,
        AVG(salary) AS average_salary,
        COUNT(*) AS total_employees
    FROM employees
    GROUP BY department
) e;

-- FIXTURE: compound/subselect-table-masking
SELECT
CASE WHEN Addr.country = 'US' THEN Addr.state ELSE 'ex-US' END AS state
FROM (
	SELECT DISTINCT
	    coalesce(Cust.state, Addr.region) AS state,
	    coalesce(Cust.country, Addr.country) AS country
	FROM orders
	    LEFT JOIN addresses AS Addr ON orders.organization_id = Addr.organization_id
	    LEFT JOIN customers AS Cust ON orders.customer_id = Cust.id
) AS Addr

-- FIXTURE: compound/union
SELECT
    department as department_name,
    AVG(salary) AS avg_salary,
    COUNT(*) AS total_employees,
    NULL AS high_earners
FROM employees
GROUP BY department

UNION ALL

SELECT
    department as department_name,
    NULL AS avg_salary,
    NULL AS total_employees,
    COUNT(*) AS high_earners
FROM employees
WHERE salary > 60000
GROUP BY department;

-- FIXTURE: compound/cycle-cte
-- we eventually want to check that the fields are cycled twice in the attribution of the final outputs.
WITH b AS (
    SELECT
       x as y,
       y as z,
       z as x
    FROM a
),
c AS (
    SELECT
       x as y,
       y as z,
       z as x
    FROM b
)
SELECT
    x,
    y,
    z
FROM c;

-- FIXTURE: compound/nested-cte
WITH c AS (
    WITH b AS (
        SELECT
           x as y,
           y as z,
           z as x
        FROM a
    )
    SELECT
       x as y,
       y as z,
       z as x
    FROM b
)
SELECT
    x,
    y,
    z
FROM c;

-- FIXTURE: compound/nested-cte-sneaky
WITH c AS (
    WITH b AS (
        SELECT
           x as y,
           y as z,
           z as x
        FROM a
    )
    SELECT
       x as y,
       y as z,
       z as x
    FROM b
)
SELECT
    x,
    y,
    z
-- NOTE that this is NOT c, we're checking that it knows that the b-alias is out of scope, and this is a real table.
FROM b;

-- FIXTURE: compound/duplicate-scopes
-- This is a minimal example to illustrate why we need to put an id on each scope.
-- As we add more complex compound query tests this will become redundant, and we can then delete it
-- we eventually want to check that the fields are cycled twice in the attribution of the final outputs.
WITH b AS (SELECT x FROM a),
     c AS (SELECT x FROM a)
SELECT
    b.x,
    c.x
FROM b, c;

-- FIXTURE: literal/with-table
SELECT FALSE, 'str', 1, x FROM t

-- FIXTURE: literal/without-table
SELECT FALSE, 'str', 1

-- FIXTURE: no-source-columns
WITH cte AS (SELECT COUNT(*) AS a FROM foo)
SELECT a AS b FROM cte

-- FIXTURE: oracle/open-for
OPEN ccur FOR
    'select c.category
     from ' || TABLE_NAME || ' c
     where c.deptid=' || PI_N_Dept ||
     ' and c.category not in ('|| sExcludeCategories ||')';

-- FIXTURE: reserved/final
with final as (
   select
     id,
     amount_paid_cents::float / 100 as amount_paid
   from invoice
   where not is_deleted
 )

 select * from final

-- FIXTURE: compound/shadow-subselect
SELECT
    e.id,
    e.name,
    d.name AS department_name,
    e.num_employees
FROM (
    SELECT
        id,
        first_name || ' ' || last_name AS name,
        COUNT(*) AS num_employees
    FROM employees
    GROUP BY first_name, last_name, department_id
) e JOIN departments d ON d.id = e.department_id;

-- FIXTURE: mutation/select-into
SELECT id, name
INTO new_user_summary
FROM user;

-- FIXTURE: simple/select-star
SELECT * FROM t;

-- FIXTURE: snowflakelet
SELECT
    column_2564,
    (
        (column_2563 / column_2561) / 2
    ) * 100,
    NVL (
        LEAST (
            column_2562,
            ABS(column_2560)
        ) / column_2561,
        0
    ),
    (
        LEAST (
            SUM(column_2562) OVER (
                ORDER BY
                    column_7299 ROWS BETWEEN 11 PRECEDING
                    AND CURRENT ROW
            ),
            ABS(
                SUM(column_2560) OVER (
                    ORDER BY
                        column_7299 ROWS BETWEEN 11 PRECEDING
                        AND CURRENT ROW
                )
            )
        )
    ) / AVG(column_2561) OVER (
        ORDER BY
            column_7299 ROWS BETWEEN 11 PRECEDING
            AND CURRENT ROW
    )
FROM
    table_2559
ORDER BY
    column_7421 ASC

-- FIXTURE: sqlserver/execute
EXECUTE stmt;

-- FIXTURE: sqlserver/executesql
EXEC sp_executesql @SQL

-- FIXTURE: string-concat
SELECT x || y AS z FROM t

-- FIXTURE: mutation/alter-table
ALTER TABLE users
ADD COLUMN email VARCHAR(255);

-- FIXTURE: mutation/drop-table
DROP TABLE IF EXISTS users;

-- FIXTURE: mutation/truncate-table
TRUNCATE TABLE users;

-- FIXTURE: mutation/insert
INSERT INTO users (name, email)
VALUES ('Alice', 'alice@example.com');

-- FIXTURE: mutation/update
UPDATE users
SET email = 'newemail@example.com'
WHERE name = 'Alice';

-- FIXTURE: mutation/delete
DELETE FROM users
WHERE name = 'Alice';
