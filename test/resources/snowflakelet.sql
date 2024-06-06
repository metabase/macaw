WITH
    cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 as (
        SELECT DISTINCT
            CASE
                WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                ELSE NULL
            END AS UH_D6A8948F8B678837,
            LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6) AS UH_0CD05BAA62496454,
            MAX(
                LAST_DAY (
                    TO_DATE (t4634.L_1__DATE_ID__0BE2505CFE, 'YYYYMMDD'),
                    MONTH
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
                    WHEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D > 0 THEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D
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
                    WHEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D < 0 THEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D
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
                        WHEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D > 0 THEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D
                        ELSE 0
                    END
                ) - (
                    CASE
                        WHEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D < 0 THEN t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D
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
                        ABS(t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D) / t4634.L_1__DAY_END_NET_A_U_M__B195059E66
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
            FIRST_VALUE (t4634.L_1__DAY_END_NET_A_U_M__B195059E66) OVER (
                PARTITION BY
                    t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                    LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6)
                ORDER BY
                    t4634.L_1__DATE_ID__0BE2505CFE
            ) AS UH_0615DE85B613C8A2
        FROM
            REFDATA.TurnoverData3004 AS t4634
        ORDER BY
            UH_0CD05BAA62496454 ASC
    )
SELECT
    c2300.UH_D6A8948F8B678837 AS UH_8C8B21C2A4D05334,
    c2300.UH_462686D096D3FBC2 AS UH_ECE3615915F146D1,
    c2300.UH_0CD05BAA62496454 AS UH_435E4C4A2012AF1F,
    c2300.UH_06C4E1E4A8A1FD2A AS UH_4E6FB1CAD1F9D4A5,
    c2300.UH_A8D9B0C610725FEF AS UH_88D4F973916A28BB,
    c2300.UH_BDB5D41E23017D7F AS UH_D89DC59DE3440953,
    c2300.UH_0615DE85B613C8A2 AS UH_F3A6E2A6F6F0B0D7,
    (
        (
            c2300.UH_BDB5D41E23017D7F / c2300.UH_0615DE85B613C8A2
        ) / 2
    ) * 100 AS UH_3065C273BCC343E0,
    c2300.UH_B53C543EC59E9A11 AS UH_5FA5BFD8200340AF,
    NVL (
        LEAST (
            c2300.UH_06C4E1E4A8A1FD2A,
            ABS(c2300.UH_A8D9B0C610725FEF)
        ) / c2300.UH_0615DE85B613C8A2,
        0
    ) AS UH_90F8460CAACD4013,
    (
        LEAST (
            SUM(c2300.UH_06C4E1E4A8A1FD2A) OVER (
                ORDER BY
                    c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                    AND CURRENT ROW
            ),
            ABS(
                SUM(c2300.UH_A8D9B0C610725FEF) OVER (
                    ORDER BY
                        c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                        AND CURRENT ROW
                )
            )
        )
    ) / AVG(c2300.UH_0615DE85B613C8A2) OVER (
        ORDER BY
            c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
            AND CURRENT ROW
    ) AS UH_4ACD3F273B71139F
FROM
    cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 AS c2300
ORDER BY
    UH_435E4C4A2012AF1F ASC