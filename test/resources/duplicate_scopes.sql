-- This is a minimal example to illustrate why we need to put an id on each scope.
-- As we add more complex compound query tests this will become redundant, and we can then delete it
-- we eventually want to check that the fields are cycled twice in the attribution of the final outputs.
WITH b AS (SELECT x FROM a),
     c AS (SELECT x FROM a)
SELECT
    b.x,
    c.x
FROM b, c;
