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
