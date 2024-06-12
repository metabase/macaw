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
