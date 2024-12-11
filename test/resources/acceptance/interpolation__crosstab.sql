SELECT * FROM crosstab($$

    SELECT
        history.page,
        date_trunc('month', history.h_timestamp)::DATE,
        count(history.id) as total
    FROM history
    WHERE h_timestamp between '2024-01-01' and '2024-12-01'
    GROUP BY page, date_trunc('month', history.h_timestamp)
$$,

        $$
            SELECT
                date_trunc('month', generate_series('2024-01-01', '2024-02-01', '1 month'::INTERVAL))::DATE
$$
) AS ct(
    page INTEGER,
    "Jan" FLOAT,
    "Feb" FLOAT
)
