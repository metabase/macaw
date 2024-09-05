WITH c AS (SELECT x FROM b),
     b AS (SELECT y FROM a),
     a AS (SELECT x FROM c)
SELECT a.x, b.y FROM a, b
