WITH
    cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 as (
        SELECT DISTINCT
            CASE
                WHEN table_7249.column_7271 = 'LDMLONG' THEN 'FUND1'
                ELSE NULL
            END AS UH_D6A8948F8B678837,
            LEFT (table_7249.column_7273, 6) AS UH_0CD05BAA62496454,
            MAX(
                LAST_DAY (
                    TO_DATE (table_7249.column_7273, 'YYYYMMDD'),
                    column_7261
                )
            ) OVER (
                PARTITION BY
                    (
                        CASE
                            WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                            ELSE NULL
                        END
                    ),
                    (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_462686D096D3FBC2,
            SUM(
                CASE
                    WHEN table_7249.column_7258 > 0 THEN table_7249.column_7258
                    ELSE 0
                END
            ) OVER (
                PARTITION BY
                    (
                        CASE
                            WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                            ELSE NULL
                        END
                    ),
                    (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_06C4E1E4A8A1FD2A,
            SUM(
                CASE
                    WHEN table_7249.column_7258 < 0 THEN table_7249.column_7258
                    ELSE 0
                END
            ) OVER (
                PARTITION BY
                    (
                        CASE
                            WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                            ELSE NULL
                        END
                    ),
                    (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_A8D9B0C610725FEF,
            SUM(
                (
                    CASE
                        WHEN table_7249.column_7258 > 0 THEN table_7249.column_7258
                        ELSE 0
                    END
                ) - (
                    CASE
                        WHEN table_7249.column_7258 < 0 THEN table_7249.column_7258
                        ELSE 0
                    END
                )
            ) OVER (
                PARTITION BY
                    (
                        CASE
                            WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                            ELSE NULL
                        END
                    ),
                    (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_BDB5D41E23017D7F,
            SUM(
                (
                    (
                        ABS(table_7249.column_7258) / table_7249.column_7253
                    ) / 2
                ) * 100
            ) OVER (
                PARTITION BY
                    (
                        CASE
                            WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                            ELSE NULL
                        END
                    ),
                    (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_B53C543EC59E9A11,
            FIRST_VALUE (table_7249.column_7253) OVER (
                PARTITION BY
                    t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                    LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6)
                ORDER BY
                    t4634.L_1__DATE_ID__0BE2505CFE
            ) AS UH_0615DE85B613C8A2
        FROM
            schema_7247.table_7249 AS t4634
        ORDER BY
            UH_0CD05BAA62496454 ASC
    )
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