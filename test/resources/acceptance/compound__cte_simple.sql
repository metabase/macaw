WITH cte AS (SELECT x FROM t1) SELECT cte.x, y FROM t2 JOIN cte;
