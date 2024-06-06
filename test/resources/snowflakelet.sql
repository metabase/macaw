SELECT
    table_7248.column_7260 AS UH_8C8B21C2A4D05334,
    table_7248.column_7267 AS UH_ECE3615915F146D1,
    table_7248.column_7250 AS UH_435E4C4A2012AF1F,
    table_7248.column_7252 AS UH_4E6FB1CAD1F9D4A5,
    table_7248.column_7251 AS UH_88D4F973916A28BB,
    table_7248.column_7256 AS UH_D89DC59DE3440953,
    table_7248.column_7257 AS UH_F3A6E2A6F6F0B0D7,
    (
        (table_7248.column_7256 / table_7248.column_7257) / 2
    ) * 100 AS UH_3065C273BCC343E0,
    table_7248.column_7254 AS UH_5FA5BFD8200340AF,
    NVL (
        LEAST (
            table_7248.column_7252,
            ABS(table_7248.column_7251)
        ) / table_7248.column_7257,
        0
    ) AS UH_90F8460CAACD4013,
    (
        LEAST (
            SUM(table_7248.column_7252) OVER (
                ORDER BY
                    c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                    AND CURRENT ROW
            ),
            ABS(
                SUM(table_7248.column_7251) OVER (
                    ORDER BY
                        c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                        AND CURRENT ROW
                )
            )
        )
    ) / AVG(table_7248.column_7257) OVER (
        ORDER BY
            c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
            AND CURRENT ROW
    ) AS UH_4ACD3F273B71139F
FROM
    table_7248 AS c2300
ORDER BY
    UH_435E4C4A2012AF1F ASC