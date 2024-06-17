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
