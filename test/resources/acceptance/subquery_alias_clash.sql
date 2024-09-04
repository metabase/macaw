SELECT
CASE WHEN Addr.country = 'US' THEN Addr.state ELSE 'ex-US' END AS state
FROM (
	SELECT DISTINCT
	    coalesce(Cust.state, Addr.region) AS state,
	    coalesce(Cust.country, Addr.country) AS country
	FROM orders
	    LEFT JOIN addresses AS Addr ON orders.organization_id = Addr.organization_id
	    LEFT JOIN customers AS Cust ON orders.customer_id = Cust.id
) AS Addr
