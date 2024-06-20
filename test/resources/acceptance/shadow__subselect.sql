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
