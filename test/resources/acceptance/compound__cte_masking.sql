WITH a AS (SELECT x FROM b),
     b AS (SELECT x from a)
SELECT a.x, b.y FROM a, b
