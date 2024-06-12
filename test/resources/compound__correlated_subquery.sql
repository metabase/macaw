SELECT
    e.department as department_name,
    (SELECT AVG(salary) FROM employees WHERE department = e.department) AS avg_salary,
    (SELECT COUNT(*) FROM employees WHERE department = e.department) AS total_employees,
    (SELECT COUNT(*) FROM employees WHERE department = e.department AND salary > 9000) AS high_earners
FROM employees e;
