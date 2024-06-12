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
