with cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
    SELECT
        TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
        m5636.L_1__SHARE_CLASS__D65B07E846 AS UH_4A953737BEE1C143,
        m5636.L_1__ISSUE_CLASS__B467FBDC38 AS UH_5E59A8E4B632AD08,
        m5636.L_1__ADMINISTRATOR_CODE__98DA43B58A AS UH_ACD23440ED0B3A1C,
        m5636.L_1__N_A_V_PRICE__138937E91E AS UH_5B4AFC1D55960E91,
        m5636.L_1__M_T_D__L_O__4C4D62636F AS UH_3B298B340A3AA73E,
        CASE
            WHEN m5636.L_1__M_T_D__L_O__4C4D62636F = '' THEN 0
            ELSE CAST (
                m5636.L_1__M_T_D__L_O__4C4D62636F AS DECIMAL (38, 12)
            )
        END AS UH_C5591FBB04AAC642,
        CAST (
            m5636.L_1__N_A_V_PRICE__138937E91E AS DECIMAL (38, 12)
        ) AS UH_B1D0046780C85DFA,
        left (
            TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
            7
        ) AS UH_DCE9829EEDFB87EE,
        left (
            TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
            4
        ) AS UH_C30D64DC99F0374D
    FROM
        REFDATA.MockNAVFileClean2 AS m5636
    ORDER BY
        UH_5D80FE84FD58ACF9 ASC
),
cte_a8e7867c_f64f_4584_a1e2_a8a7d202b67d as (
    with cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
        SELECT
            TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
            m5636.L_1__SHARE_CLASS__D65B07E846 AS UH_4A953737BEE1C143,
            m5636.L_1__ISSUE_CLASS__B467FBDC38 AS UH_5E59A8E4B632AD08,
            m5636.L_1__ADMINISTRATOR_CODE__98DA43B58A AS UH_ACD23440ED0B3A1C,
            m5636.L_1__N_A_V_PRICE__138937E91E AS UH_5B4AFC1D55960E91,
            m5636.L_1__M_T_D__L_O__4C4D62636F AS UH_3B298B340A3AA73E,
            CASE
                WHEN m5636.L_1__M_T_D__L_O__4C4D62636F = '' THEN 0
                ELSE CAST (
                    m5636.L_1__M_T_D__L_O__4C4D62636F AS DECIMAL (38, 12)
                )
            END AS UH_C5591FBB04AAC642,
            CAST (
                m5636.L_1__N_A_V_PRICE__138937E91E AS DECIMAL (38, 12)
            ) AS UH_B1D0046780C85DFA,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                7
            ) AS UH_DCE9829EEDFB87EE,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                4
            ) AS UH_C30D64DC99F0374D
        FROM
            REFDATA.MockNAVFileClean2 AS m5636
        ORDER BY
            UH_5D80FE84FD58ACF9 ASC
    )
    SELECT
        c7447.UH_5D80FE84FD58ACF9 AS UH_90E3B790134DD8EC,
        c7447.UH_4A953737BEE1C143 AS UH_7B3DD5E2F3BDFFAE,
        c7447.UH_5E59A8E4B632AD08 AS UH_4395CAC50FEDD42A,
        c7447.UH_ACD23440ED0B3A1C AS UH_7D97C0D42EB6A550,
        c7447.UH_5B4AFC1D55960E91 AS UH_562914F30A07C8A1,
        c7447.UH_3B298B340A3AA73E AS UH_FB087243CF5CF40D,
        c7447.UH_C5591FBB04AAC642 AS UH_778A91686286894F,
        c7447.UH_B1D0046780C85DFA AS UH_1EA45AB23443BF5A,
        case
            when c7447.UH_5D80FE84FD58ACF9 = last_day (c7447.UH_5D80FE84FD58ACF9, month) then 1
            else 0
        end AS UH_2C7819A556F16884,
        left (
            dateadd ('MONTH', 1, c7447.UH_5D80FE84FD58ACF9),
            7
        ) AS UH_381C1BE83905280E
    FROM
        cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 AS c7447
    WHERE
        UH_2C7819A556F16884 ILIKE '1'
    ORDER BY
        UH_90E3B790134DD8EC ASC
),
cte_b5bf0778_f4e1_465f_8bc7_3311ae5d6986 as (
    with cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
        SELECT
            TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
            m5636.L_1__SHARE_CLASS__D65B07E846 AS UH_4A953737BEE1C143,
            m5636.L_1__ISSUE_CLASS__B467FBDC38 AS UH_5E59A8E4B632AD08,
            m5636.L_1__ADMINISTRATOR_CODE__98DA43B58A AS UH_ACD23440ED0B3A1C,
            m5636.L_1__N_A_V_PRICE__138937E91E AS UH_5B4AFC1D55960E91,
            m5636.L_1__M_T_D__L_O__4C4D62636F AS UH_3B298B340A3AA73E,
            CASE
                WHEN m5636.L_1__M_T_D__L_O__4C4D62636F = '' THEN 0
                ELSE CAST (
                    m5636.L_1__M_T_D__L_O__4C4D62636F AS DECIMAL (38, 12)
                )
            END AS UH_C5591FBB04AAC642,
            CAST (
                m5636.L_1__N_A_V_PRICE__138937E91E AS DECIMAL (38, 12)
            ) AS UH_B1D0046780C85DFA,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                7
            ) AS UH_DCE9829EEDFB87EE,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                4
            ) AS UH_C30D64DC99F0374D
        FROM
            REFDATA.MockNAVFileClean2 AS m5636
        ORDER BY
            UH_5D80FE84FD58ACF9 ASC
    )
    SELECT
        c7447.UH_5D80FE84FD58ACF9 AS UH_4E0ACF9B987FBEE3,
        c7447.UH_4A953737BEE1C143 AS UH_61F8CF8BC3911D4F,
        c7447.UH_5E59A8E4B632AD08 AS UH_95D21B5E8DF3FE7A,
        c7447.UH_ACD23440ED0B3A1C AS UH_CAC8C78DC7FA3DB2,
        c7447.UH_5B4AFC1D55960E91 AS UH_ABB88326A4828A22,
        c7447.UH_3B298B340A3AA73E AS UH_4FEE7460995E5F91,
        c7447.UH_C5591FBB04AAC642 AS UH_015FA9333CE61928,
        c7447.UH_B1D0046780C85DFA AS UH_6C6A665A9720BD13,
        case
            when c7447.UH_5D80FE84FD58ACF9 = last_day (c7447.UH_5D80FE84FD58ACF9, year) then 1
            else 0
        end AS UH_F9B3D31E28F3E01B,
        left (
            dateadd ('YEAR', 1, c7447.UH_5D80FE84FD58ACF9),
            4
        ) AS UH_A82129841CC09D28
    FROM
        cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 AS c7447
    WHERE
        UH_F9B3D31E28F3E01B ILIKE '%1%'
    ORDER BY
        UH_4E0ACF9B987FBEE3 ASC
),
cte_9f66c556_1f65_4dc3_8e76_5a440580d83a as (
    with cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 as (
        SELECT
            DISTINCT CASE
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
                PARTITION BY (
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
                PARTITION BY (
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
                PARTITION BY (
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
                PARTITION BY (
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
                        ABS (t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D) / t4634.L_1__DAY_END_NET_A_U_M__B195059E66
                    ) / 2
                ) * 100
            ) OVER (
                PARTITION BY (
                    CASE
                        WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                        ELSE NULL
                    END
                ),
                (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_B53C543EC59E9A11,
            FIRST_VALUE (t4634.L_1__DAY_END_NET_A_U_M__B195059E66) OVER (
                PARTITION BY t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
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
                ABS (c2300.UH_A8D9B0C610725FEF)
            ) / c2300.UH_0615DE85B613C8A2,
            0
        ) AS UH_90F8460CAACD4013,
        (
            LEAST (
                SUM (c2300.UH_06C4E1E4A8A1FD2A) OVER (
                    ORDER BY
                        c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                        AND CURRENT ROW
                ),
                ABS (
                    SUM (c2300.UH_A8D9B0C610725FEF) OVER (
                        ORDER BY
                            c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                            AND CURRENT ROW
                    )
                )
            )
        ) / AVG (c2300.UH_0615DE85B613C8A2) OVER (
            ORDER BY
                c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                AND CURRENT ROW
        ) AS UH_4ACD3F273B71139F
    FROM
        cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 AS c2300
    ORDER BY
        UH_435E4C4A2012AF1F ASC
),
cte_195838c3_9953_4579_94f5_35bc6a28b77f as (
    with cte_30adc85e_aba8_4f67_ae71_b8809fba22bb as (
        with cte_81baaa39_4f00_4c39_a840_f9fdffada5ed as (
            SELECT
                l3411.L_1__ASSET_TYPE_OVERRIDE__9B520A2FED AS UH_DC244AE0330745DE,
                l3411.L_1__ASSET_TYPE_OVERRIDE_SORT__33FADB5C3F AS UH_20EA696BEEE3A12C,
                l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS UH_D33132BF664C12A7,
                l3411.L_1__FX_CURRENCY_CODE__A7916A885E AS UH_8AF0B8BDF6DB49F7,
                l3411.L_1__FX_EXP_CURRENCY_ID__BBDE30151A AS UH_3645E57371D21B66,
                l3411.L_1__IS_TOTAL_ROW__F7258BCE7B AS UH_CF3A719C9185BF4B,
                l3411.L_1__MKT_VALUE_NET_USD__F5C3A8739D AS UH_F36E87222C14AC5B,
                l3411.L_1__ASSET_TYPE_OVERRIDE__9B520A2FED != 'FX' AS UH_B5658A9B439B51DA,
                SUM (l3411.L_1__MKT_VALUE_NET_USD__F5C3A8739D) OVER (
                    PARTITION BY l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                    l3411.L_1__FX_CURRENCY_CODE__A7916A885E
                ) AS UH_59B1562A197A6018
            FROM
                REFDATA.LeverageCash3004 AS l3411
            WHERE
                UH_B5658A9B439B51DA ILIKE 'true'
            ORDER BY
                UH_DC244AE0330745DE ASC
        ),
        a1 as (
            SELECT
                l6607.L_1__MKT_VALUE_NET_USD__F5C3A8739D AS gt_29cbb412_7bbb_454e_aa87_e19755e782fe,
                l6607.L_1__FX_CURRENCY_CODE__A7916A885E AS gt_17d2d30d_0dc4_4415_b658_73d576b98e10,
                l6607.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
            FROM
                REFDATA.LeverageCashFXOnly3004 AS l6607
        ),
        a2 as (
            SELECT
                l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_dc37a044_0478_4e58_a045_7b746d0b3466,
                l3411.L_1__FX_CURRENCY_CODE__A7916A885E AS gt_7fed1589_a818_4ebf_848a_da15187575ac
            FROM
                REFDATA.LeverageCash3004 AS l3411
        ),
        a3 as (
            SELECT
                c5340.UH_59B1562A197A6018 AS UH_59B1562A197A6018,
                c5340.UH_D33132BF664C12A7 AS UH_D33132BF664C12A7,
                c5340.UH_8AF0B8BDF6DB49F7 AS UH_8AF0B8BDF6DB49F7
            FROM
                cte_81baaa39_4f00_4c39_a840_f9fdffada5ed AS c5340
        ),
        p1 as (
            SELECT
                g0621.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                g0621.L_1__DAY_END_NET_A_U_M_USD__4E52B3E42C AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
                g0621.L_1__BOOK_CURRENCY_CODE__006D88E156 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27
            FROM
                REFDATA.GrossExposure0105 AS g0621
        )
        SELECT
            DISTINCT p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c AS UH_7984D9BFB4F58688,
            p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS UH_4C2F680C0C7751CD,
            a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe AS UH_F2E3823313DB74F6,
            a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10 AS UH_5790AB51C3D61ED8,
            a5762.UH_59B1562A197A6018 AS UH_5DA416C9B0A910F3,
            ABS (
                CASE
                    WHEN CONCAT (
                        p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                        p6701.gt_8c158412_3f3a_4002_9059_5d9567cebe27
                    ) = CONCAT (
                        a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f,
                        a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10
                    ) THEN 0
                    ELSE CAST (
                        a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe AS DECIMAL (38, 12)
                    ) / CAST (
                        p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS NUMBER (38)
                    )
                END
            ) AS UH_52D93A543DA41E88,
            CASE
                WHEN a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe > 0
                AND a5762.UH_59B1562A197A6018 < 0 THEN GREATEST (
                    a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe + a5762.UH_59B1562A197A6018,
                    0
                )
                ELSE CASE
                    WHEN a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe < 0
                    AND a5762.UH_59B1562A197A6018 > 0 THEN LEAST (
                        a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe + a5762.UH_59B1562A197A6018,
                        0
                    )
                    ELSE a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe
                END
            END AS UH_1A1E835F0445B04D
        FROM
            p1 AS p6701
            LEFT JOIN a1 AS a5760 ON a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f = p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            LEFT JOIN a2 AS a5761 ON a5761.gt_dc37a044_0478_4e58_a045_7b746d0b3466 = a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
            AND a5761.gt_7fed1589_a818_4ebf_848a_da15187575ac = a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10
            LEFT JOIN a3 AS a5762 ON a5762.UH_D33132BF664C12A7 = a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
            AND a5762.UH_8AF0B8BDF6DB49F7 = a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10
    ),
    cte_8398e7e0_efde_43bb_8816_a6dba1a06e24 as (
        with a1 as (
            SELECT
                b2413.L_1__FUND__F2D69CC9B2 AS gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1,
                b2413.L_1__BOOK__52DAF020E9 AS gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b
            FROM
                REFDATA.BookFundLookup3 AS b2413
        ),
        a2 as (
            SELECT
                g0621.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                g0621.L_1__DAY_END_NET_A_U_M__B195059E66 AS gt_c194d47d_8ad3_4486_ae01_7b450dae7069
            FROM
                REFDATA.GrossExposure0105 AS g0621
        ),
        p1 as (
            SELECT
                s7270.L_1__FUND_CODE__7E7D474910 AS gt_bc2847f1_4228_41bd_99ff_34355072a6fc,
                s7270.L_1__POSITION_CURRENCY_CODE__F0E15771B2 AS gt_60079311_9d9e_475d_a7b0_e38cce009176,
                s7270.L_1__QUANTITY__28F35054A4 AS gt_9a9a4be8_a848_4aff_a60b_79430e678b64
            FROM
                REFDATA.SCFwds0305 AS s7270
        )
        SELECT
            DISTINCT a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1 AS UH_4FD7FFD09F4A91C5,
            SUM(
                ABS (
                    (
                        CASE
                            WHEN p6701.gt_60079311_9d9e_475d_a7b0_e38cce009176 = RIGHT (
                                a5760.gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b,
                                3
                            ) THEN 0
                            ELSE CAST (
                                p6701.gt_9a9a4be8_a848_4aff_a60b_79430e678b64 as DECIMAL (38, 12)
                            )
                        END
                    ) / a5761.gt_c194d47d_8ad3_4486_ae01_7b450dae7069
                ) * 100
            ) OVER (
                PARTITION BY a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1
            ) AS UH_B57C1D76748904B8
        FROM
            p1 AS p6701
            LEFT JOIN a1 AS a5760 ON a5760.gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b = p6701.gt_bc2847f1_4228_41bd_99ff_34355072a6fc
            LEFT JOIN a2 AS a5761 ON a5761.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c = a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1
    ),
    a1 as (
        SELECT
            c6007.UH_7984D9BFB4F58688 AS UH_7984D9BFB4F58688,
            c6007.UH_52D93A543DA41E88 AS UH_52D93A543DA41E88,
            c6007.UH_1A1E835F0445B04D AS UH_1A1E835F0445B04D
        FROM
            cte_30adc85e_aba8_4f67_ae71_b8809fba22bb AS c6007
    ),
    a2 as (
        SELECT
            c7535.UH_B57C1D76748904B8 AS UH_B57C1D76748904B8,
            c7535.UH_4FD7FFD09F4A91C5 AS UH_4FD7FFD09F4A91C5
        FROM
            cte_8398e7e0_efde_43bb_8816_a6dba1a06e24 AS c7535
    ),
    p1 as (
        SELECT
            g0621.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
            g0621.L_1__BOOK_CURRENCY_CODE__006D88E156 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27,
            g0621.L_1__DAY_END_NET_A_U_M_USD__4E52B3E42C AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
            g0621.L_1__DELTA_ADJ_EXP_GROSS_A_U_M_PCT__EDE95732AE AS gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec
        FROM
            REFDATA.GrossExposure0105 AS g0621
    )
    SELECT
        DISTINCT p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c AS UH_34CCFB90CCC6F02D,
        p6701.gt_8c158412_3f3a_4002_9059_5d9567cebe27 AS UH_2996455587BED195,
        p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS UH_ABBF62C1F08513B5,
        SUM (a5760.UH_52D93A543DA41E88 * 100) OVER (
            PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        ) AS UH_ECECB096A13E53A7,
        SUM (
            ABS (
                a5760.UH_1A1E835F0445B04D / CAST (
                    p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS NUMBER
                )
            ) * 100
        ) OVER (
            PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        ) AS UH_C0F31F5050675FDD,
        a5761.UH_B57C1D76748904B8 AS UH_355CB087330094C6,
        (
            p6701.gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec * 100
        ) + SUM (a5760.UH_52D93A543DA41E88 * 100) OVER (
            PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        ) + a5761.UH_B57C1D76748904B8 AS UH_159FD7EBE29E8604,
        (
            p6701.gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec * 100
        ) + GREATEST (
            SUM (
                ABS (
                    a5760.UH_1A1E835F0445B04D / CAST (
                        p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS NUMBER
                    )
                ) * 100
            ) OVER (
                PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ),
            0
        ) + 0 AS UH_F71D209652B52DC5
    FROM
        p1 AS p6701
        LEFT JOIN a1 AS a5760 ON a5760.UH_7984D9BFB4F58688 = p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        INNER JOIN a2 AS a5761 ON a5761.UH_4FD7FFD09F4A91C5 = p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
),
cte_6a6b154f_57f0_429f_9e17_b9dce8fa501b as (
    with cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
        SELECT
            i2070.L_1__CURRENCY__FC9B72EFA3 AS UH_FF0E34C1334A718F,
            TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
            i2070.L_1__LONGNAME__D5260C6405 AS UH_58C81BBAE68C4209,
            CAST (
                i2070.L_1__P_X__L_A_S_T__B8D59A5CDC as DECIMAL (38, 3)
            ) AS UH_0964E7490E69F16D,
            i2070.L_1__TICKER__C5F4C49604 AS UH_F07E6870C5573841,
            left (
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                7
            ) AS UH_3BD6A59C9DBBA13C,
            left (
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                4
            ) AS UH_56C8047C9B06D885,
            CONCAT (
                i2070.L_1__TICKER__C5F4C49604,
                ' ',
                i2070.L_1__CURRENCY__FC9B72EFA3
            ) AS UH_B28070E7B7E22086,
            FIRST_VALUE (i2070.L_1__P_X__L_A_S_T__B8D59A5CDC) OVER (
                PARTITION BY i2070.L_1__TICKER__C5F4C49604
                ORDER BY
                    i2070.L_1__DATE__830D4106E6
            ) AS UH_50E3C1F21076A04D
        FROM
            REFDATA.IndexPricesFeed2 AS i2070
        ORDER BY
            UH_FF0E34C1334A718F ASC
    ),
    cte_c4c724f9_8c43_4447_844c_2a112abf3f23 as (
        with cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
            SELECT
                i2070.L_1__CURRENCY__FC9B72EFA3 AS UH_FF0E34C1334A718F,
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                i2070.L_1__LONGNAME__D5260C6405 AS UH_58C81BBAE68C4209,
                CAST (
                    i2070.L_1__P_X__L_A_S_T__B8D59A5CDC as DECIMAL (38, 3)
                ) AS UH_0964E7490E69F16D,
                i2070.L_1__TICKER__C5F4C49604 AS UH_F07E6870C5573841,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    7
                ) AS UH_3BD6A59C9DBBA13C,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    4
                ) AS UH_56C8047C9B06D885,
                CONCAT (
                    i2070.L_1__TICKER__C5F4C49604,
                    ' ',
                    i2070.L_1__CURRENCY__FC9B72EFA3
                ) AS UH_B28070E7B7E22086,
                FIRST_VALUE (i2070.L_1__P_X__L_A_S_T__B8D59A5CDC) OVER (
                    PARTITION BY i2070.L_1__TICKER__C5F4C49604
                    ORDER BY
                        i2070.L_1__DATE__830D4106E6
                ) AS UH_50E3C1F21076A04D
            FROM
                REFDATA.IndexPricesFeed2 AS i2070
            ORDER BY
                UH_FF0E34C1334A718F ASC
        )
        SELECT
            c0316.UH_65CE5957F5592694 AS UH_1909EAC2D231DD6B,
            c0316.UH_0964E7490E69F16D AS UH_92F772F39AD988ED,
            c0316.UH_F07E6870C5573841 AS UH_514FC83227D4EDF2,
            case
                when c0316.UH_65CE5957F5592694 = last_day (c0316.UH_65CE5957F5592694, year) then 1
                else 0
            end AS UH_0101D066957E052A,
            left (
                dateadd ('YEAR', 1, c0316.UH_65CE5957F5592694),
                4
            ) AS UH_BC19CABF07D4C656
        FROM
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a AS c0316
        WHERE
            UH_0101D066957E052A ILIKE '1'
        ORDER BY
            UH_1909EAC2D231DD6B ASC
    ),
    cte_da66352d_6ce2_49ce_8226_5a3bbb41c2a3 as (
        with cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
            SELECT
                i2070.L_1__CURRENCY__FC9B72EFA3 AS UH_FF0E34C1334A718F,
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                i2070.L_1__LONGNAME__D5260C6405 AS UH_58C81BBAE68C4209,
                CAST (
                    i2070.L_1__P_X__L_A_S_T__B8D59A5CDC as DECIMAL (38, 3)
                ) AS UH_0964E7490E69F16D,
                i2070.L_1__TICKER__C5F4C49604 AS UH_F07E6870C5573841,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    7
                ) AS UH_3BD6A59C9DBBA13C,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    4
                ) AS UH_56C8047C9B06D885,
                CONCAT (
                    i2070.L_1__TICKER__C5F4C49604,
                    ' ',
                    i2070.L_1__CURRENCY__FC9B72EFA3
                ) AS UH_B28070E7B7E22086,
                FIRST_VALUE (i2070.L_1__P_X__L_A_S_T__B8D59A5CDC) OVER (
                    PARTITION BY i2070.L_1__TICKER__C5F4C49604
                    ORDER BY
                        i2070.L_1__DATE__830D4106E6
                ) AS UH_50E3C1F21076A04D
            FROM
                REFDATA.IndexPricesFeed2 AS i2070
            ORDER BY
                UH_FF0E34C1334A718F ASC
        )
        SELECT
            c0316.UH_65CE5957F5592694 AS UH_EE0C82CB92CDCF2E,
            c0316.UH_0964E7490E69F16D AS UH_6A8801D276D99093,
            c0316.UH_F07E6870C5573841 AS UH_40DCE4CBD24F0A7C,
            case
                when c0316.UH_65CE5957F5592694 = last_day (c0316.UH_65CE5957F5592694, month) then 1
                else 0
            end AS UH_96368C264D40B643,
            left (
                dateadd ('MONTH', 1, c0316.UH_65CE5957F5592694),
                7
            ) AS UH_F7DF7E630AFA1D81
        FROM
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a AS c0316
        WHERE
            UH_96368C264D40B643 ILIKE '1'
        ORDER BY
            UH_EE0C82CB92CDCF2E ASC
    ),
    a1 as (
        SELECT
            c4651.UH_92F772F39AD988ED AS UH_92F772F39AD988ED,
            c4651.UH_BC19CABF07D4C656 AS UH_BC19CABF07D4C656
        FROM
            cte_c4c724f9_8c43_4447_844c_2a112abf3f23 AS c4651
    ),
    a2 as (
        SELECT
            c1344.UH_F7DF7E630AFA1D81 AS UH_F7DF7E630AFA1D81,
            c1344.UH_6A8801D276D99093 AS UH_6A8801D276D99093
        FROM
            cte_da66352d_6ce2_49ce_8226_5a3bbb41c2a3 AS c1344
    ),
    p1 as (
        SELECT
            c0316.UH_65CE5957F5592694 AS UH_65CE5957F5592694,
            c0316.UH_B28070E7B7E22086 AS UH_B28070E7B7E22086,
            c0316.UH_F07E6870C5573841 AS UH_F07E6870C5573841,
            c0316.UH_0964E7490E69F16D AS UH_0964E7490E69F16D,
            c0316.UH_56C8047C9B06D885 AS UH_56C8047C9B06D885,
            c0316.UH_3BD6A59C9DBBA13C AS UH_3BD6A59C9DBBA13C
        FROM
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a AS c0316
    )
    SELECT
        p6701.UH_65CE5957F5592694 AS UH_687D1D46F8A5B651,
        p6701.UH_B28070E7B7E22086 AS UH_28F4A35747EA0274,
        p6701.UH_F07E6870C5573841 AS UH_AA1FCF59717B80AB,
        p6701.UH_0964E7490E69F16D AS UH_7FE40783F09C637F,
        (
            (
                p6701.UH_0964E7490E69F16D / a5761.UH_6A8801D276D99093
            ) - 1
        ) * 100 AS UH_4050277F68110E5E,
        a5760.UH_92F772F39AD988ED AS UH_5CEE650DC0BAF835,
        (
            (
                p6701.UH_0964E7490E69F16D / a5760.UH_92F772F39AD988ED
            ) - 1
        ) * 100 AS UH_7AC21AAF3B07A1AB
    FROM
        p1 AS p6701
        LEFT JOIN a1 AS a5760 ON a5760.UH_BC19CABF07D4C656 = p6701.UH_56C8047C9B06D885
        LEFT JOIN a2 AS a5761 ON a5761.UH_F7DF7E630AFA1D81 = p6701.UH_3BD6A59C9DBBA13C
    ORDER BY
        UH_687D1D46F8A5B651 ASC
),
cte_d4667322_38d8_47f6_93a6_6cabea506e8e as (
    SELECT
        m0243.L_1__DATE__830D4106E6 AS UH_439A8F94F02832BA,
        m0243.L_1__ADMIN__CODE__C138FEEC0C AS UH_8C49F48C4CCB8698,
        m0243.L_1__EXP__GROSS____A_U_M__8B195FC6DC AS UH_61BF1508FDC779BB,
        m0243.L_1__EXP__LONG____A_U_M__EBEADABF3D AS UH_B3492BAE64560E42,
        m0243.L_1__EXP__NET____A_U_M__2B273CBA53 AS UH_D08F1496FC258AB1,
        m0243.L_1__EXP__SHORT____A_U_M__CED3896E78 AS UH_5A79454169A76BFF,
        m0243.L_1__M_T_D__CONTRIBUTION__D23849EC1D AS UH_99FDB2C7CFC4AC5C,
        m0243.L_1__M_T_D__R_O_I_C__FCDD9BAB84 AS UH_0D21AED63876730E,
        m0243.L_1__PERIOD__CONTRIBUTION__9FFABDAA7A AS UH_40861D00A51BA340,
        m0243.L_1__PERIOD__R_O_I_C__87BDAA00E1 AS UH_F8BEE82FA285C0C1,
        m0243.L_1__Q_T_D__CONTRIBUTION__0CC98A8722 AS UH_C8036DC63CE84EC1,
        m0243.L_1__Q_T_D__R_O_I_C__57DEA2D6AA AS UH_EB79B2A898A3F169,
        m0243.L_1__SIXTY_D__VOLATILITY__0CF2BDCEBA AS UH_0A4378EE90118B47,
        m0243.L_1__TWENTY_D__VOLATILITY__E7D443DF58 AS UH_7692DD7CB1AB5580,
        m0243.L_1__Y_T_D__CONTRIBUTION__8C09AE88AA AS UH_4C0A00613DF168D6,
        m0243.L_1__Y_T_D__R_O_I_C__DD661C50B5 AS UH_0A2A153BAAF5BAE3,
        TO_DATE (m0243.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_3DA72BEEDBE8750F
    FROM
        REFDATA.MIK_Paste AS m0243
    ORDER BY
        UH_439A8F94F02832BA ASC
),
cte_a8e7867c_f64f_4584_a1e2_a8a7d202b67d as (
    with cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
        SELECT
            TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
            m5636.L_1__SHARE_CLASS__D65B07E846 AS UH_4A953737BEE1C143,
            m5636.L_1__ISSUE_CLASS__B467FBDC38 AS UH_5E59A8E4B632AD08,
            m5636.L_1__ADMINISTRATOR_CODE__98DA43B58A AS UH_ACD23440ED0B3A1C,
            m5636.L_1__N_A_V_PRICE__138937E91E AS UH_5B4AFC1D55960E91,
            m5636.L_1__M_T_D__L_O__4C4D62636F AS UH_3B298B340A3AA73E,
            CASE
                WHEN m5636.L_1__M_T_D__L_O__4C4D62636F = '' THEN 0
                ELSE CAST (
                    m5636.L_1__M_T_D__L_O__4C4D62636F AS DECIMAL (38, 12)
                )
            END AS UH_C5591FBB04AAC642,
            CAST (
                m5636.L_1__N_A_V_PRICE__138937E91E AS DECIMAL (38, 12)
            ) AS UH_B1D0046780C85DFA,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                7
            ) AS UH_DCE9829EEDFB87EE,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                4
            ) AS UH_C30D64DC99F0374D
        FROM
            REFDATA.MockNAVFileClean2 AS m5636
        ORDER BY
            UH_5D80FE84FD58ACF9 ASC
    )
    SELECT
        c7447.UH_5D80FE84FD58ACF9 AS UH_90E3B790134DD8EC,
        c7447.UH_4A953737BEE1C143 AS UH_7B3DD5E2F3BDFFAE,
        c7447.UH_5E59A8E4B632AD08 AS UH_4395CAC50FEDD42A,
        c7447.UH_ACD23440ED0B3A1C AS UH_7D97C0D42EB6A550,
        c7447.UH_5B4AFC1D55960E91 AS UH_562914F30A07C8A1,
        c7447.UH_3B298B340A3AA73E AS UH_FB087243CF5CF40D,
        c7447.UH_C5591FBB04AAC642 AS UH_778A91686286894F,
        c7447.UH_B1D0046780C85DFA AS UH_1EA45AB23443BF5A,
        case
            when c7447.UH_5D80FE84FD58ACF9 = last_day (c7447.UH_5D80FE84FD58ACF9, month) then 1
            else 0
        end AS UH_2C7819A556F16884,
        left (
            dateadd ('MONTH', 1, c7447.UH_5D80FE84FD58ACF9),
            7
        ) AS UH_381C1BE83905280E
    FROM
        cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 AS c7447
    WHERE
        UH_2C7819A556F16884 ILIKE '1'
    ORDER BY
        UH_90E3B790134DD8EC ASC
),
cte_b5bf0778_f4e1_465f_8bc7_3311ae5d6986 as (
    with cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
        SELECT
            TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
            m5636.L_1__SHARE_CLASS__D65B07E846 AS UH_4A953737BEE1C143,
            m5636.L_1__ISSUE_CLASS__B467FBDC38 AS UH_5E59A8E4B632AD08,
            m5636.L_1__ADMINISTRATOR_CODE__98DA43B58A AS UH_ACD23440ED0B3A1C,
            m5636.L_1__N_A_V_PRICE__138937E91E AS UH_5B4AFC1D55960E91,
            m5636.L_1__M_T_D__L_O__4C4D62636F AS UH_3B298B340A3AA73E,
            CASE
                WHEN m5636.L_1__M_T_D__L_O__4C4D62636F = '' THEN 0
                ELSE CAST (
                    m5636.L_1__M_T_D__L_O__4C4D62636F AS DECIMAL (38, 12)
                )
            END AS UH_C5591FBB04AAC642,
            CAST (
                m5636.L_1__N_A_V_PRICE__138937E91E AS DECIMAL (38, 12)
            ) AS UH_B1D0046780C85DFA,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                7
            ) AS UH_DCE9829EEDFB87EE,
            left (
                TO_DATE (m5636.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                4
            ) AS UH_C30D64DC99F0374D
        FROM
            REFDATA.MockNAVFileClean2 AS m5636
        ORDER BY
            UH_5D80FE84FD58ACF9 ASC
    )
    SELECT
        c7447.UH_5D80FE84FD58ACF9 AS UH_4E0ACF9B987FBEE3,
        c7447.UH_4A953737BEE1C143 AS UH_61F8CF8BC3911D4F,
        c7447.UH_5E59A8E4B632AD08 AS UH_95D21B5E8DF3FE7A,
        c7447.UH_ACD23440ED0B3A1C AS UH_CAC8C78DC7FA3DB2,
        c7447.UH_5B4AFC1D55960E91 AS UH_ABB88326A4828A22,
        c7447.UH_3B298B340A3AA73E AS UH_4FEE7460995E5F91,
        c7447.UH_C5591FBB04AAC642 AS UH_015FA9333CE61928,
        c7447.UH_B1D0046780C85DFA AS UH_6C6A665A9720BD13,
        case
            when c7447.UH_5D80FE84FD58ACF9 = last_day (c7447.UH_5D80FE84FD58ACF9, year) then 1
            else 0
        end AS UH_F9B3D31E28F3E01B,
        left (
            dateadd ('YEAR', 1, c7447.UH_5D80FE84FD58ACF9),
            4
        ) AS UH_A82129841CC09D28
    FROM
        cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 AS c7447
    WHERE
        UH_F9B3D31E28F3E01B ILIKE '%1%'
    ORDER BY
        UH_4E0ACF9B987FBEE3 ASC
),
cte_9f66c556_1f65_4dc3_8e76_5a440580d83a as (
    with cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 as (
        SELECT
            DISTINCT CASE
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
                PARTITION BY (
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
                PARTITION BY (
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
                PARTITION BY (
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
                PARTITION BY (
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
                        ABS (t4634.L_1__GROSS_MONEY_NET_BASE__565305D36D) / t4634.L_1__DAY_END_NET_A_U_M__B195059E66
                    ) / 2
                ) * 100
            ) OVER (
                PARTITION BY (
                    CASE
                        WHEN t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3 = 'LDMLONG' THEN 'FUND1'
                        ELSE NULL
                    END
                ),
                (LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6))
            ) AS UH_B53C543EC59E9A11,
            FIRST_VALUE (t4634.L_1__DAY_END_NET_A_U_M__B195059E66) OVER (
                PARTITION BY t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
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
                ABS (c2300.UH_A8D9B0C610725FEF)
            ) / c2300.UH_0615DE85B613C8A2,
            0
        ) AS UH_90F8460CAACD4013,
        (
            LEAST (
                SUM (c2300.UH_06C4E1E4A8A1FD2A) OVER (
                    ORDER BY
                        c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                        AND CURRENT ROW
                ),
                ABS (
                    SUM (c2300.UH_A8D9B0C610725FEF) OVER (
                        ORDER BY
                            c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                            AND CURRENT ROW
                    )
                )
            )
        ) / AVG (c2300.UH_0615DE85B613C8A2) OVER (
            ORDER BY
                c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                AND CURRENT ROW
        ) AS UH_4ACD3F273B71139F
    FROM
        cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 AS c2300
    ORDER BY
        UH_435E4C4A2012AF1F ASC
),
cte_195838c3_9953_4579_94f5_35bc6a28b77f as (
    with cte_30adc85e_aba8_4f67_ae71_b8809fba22bb as (
        with cte_81baaa39_4f00_4c39_a840_f9fdffada5ed as (
            SELECT
                l3411.L_1__ASSET_TYPE_OVERRIDE__9B520A2FED AS UH_DC244AE0330745DE,
                l3411.L_1__ASSET_TYPE_OVERRIDE_SORT__33FADB5C3F AS UH_20EA696BEEE3A12C,
                l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS UH_D33132BF664C12A7,
                l3411.L_1__FX_CURRENCY_CODE__A7916A885E AS UH_8AF0B8BDF6DB49F7,
                l3411.L_1__FX_EXP_CURRENCY_ID__BBDE30151A AS UH_3645E57371D21B66,
                l3411.L_1__IS_TOTAL_ROW__F7258BCE7B AS UH_CF3A719C9185BF4B,
                l3411.L_1__MKT_VALUE_NET_USD__F5C3A8739D AS UH_F36E87222C14AC5B,
                l3411.L_1__ASSET_TYPE_OVERRIDE__9B520A2FED != 'FX' AS UH_B5658A9B439B51DA,
                SUM (l3411.L_1__MKT_VALUE_NET_USD__F5C3A8739D) OVER (
                    PARTITION BY l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                    l3411.L_1__FX_CURRENCY_CODE__A7916A885E
                ) AS UH_59B1562A197A6018
            FROM
                REFDATA.LeverageCash3004 AS l3411
            WHERE
                UH_B5658A9B439B51DA ILIKE 'true'
            ORDER BY
                UH_DC244AE0330745DE ASC
        ),
        a1 as (
            SELECT
                l6607.L_1__MKT_VALUE_NET_USD__F5C3A8739D AS gt_29cbb412_7bbb_454e_aa87_e19755e782fe,
                l6607.L_1__FX_CURRENCY_CODE__A7916A885E AS gt_17d2d30d_0dc4_4415_b658_73d576b98e10,
                l6607.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
            FROM
                REFDATA.LeverageCashFXOnly3004 AS l6607
        ),
        a2 as (
            SELECT
                l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_dc37a044_0478_4e58_a045_7b746d0b3466,
                l3411.L_1__FX_CURRENCY_CODE__A7916A885E AS gt_7fed1589_a818_4ebf_848a_da15187575ac
            FROM
                REFDATA.LeverageCash3004 AS l3411
        ),
        a3 as (
            SELECT
                c5340.UH_59B1562A197A6018 AS UH_59B1562A197A6018,
                c5340.UH_D33132BF664C12A7 AS UH_D33132BF664C12A7,
                c5340.UH_8AF0B8BDF6DB49F7 AS UH_8AF0B8BDF6DB49F7
            FROM
                cte_81baaa39_4f00_4c39_a840_f9fdffada5ed AS c5340
        ),
        p1 as (
            SELECT
                g0621.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                g0621.L_1__DAY_END_NET_A_U_M_USD__4E52B3E42C AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
                g0621.L_1__BOOK_CURRENCY_CODE__006D88E156 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27
            FROM
                REFDATA.GrossExposure0105 AS g0621
        )
        SELECT
            DISTINCT p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c AS UH_7984D9BFB4F58688,
            p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS UH_4C2F680C0C7751CD,
            a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe AS UH_F2E3823313DB74F6,
            a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10 AS UH_5790AB51C3D61ED8,
            a5762.UH_59B1562A197A6018 AS UH_5DA416C9B0A910F3,
            ABS (
                CASE
                    WHEN CONCAT (
                        p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                        p6701.gt_8c158412_3f3a_4002_9059_5d9567cebe27
                    ) = CONCAT (
                        a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f,
                        a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10
                    ) THEN 0
                    ELSE CAST (
                        a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe AS DECIMAL (38, 12)
                    ) / CAST (
                        p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS NUMBER (38)
                    )
                END
            ) AS UH_52D93A543DA41E88,
            CASE
                WHEN a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe > 0
                AND a5762.UH_59B1562A197A6018 < 0 THEN GREATEST (
                    a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe + a5762.UH_59B1562A197A6018,
                    0
                )
                ELSE CASE
                    WHEN a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe < 0
                    AND a5762.UH_59B1562A197A6018 > 0 THEN LEAST (
                        a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe + a5762.UH_59B1562A197A6018,
                        0
                    )
                    ELSE a5760.gt_29cbb412_7bbb_454e_aa87_e19755e782fe
                END
            END AS UH_1A1E835F0445B04D
        FROM
            p1 AS p6701
            LEFT JOIN a1 AS a5760 ON a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f = p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            LEFT JOIN a2 AS a5761 ON a5761.gt_dc37a044_0478_4e58_a045_7b746d0b3466 = a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
            AND a5761.gt_7fed1589_a818_4ebf_848a_da15187575ac = a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10
            LEFT JOIN a3 AS a5762 ON a5762.UH_D33132BF664C12A7 = a5760.gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
            AND a5762.UH_8AF0B8BDF6DB49F7 = a5760.gt_17d2d30d_0dc4_4415_b658_73d576b98e10
    ),
    cte_8398e7e0_efde_43bb_8816_a6dba1a06e24 as (
        with a1 as (
            SELECT
                b2413.L_1__FUND__F2D69CC9B2 AS gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1,
                b2413.L_1__BOOK__52DAF020E9 AS gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b
            FROM
                REFDATA.BookFundLookup3 AS b2413
        ),
        a2 as (
            SELECT
                g0621.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                g0621.L_1__DAY_END_NET_A_U_M__B195059E66 AS gt_c194d47d_8ad3_4486_ae01_7b450dae7069
            FROM
                REFDATA.GrossExposure0105 AS g0621
        ),
        p1 as (
            SELECT
                s7270.L_1__FUND_CODE__7E7D474910 AS gt_bc2847f1_4228_41bd_99ff_34355072a6fc,
                s7270.L_1__POSITION_CURRENCY_CODE__F0E15771B2 AS gt_60079311_9d9e_475d_a7b0_e38cce009176,
                s7270.L_1__QUANTITY__28F35054A4 AS gt_9a9a4be8_a848_4aff_a60b_79430e678b64
            FROM
                REFDATA.SCFwds0305 AS s7270
        )
        SELECT
            DISTINCT a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1 AS UH_4FD7FFD09F4A91C5,
            SUM(
                ABS (
                    (
                        CASE
                            WHEN p6701.gt_60079311_9d9e_475d_a7b0_e38cce009176 = RIGHT (
                                a5760.gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b,
                                3
                            ) THEN 0
                            ELSE CAST (
                                p6701.gt_9a9a4be8_a848_4aff_a60b_79430e678b64 as DECIMAL (38, 12)
                            )
                        END
                    ) / a5761.gt_c194d47d_8ad3_4486_ae01_7b450dae7069
                ) * 100
            ) OVER (
                PARTITION BY a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1
            ) AS UH_B57C1D76748904B8
        FROM
            p1 AS p6701
            LEFT JOIN a1 AS a5760 ON a5760.gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b = p6701.gt_bc2847f1_4228_41bd_99ff_34355072a6fc
            LEFT JOIN a2 AS a5761 ON a5761.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c = a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1
    ),
    a1 as (
        SELECT
            c6007.UH_7984D9BFB4F58688 AS UH_7984D9BFB4F58688,
            c6007.UH_52D93A543DA41E88 AS UH_52D93A543DA41E88,
            c6007.UH_1A1E835F0445B04D AS UH_1A1E835F0445B04D
        FROM
            cte_30adc85e_aba8_4f67_ae71_b8809fba22bb AS c6007
    ),
    a2 as (
        SELECT
            c7535.UH_B57C1D76748904B8 AS UH_B57C1D76748904B8,
            c7535.UH_4FD7FFD09F4A91C5 AS UH_4FD7FFD09F4A91C5
        FROM
            cte_8398e7e0_efde_43bb_8816_a6dba1a06e24 AS c7535
    ),
    p1 as (
        SELECT
            g0621.L_1__FUND_GROUP_CODE__A9BAC3C5A3 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
            g0621.L_1__BOOK_CURRENCY_CODE__006D88E156 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27,
            g0621.L_1__DAY_END_NET_A_U_M_USD__4E52B3E42C AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
            g0621.L_1__DELTA_ADJ_EXP_GROSS_A_U_M_PCT__EDE95732AE AS gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec
        FROM
            REFDATA.GrossExposure0105 AS g0621
    )
    SELECT
        DISTINCT p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c AS UH_34CCFB90CCC6F02D,
        p6701.gt_8c158412_3f3a_4002_9059_5d9567cebe27 AS UH_2996455587BED195,
        p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS UH_ABBF62C1F08513B5,
        SUM (a5760.UH_52D93A543DA41E88 * 100) OVER (
            PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        ) AS UH_ECECB096A13E53A7,
        SUM (
            ABS (
                a5760.UH_1A1E835F0445B04D / CAST (
                    p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS NUMBER
                )
            ) * 100
        ) OVER (
            PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        ) AS UH_C0F31F5050675FDD,
        a5761.UH_B57C1D76748904B8 AS UH_355CB087330094C6,
        (
            p6701.gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec * 100
        ) + SUM (a5760.UH_52D93A543DA41E88 * 100) OVER (
            PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        ) + a5761.UH_B57C1D76748904B8 AS UH_159FD7EBE29E8604,
        (
            p6701.gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec * 100
        ) + GREATEST (
            SUM (
                ABS (
                    a5760.UH_1A1E835F0445B04D / CAST (
                        p6701.gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71 AS NUMBER
                    )
                ) * 100
            ) OVER (
                PARTITION BY p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ),
            0
        ) + 0 AS UH_F71D209652B52DC5
    FROM
        p1 AS p6701
        LEFT JOIN a1 AS a5760 ON a5760.UH_7984D9BFB4F58688 = p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
        INNER JOIN a2 AS a5761 ON a5761.UH_4FD7FFD09F4A91C5 = p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
),
cte_6a6b154f_57f0_429f_9e17_b9dce8fa501b as (
    with cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
        SELECT
            i2070.L_1__CURRENCY__FC9B72EFA3 AS UH_FF0E34C1334A718F,
            TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
            i2070.L_1__LONGNAME__D5260C6405 AS UH_58C81BBAE68C4209,
            CAST (
                i2070.L_1__P_X__L_A_S_T__B8D59A5CDC as DECIMAL (38, 3)
            ) AS UH_0964E7490E69F16D,
            i2070.L_1__TICKER__C5F4C49604 AS UH_F07E6870C5573841,
            left (
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                7
            ) AS UH_3BD6A59C9DBBA13C,
            left (
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                4
            ) AS UH_56C8047C9B06D885,
            CONCAT (
                i2070.L_1__TICKER__C5F4C49604,
                ' ',
                i2070.L_1__CURRENCY__FC9B72EFA3
            ) AS UH_B28070E7B7E22086,
            FIRST_VALUE (i2070.L_1__P_X__L_A_S_T__B8D59A5CDC) OVER (
                PARTITION BY i2070.L_1__TICKER__C5F4C49604
                ORDER BY
                    i2070.L_1__DATE__830D4106E6
            ) AS UH_50E3C1F21076A04D
        FROM
            REFDATA.IndexPricesFeed2 AS i2070
        ORDER BY
            UH_FF0E34C1334A718F ASC
    ),
    cte_c4c724f9_8c43_4447_844c_2a112abf3f23 as (
        with cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
            SELECT
                i2070.L_1__CURRENCY__FC9B72EFA3 AS UH_FF0E34C1334A718F,
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                i2070.L_1__LONGNAME__D5260C6405 AS UH_58C81BBAE68C4209,
                CAST (
                    i2070.L_1__P_X__L_A_S_T__B8D59A5CDC as DECIMAL (38, 3)
                ) AS UH_0964E7490E69F16D,
                i2070.L_1__TICKER__C5F4C49604 AS UH_F07E6870C5573841,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    7
                ) AS UH_3BD6A59C9DBBA13C,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    4
                ) AS UH_56C8047C9B06D885,
                CONCAT (
                    i2070.L_1__TICKER__C5F4C49604,
                    ' ',
                    i2070.L_1__CURRENCY__FC9B72EFA3
                ) AS UH_B28070E7B7E22086,
                FIRST_VALUE (i2070.L_1__P_X__L_A_S_T__B8D59A5CDC) OVER (
                    PARTITION BY i2070.L_1__TICKER__C5F4C49604
                    ORDER BY
                        i2070.L_1__DATE__830D4106E6
                ) AS UH_50E3C1F21076A04D
            FROM
                REFDATA.IndexPricesFeed2 AS i2070
            ORDER BY
                UH_FF0E34C1334A718F ASC
        )
        SELECT
            c0316.UH_65CE5957F5592694 AS UH_1909EAC2D231DD6B,
            c0316.UH_0964E7490E69F16D AS UH_92F772F39AD988ED,
            c0316.UH_F07E6870C5573841 AS UH_514FC83227D4EDF2,
            case
                when c0316.UH_65CE5957F5592694 = last_day (c0316.UH_65CE5957F5592694, year) then 1
                else 0
            end AS UH_0101D066957E052A,
            left (
                dateadd ('YEAR', 1, c0316.UH_65CE5957F5592694),
                4
            ) AS UH_BC19CABF07D4C656
        FROM
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a AS c0316
        WHERE
            UH_0101D066957E052A ILIKE '1'
        ORDER BY
            UH_1909EAC2D231DD6B ASC
    ),
    cte_da66352d_6ce2_49ce_8226_5a3bbb41c2a3 as (
        with cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
            SELECT
                i2070.L_1__CURRENCY__FC9B72EFA3 AS UH_FF0E34C1334A718F,
                TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                i2070.L_1__LONGNAME__D5260C6405 AS UH_58C81BBAE68C4209,
                CAST (
                    i2070.L_1__P_X__L_A_S_T__B8D59A5CDC as DECIMAL (38, 3)
                ) AS UH_0964E7490E69F16D,
                i2070.L_1__TICKER__C5F4C49604 AS UH_F07E6870C5573841,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    7
                ) AS UH_3BD6A59C9DBBA13C,
                left (
                    TO_DATE (i2070.L_1__DATE__830D4106E6, 'DD/MM/YYYY'),
                    4
                ) AS UH_56C8047C9B06D885,
                CONCAT (
                    i2070.L_1__TICKER__C5F4C49604,
                    ' ',
                    i2070.L_1__CURRENCY__FC9B72EFA3
                ) AS UH_B28070E7B7E22086,
                FIRST_VALUE (i2070.L_1__P_X__L_A_S_T__B8D59A5CDC) OVER (
                    PARTITION BY i2070.L_1__TICKER__C5F4C49604
                    ORDER BY
                        i2070.L_1__DATE__830D4106E6
                ) AS UH_50E3C1F21076A04D
            FROM
                REFDATA.IndexPricesFeed2 AS i2070
            ORDER BY
                UH_FF0E34C1334A718F ASC
        )
        SELECT
            c0316.UH_65CE5957F5592694 AS UH_EE0C82CB92CDCF2E,
            c0316.UH_0964E7490E69F16D AS UH_6A8801D276D99093,
            c0316.UH_F07E6870C5573841 AS UH_40DCE4CBD24F0A7C,
            case
                when c0316.UH_65CE5957F5592694 = last_day (c0316.UH_65CE5957F5592694, month) then 1
                else 0
            end AS UH_96368C264D40B643,
            left (
                dateadd ('MONTH', 1, c0316.UH_65CE5957F5592694),
                7
            ) AS UH_F7DF7E630AFA1D81
        FROM
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a AS c0316
        WHERE
            UH_96368C264D40B643 ILIKE '1'
        ORDER BY
            UH_EE0C82CB92CDCF2E ASC
    ),
    a1 as (
        SELECT
            c4651.UH_92F772F39AD988ED AS UH_92F772F39AD988ED,
            c4651.UH_BC19CABF07D4C656 AS UH_BC19CABF07D4C656
        FROM
            cte_c4c724f9_8c43_4447_844c_2a112abf3f23 AS c4651
    ),
    a2 as (
        SELECT
            c1344.UH_F7DF7E630AFA1D81 AS UH_F7DF7E630AFA1D81,
            c1344.UH_6A8801D276D99093 AS UH_6A8801D276D99093
        FROM
            cte_da66352d_6ce2_49ce_8226_5a3bbb41c2a3 AS c1344
    ),
    p1 as (
        SELECT
            c0316.UH_65CE5957F5592694 AS UH_65CE5957F5592694,
            c0316.UH_B28070E7B7E22086 AS UH_B28070E7B7E22086,
            c0316.UH_F07E6870C5573841 AS UH_F07E6870C5573841,
            c0316.UH_0964E7490E69F16D AS UH_0964E7490E69F16D,
            c0316.UH_56C8047C9B06D885 AS UH_56C8047C9B06D885,
            c0316.UH_3BD6A59C9DBBA13C AS UH_3BD6A59C9DBBA13C
        FROM
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a AS c0316
    )
    SELECT
        p6701.UH_65CE5957F5592694 AS UH_687D1D46F8A5B651,
        p6701.UH_B28070E7B7E22086 AS UH_28F4A35747EA0274,
        p6701.UH_F07E6870C5573841 AS UH_AA1FCF59717B80AB,
        p6701.UH_0964E7490E69F16D AS UH_7FE40783F09C637F,
        (
            (
                p6701.UH_0964E7490E69F16D / a5761.UH_6A8801D276D99093
            ) - 1
        ) * 100 AS UH_4050277F68110E5E,
        a5760.UH_92F772F39AD988ED AS UH_5CEE650DC0BAF835,
        (
            (
                p6701.UH_0964E7490E69F16D / a5760.UH_92F772F39AD988ED
            ) - 1
        ) * 100 AS UH_7AC21AAF3B07A1AB
    FROM
        p1 AS p6701
        LEFT JOIN a1 AS a5760 ON a5760.UH_BC19CABF07D4C656 = p6701.UH_56C8047C9B06D885
        LEFT JOIN a2 AS a5761 ON a5761.UH_F7DF7E630AFA1D81 = p6701.UH_3BD6A59C9DBBA13C
    ORDER BY
        UH_687D1D46F8A5B651 ASC
),
cte_d4667322_38d8_47f6_93a6_6cabea506e8e as (
    SELECT
        m0243.L_1__DATE__830D4106E6 AS UH_439A8F94F02832BA,
        m0243.L_1__ADMIN__CODE__C138FEEC0C AS UH_8C49F48C4CCB8698,
        m0243.L_1__EXP__GROSS____A_U_M__8B195FC6DC AS UH_61BF1508FDC779BB,
        m0243.L_1__EXP__LONG____A_U_M__EBEADABF3D AS UH_B3492BAE64560E42,
        m0243.L_1__EXP__NET____A_U_M__2B273CBA53 AS UH_D08F1496FC258AB1,
        m0243.L_1__EXP__SHORT____A_U_M__CED3896E78 AS UH_5A79454169A76BFF,
        m0243.L_1__M_T_D__CONTRIBUTION__D23849EC1D AS UH_99FDB2C7CFC4AC5C,
        m0243.L_1__M_T_D__R_O_I_C__FCDD9BAB84 AS UH_0D21AED63876730E,
        m0243.L_1__PERIOD__CONTRIBUTION__9FFABDAA7A AS UH_40861D00A51BA340,
        m0243.L_1__PERIOD__R_O_I_C__87BDAA00E1 AS UH_F8BEE82FA285C0C1,
        m0243.L_1__Q_T_D__CONTRIBUTION__0CC98A8722 AS UH_C8036DC63CE84EC1,
        m0243.L_1__Q_T_D__R_O_I_C__57DEA2D6AA AS UH_EB79B2A898A3F169,
        m0243.L_1__SIXTY_D__VOLATILITY__0CF2BDCEBA AS UH_0A4378EE90118B47,
        m0243.L_1__TWENTY_D__VOLATILITY__E7D443DF58 AS UH_7692DD7CB1AB5580,
        m0243.L_1__Y_T_D__CONTRIBUTION__8C09AE88AA AS UH_4C0A00613DF168D6,
        m0243.L_1__Y_T_D__R_O_I_C__DD661C50B5 AS UH_0A2A153BAAF5BAE3,
        TO_DATE (m0243.L_1__DATE__830D4106E6, 'DD/MM/YYYY') AS UH_3DA72BEEDBE8750F
    FROM
        REFDATA.MIK_Paste AS m0243
    ORDER BY
        UH_439A8F94F02832BA ASC
),
a1 as (
    SELECT
        c3632.UH_1EA45AB23443BF5A AS UH_1EA45AB23443BF5A,
        c3632.UH_381C1BE83905280E AS UH_381C1BE83905280E,
        c3632.UH_90E3B790134DD8EC AS UH_90E3B790134DD8EC
    FROM
        cte_a8e7867c_f64f_4584_a1e2_a8a7d202b67d AS c3632
),
a2 as (
    SELECT
        c0771.UH_6C6A665A9720BD13 AS UH_6C6A665A9720BD13,
        c0771.UH_A82129841CC09D28 AS UH_A82129841CC09D28
    FROM
        cte_b5bf0778_f4e1_465f_8bc7_3311ae5d6986 AS c0771
),
a3 as (
    SELECT
        s0665.L_1__D_W_FUND_GROUP_CODE__A1600E6CA4 AS gt_30e2ab2b_eb34_44e6_ab5f_b38c561e4c40,
        s0665.L_1__SHARE_CLASS_NAME__1B4DA6B9C2 AS gt_676e3f9f_7754_4d65_a746_9086ac757f9b,
        s0665.L_1__ADMIN_SHARE_CLASS_CODES__EFD6386210 AS gt_b2c3ef95_e41e_44d3_b62d_335353b9a758,
        s0665.L_1__ADMIN_CODE_FOR_BENCHMARK__8A17984FE8 AS gt_ecfc8339_adf3_4df3_a33d_387a059573c1
    FROM
        REFDATA.ShareClassMapping AS s0665
),
a4 as (
    SELECT
        c1526.UH_90F8460CAACD4013 AS UH_90F8460CAACD4013,
        c1526.UH_8C8B21C2A4D05334 AS UH_8C8B21C2A4D05334,
        c1526.UH_ECE3615915F146D1 AS UH_ECE3615915F146D1
    FROM
        cte_9f66c556_1f65_4dc3_8e76_5a440580d83a AS c1526
),
a5 as (
    SELECT
        c6736.UH_159FD7EBE29E8604 AS UH_159FD7EBE29E8604,
        c6736.UH_F71D209652B52DC5 AS UH_F71D209652B52DC5,
        c6736.UH_34CCFB90CCC6F02D AS UH_34CCFB90CCC6F02D
    FROM
        cte_195838c3_9953_4579_94f5_35bc6a28b77f AS c6736
),
a6 as (
    SELECT
        c0225.UH_28F4A35747EA0274 AS UH_28F4A35747EA0274,
        c0225.UH_7FE40783F09C637F AS UH_7FE40783F09C637F,
        c0225.UH_4050277F68110E5E AS UH_4050277F68110E5E,
        c0225.UH_7AC21AAF3B07A1AB AS UH_7AC21AAF3B07A1AB,
        c0225.UH_687D1D46F8A5B651 AS UH_687D1D46F8A5B651
    FROM
        cte_6a6b154f_57f0_429f_9e17_b9dce8fa501b AS c0225
),
a7 as (
    SELECT
        c1743.UH_0D21AED63876730E AS UH_0D21AED63876730E,
        c1743.UH_8C49F48C4CCB8698 AS UH_8C49F48C4CCB8698,
        c1743.UH_3DA72BEEDBE8750F AS UH_3DA72BEEDBE8750F
    FROM
        cte_d4667322_38d8_47f6_93a6_6cabea506e8e AS c1743
),
p1 as (
    SELECT
        c7447.UH_5D80FE84FD58ACF9 AS UH_5D80FE84FD58ACF9,
        c7447.UH_B1D0046780C85DFA AS UH_B1D0046780C85DFA,
        c7447.UH_DCE9829EEDFB87EE AS UH_DCE9829EEDFB87EE,
        c7447.UH_C30D64DC99F0374D AS UH_C30D64DC99F0374D,
        c7447.UH_ACD23440ED0B3A1C AS UH_ACD23440ED0B3A1C
    FROM
        cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 AS c7447
)
SELECT
    p6701.UH_5D80FE84FD58ACF9 AS UH_893755EB52A1E609,
    a5762.gt_30e2ab2b_eb34_44e6_ab5f_b38c561e4c40 AS UH_3CFC32F7423065E7,
    a5762.gt_676e3f9f_7754_4d65_a746_9086ac757f9b AS UH_18374F59B3703054,
    a5762.gt_b2c3ef95_e41e_44d3_b62d_335353b9a758 AS UH_787C3497B41AB746,
    year (p6701.UH_5D80FE84FD58ACF9) AS UH_FED4353AF3C9754B,
    p6701.UH_B1D0046780C85DFA AS UH_DC4634F03C3103A3,
    a5760.UH_1EA45AB23443BF5A AS UH_CA923BDFCB7A9286,
    a5761.UH_6C6A665A9720BD13 AS UH_C0432D2D3BE5226A,
    (
        (
            p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
        ) - 1
    ) * 100 AS UH_50337B98AD8E493C,
    POWER (
        LEAST (
            (
                (
                    (
                        p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
                    ) - 1
                )
            ) - (POWER (1.1, 1 / 12) - 1),
            0
        ),
        2
    ) AS UH_07FF96528E8A5ECB,
    p6701.UH_B1D0046780C85DFA / MAX (p6701.UH_B1D0046780C85DFA) OVER (
        PARTITION BY p6701.UH_ACD23440ED0B3A1C
        ORDER BY
            p6701.UH_5D80FE84FD58ACF9 ROWS BETWEEN UNBOUNDED PRECEDING
            AND CURRENT ROW
    ) - 1 AS UH_62227B828A7194C2,
    (
        (
            p6701.UH_B1D0046780C85DFA / a5761.UH_6C6A665A9720BD13
        ) - 1
    ) * 100 AS UH_60BEF4ED0F4CA718,
    a5765.UH_28F4A35747EA0274 AS UH_6B68542F0EC9B9BF,
    a5765.UH_7FE40783F09C637F AS UH_BDF22E0940EA9F42,
    a5765.UH_4050277F68110E5E AS UH_FC01B6F984025CFF,
    POWER (
        LEAST (
            (a5765.UH_4050277F68110E5E / 100) - (POWER (1.1, 1 / 12) - 1),
            0
        ),
        2
    ) AS UH_47F35F809703222A,
    a5765.UH_7FE40783F09C637F / MAX (a5765.UH_7FE40783F09C637F) OVER (
        PARTITION BY p6701.UH_ACD23440ED0B3A1C
        ORDER BY
            p6701.UH_5D80FE84FD58ACF9 ROWS BETWEEN UNBOUNDED PRECEDING
            AND CURRENT ROW
    ) - 1 AS UH_4702036A801925F0,
    a5765.UH_7AC21AAF3B07A1AB AS UH_14E4561D98B7ACAC,
    a5763.UH_90F8460CAACD4013 AS UH_043D884C47A898BB,
    a5764.UH_159FD7EBE29E8604 AS UH_1BFDC847EB80AC0C,
    a5764.UH_F71D209652B52DC5 AS UH_ACECAE7C8E263166,
    (
        (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100
    ) - a5765.UH_4050277F68110E5E AS UH_E5710D7D44FD0EB0,
    (
        (
            1 + (
                (
                    (
                        p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
                    ) - 1
                )
            )
        ) / (1 + (a5765.UH_4050277F68110E5E / 100)) - 1
    ) * 100 AS UH_081C46831EB9808D,
    a5766.UH_0D21AED63876730E AS UH_DE5FAAB85196DCAD,
    (
        CASE
            WHEN a5766.UH_0D21AED63876730E = '' THEN 0
            ELSE CAST (
                a5766.UH_0D21AED63876730E AS DECIMAL (38, 12)
            )
        END * 100
    ) - a5765.UH_4050277F68110E5E AS UH_28DAA6683053AB8B,
    CASE
        WHEN (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100 > 0 THEN (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100
        ELSE NULL
    END AS UH_C48E81316ECF09FD,
    CASE
        WHEN (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100 < 0 THEN (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100
        ELSE NULL
    END AS UH_E66467DE4E3C3D94,
    CASE
        WHEN (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100 > 0 THEN a5765.UH_4050277F68110E5E
        ELSE NULL
    END AS UH_A085FE48B96BDD7E,
    CASE
        WHEN (
            (
                p6701.UH_B1D0046780C85DFA / a5760.UH_1EA45AB23443BF5A
            ) - 1
        ) * 100 < 0 THEN a5765.UH_4050277F68110E5E
        ELSE NULL
    END AS UH_0FFC953F8E95E49F
FROM
    p1 AS p6701
    LEFT JOIN a1 AS a5760 ON a5760.UH_381C1BE83905280E = p6701.UH_DCE9829EEDFB87EE
    LEFT JOIN a2 AS a5761 ON a5761.UH_A82129841CC09D28 = p6701.UH_C30D64DC99F0374D
    INNER JOIN a3 AS a5762 ON a5762.gt_b2c3ef95_e41e_44d3_b62d_335353b9a758 = p6701.UH_ACD23440ED0B3A1C
    INNER JOIN a4 AS a5763 ON a5763.UH_8C8B21C2A4D05334 = a5762.gt_30e2ab2b_eb34_44e6_ab5f_b38c561e4c40
    AND a5763.UH_ECE3615915F146D1 = a5760.UH_90E3B790134DD8EC
    INNER JOIN a5 AS a5764 ON a5764.UH_34CCFB90CCC6F02D = a5762.gt_30e2ab2b_eb34_44e6_ab5f_b38c561e4c40
    LEFT JOIN a6 AS a5765 ON a5765.UH_28F4A35747EA0274 = a5762.gt_ecfc8339_adf3_4df3_a33d_387a059573c1
    AND a5765.UH_687D1D46F8A5B651 = p6701.UH_5D80FE84FD58ACF9
    LEFT JOIN a7 AS a5766 ON a5766.UH_8C49F48C4CCB8698 = a5762.gt_b2c3ef95_e41e_44d3_b62d_335353b9a758
    AND a5766.UH_3DA72BEEDBE8750F = p6701.UH_5D80FE84FD58ACF9
WHERE
    case
        when p6701.UH_5D80FE84FD58ACF9 = last_day (p6701.UH_5D80FE84FD58ACF9, month) then 1
        else 0
    end ILIKE '%1%'
ORDER BY
    UH_893755EB52A1E609 ASC