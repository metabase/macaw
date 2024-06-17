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
