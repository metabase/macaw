SELECT
    column_2564,
    (
        (column_2563 / column_2561) / 2
    ) * 100,
    NVL (
        LEAST (
            column_2562,
            ABS(column_2560)
        ) / column_2561,
        0
    ),
    (
        LEAST (
            SUM(column_2562) OVER (
                ORDER BY
                    column_7299 ROWS BETWEEN 11 PRECEDING
                    AND CURRENT ROW
            ),
            ABS(
                SUM(column_2560) OVER (
                    ORDER BY
                        column_7299 ROWS BETWEEN 11 PRECEDING
                        AND CURRENT ROW
                )
            )
        )
    ) / AVG(column_2561) OVER (
        ORDER BY
            column_7299 ROWS BETWEEN 11 PRECEDING
            AND CURRENT ROW
    )
FROM
    table_2559
ORDER BY
    column_7421 ASC