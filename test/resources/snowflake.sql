with
    cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
        SELECT
            TO_DATE (table_2558.column_2604, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
            table_2558.column_2783 AS UH_4A953737BEE1C143,
            table_2558.column_2825 AS UH_5E59A8E4B632AD08,
            table_2558.column_2600 AS UH_ACD23440ED0B3A1C,
            table_2558.column_2665 AS UH_5B4AFC1D55960E91,
            table_2558.column_2766 AS UH_3B298B340A3AA73E,
            CASE
                WHEN table_2558.column_2766 = '' THEN 0
                ELSE CAST(table_2558.column_2766 AS DECIMAL(38, 12))
            END AS UH_C5591FBB04AAC642,
            CAST(table_2558.column_2665 AS DECIMAL(38, 12)) AS UH_B1D0046780C85DFA,
            left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 7) AS UH_DCE9829EEDFB87EE,
            left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 4) AS UH_C30D64DC99F0374D
        FROM
            schema_2542.table_2558 AS m5636
        ORDER BY
            UH_5D80FE84FD58ACF9 ASC
    ),
    cte_a8e7867c_f64f_4584_a1e2_a8a7d202b67d as (
        with
            cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
                SELECT
                    TO_DATE (table_2558.column_2604, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
                    table_2558.column_2783 AS UH_4A953737BEE1C143,
                    table_2558.column_2825 AS UH_5E59A8E4B632AD08,
                    table_2558.column_2600 AS UH_ACD23440ED0B3A1C,
                    table_2558.column_2665 AS UH_5B4AFC1D55960E91,
                    table_2558.column_2766 AS UH_3B298B340A3AA73E,
                    CASE
                        WHEN table_2558.column_2766 = '' THEN 0
                        ELSE CAST(table_2558.column_2766 AS DECIMAL(38, 12))
                    END AS UH_C5591FBB04AAC642,
                    CAST(table_2558.column_2665 AS DECIMAL(38, 12)) AS UH_B1D0046780C85DFA,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 7) AS UH_DCE9829EEDFB87EE,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 4) AS UH_C30D64DC99F0374D
                FROM
                    schema_2542.table_2558 AS m5636
                ORDER BY
                    UH_5D80FE84FD58ACF9 ASC
            )
        SELECT
            table_2552.column_2618 AS UH_90E3B790134DD8EC,
            table_2552.column_2695 AS UH_7B3DD5E2F3BDFFAE,
            table_2552.column_2575 AS UH_4395CAC50FEDD42A,
            table_2552.column_2641 AS UH_7D97C0D42EB6A550,
            table_2552.column_2648 AS UH_562914F30A07C8A1,
            table_2552.column_2583 AS UH_FB087243CF5CF40D,
            table_2552.column_2590 AS UH_778A91686286894F,
            table_2552.column_2611 AS UH_1EA45AB23443BF5A,
            case
                when table_2552.column_2618 = last_day (table_2552.column_2618, column_2607) then 1
                else 0
            end AS UH_2C7819A556F16884,
            left (dateadd ('MONTH', 1, table_2552.column_2618), 7) AS UH_381C1BE83905280E
        FROM
            table_2552 AS c7447
        WHERE
            column_2666 ILIKE '1'
        ORDER BY
            UH_90E3B790134DD8EC ASC
    ),
    cte_b5bf0778_f4e1_465f_8bc7_3311ae5d6986 as (
        with
            cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
                SELECT
                    TO_DATE (table_2558.column_2604, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
                    table_2558.column_2783 AS UH_4A953737BEE1C143,
                    table_2558.column_2825 AS UH_5E59A8E4B632AD08,
                    table_2558.column_2600 AS UH_ACD23440ED0B3A1C,
                    table_2558.column_2665 AS UH_5B4AFC1D55960E91,
                    table_2558.column_2766 AS UH_3B298B340A3AA73E,
                    CASE
                        WHEN table_2558.column_2766 = '' THEN 0
                        ELSE CAST(table_2558.column_2766 AS DECIMAL(38, 12))
                    END AS UH_C5591FBB04AAC642,
                    CAST(table_2558.column_2665 AS DECIMAL(38, 12)) AS UH_B1D0046780C85DFA,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 7) AS UH_DCE9829EEDFB87EE,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 4) AS UH_C30D64DC99F0374D
                FROM
                    schema_2542.table_2558 AS m5636
                ORDER BY
                    UH_5D80FE84FD58ACF9 ASC
            )
        SELECT
            table_2552.column_2618 AS UH_4E0ACF9B987FBEE3,
            table_2552.column_2695 AS UH_61F8CF8BC3911D4F,
            table_2552.column_2575 AS UH_95D21B5E8DF3FE7A,
            table_2552.column_2641 AS UH_CAC8C78DC7FA3DB2,
            table_2552.column_2648 AS UH_ABB88326A4828A22,
            table_2552.column_2583 AS UH_4FEE7460995E5F91,
            table_2552.column_2590 AS UH_015FA9333CE61928,
            table_2552.column_2611 AS UH_6C6A665A9720BD13,
            case
                when table_2552.column_2618 = last_day (table_2552.column_2618, column_2790) then 1
                else 0
            end AS UH_F9B3D31E28F3E01B,
            left (dateadd ('YEAR', 1, table_2552.column_2618), 4) AS UH_A82129841CC09D28
        FROM
            table_2552 AS c7447
        WHERE
            column_2768 ILIKE '%1%'
        ORDER BY
            UH_4E0ACF9B987FBEE3 ASC
    ),
    cte_9f66c556_1f65_4dc3_8e76_5a440580d83a as (
        with
            cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 as (
                SELECT DISTINCT
                    CASE
                        WHEN table_2553.column_2795 = 'LDMLONG' THEN 'FUND1'
                        ELSE NULL
                    END AS UH_D6A8948F8B678837,
                    LEFT (table_2553.column_2609, 6) AS UH_0CD05BAA62496454,
                    MAX(
                        LAST_DAY (
                            TO_DATE (table_2553.column_2609, 'YYYYMMDD'),
                            column_2846
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
                            WHEN table_2553.column_2701 > 0 THEN table_2553.column_2701
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
                            WHEN table_2553.column_2701 < 0 THEN table_2553.column_2701
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
                                WHEN table_2553.column_2701 > 0 THEN table_2553.column_2701
                                ELSE 0
                            END
                        ) - (
                            CASE
                                WHEN table_2553.column_2701 < 0 THEN table_2553.column_2701
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
                                ABS(table_2553.column_2701) / table_2553.column_2647
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
                    FIRST_VALUE (table_2553.column_2647) OVER (
                        PARTITION BY
                            t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                            LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6)
                        ORDER BY
                            t4634.L_1__DATE_ID__0BE2505CFE
                    ) AS UH_0615DE85B613C8A2
                FROM
                    schema_2542.table_2553 AS t4634
                ORDER BY
                    UH_0CD05BAA62496454 ASC
            )
        SELECT
            table_2572.column_2664 AS UH_8C8B21C2A4D05334,
            table_2572.column_2723 AS UH_ECE3615915F146D1,
            table_2572.column_2682 AS UH_435E4C4A2012AF1F,
            table_2572.column_2603 AS UH_4E6FB1CAD1F9D4A5,
            table_2572.column_2700 AS UH_88D4F973916A28BB,
            table_2572.column_2630 AS UH_D89DC59DE3440953,
            table_2572.column_2642 AS UH_F3A6E2A6F6F0B0D7,
            (
                (table_2572.column_2630 / table_2572.column_2642) / 2
            ) * 100 AS UH_3065C273BCC343E0,
            table_2572.column_2627 AS UH_5FA5BFD8200340AF,
            NVL (
                LEAST (
                    table_2572.column_2603,
                    ABS(table_2572.column_2700)
                ) / table_2572.column_2642,
                0
            ) AS UH_90F8460CAACD4013,
            (
                LEAST (
                    SUM(table_2572.column_2603) OVER (
                        ORDER BY
                            c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                            AND CURRENT ROW
                    ),
                    ABS(
                        SUM(table_2572.column_2700) OVER (
                            ORDER BY
                                c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                                AND CURRENT ROW
                        )
                    )
                )
            ) / AVG(table_2572.column_2642) OVER (
                ORDER BY
                    c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                    AND CURRENT ROW
            ) AS UH_4ACD3F273B71139F
        FROM
            table_2572 AS c2300
        ORDER BY
            UH_435E4C4A2012AF1F ASC
    ),
    cte_195838c3_9953_4579_94f5_35bc6a28b77f as (
        with
            cte_30adc85e_aba8_4f67_ae71_b8809fba22bb as (
                with
                    cte_81baaa39_4f00_4c39_a840_f9fdffada5ed as (
                        SELECT
                            table_2556.column_2750 AS UH_DC244AE0330745DE,
                            table_2556.column_2781 AS UH_20EA696BEEE3A12C,
                            table_2556.column_2669 AS UH_D33132BF664C12A7,
                            table_2556.column_2771 AS UH_8AF0B8BDF6DB49F7,
                            table_2556.column_2678 AS UH_3645E57371D21B66,
                            table_2556.column_2787 AS UH_CF3A719C9185BF4B,
                            table_2556.column_2693 AS UH_F36E87222C14AC5B,
                            table_2556.column_2750 != 'FX' AS UH_B5658A9B439B51DA,
                            SUM(table_2556.column_2693) OVER (
                                PARTITION BY
                                    l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                                    l3411.L_1__FX_CURRENCY_CODE__A7916A885E
                            ) AS UH_59B1562A197A6018
                        FROM
                            schema_2542.table_2556 AS l3411
                        WHERE
                            column_2758 ILIKE 'true'
                        ORDER BY
                            UH_DC244AE0330745DE ASC
                    ),
                    a1 as (
                        SELECT
                            table_2574.column_2774 AS gt_29cbb412_7bbb_454e_aa87_e19755e782fe,
                            table_2574.column_2785 AS gt_17d2d30d_0dc4_4415_b658_73d576b98e10,
                            table_2574.column_2743 AS gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
                        FROM
                            schema_2542.table_2574 AS l6607
                    ),
                    a2 as (
                        SELECT
                            table_2556.column_2669 AS gt_dc37a044_0478_4e58_a045_7b746d0b3466,
                            table_2556.column_2771 AS gt_7fed1589_a818_4ebf_848a_da15187575ac
                        FROM
                            schema_2542.table_2556 AS l3411
                    ),
                    a3 as (
                        SELECT
                            table_2549.column_2756 AS UH_59B1562A197A6018,
                            table_2549.column_2605 AS UH_D33132BF664C12A7,
                            table_2549.column_2711 AS UH_8AF0B8BDF6DB49F7
                        FROM
                            table_2549 AS c5340
                    ),
                    p1 as (
                        SELECT
                            table_2565.column_2687 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                            table_2565.column_2852 AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
                            table_2565.column_2631 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27
                        FROM
                            schema_2542.table_2565 AS g0621
                    )
                SELECT DISTINCT
                    table_2547.column_2706 AS UH_7984D9BFB4F58688,
                    table_2547.column_2592 AS UH_4C2F680C0C7751CD,
                    table_2544.column_2677 AS UH_F2E3823313DB74F6,
                    table_2544.column_2670 AS UH_5790AB51C3D61ED8,
                    table_2569.column_2675 AS UH_5DA416C9B0A910F3,
                    ABS(
                        CASE
                            WHEN CONCAT (table_2547.column_2706, table_2547.column_2594) = CONCAT (table_2544.column_2775, table_2544.column_2670) THEN 0
                            ELSE CAST(table_2544.column_2677 AS DECIMAL(38, 12)) / CAST(table_2547.column_2592 AS NUMBER (38))
                        END
                    ) AS UH_52D93A543DA41E88,
                    CASE
                        WHEN table_2544.column_2677 > 0
                        AND table_2569.column_2675 < 0 THEN GREATEST (
                            table_2544.column_2677 + table_2569.column_2675,
                            0
                        )
                        ELSE CASE
                            WHEN table_2544.column_2677 < 0
                            AND table_2569.column_2675 > 0 THEN LEAST (
                                table_2544.column_2677 + table_2569.column_2675,
                                0
                            )
                            ELSE table_2544.column_2677
                        END
                    END AS UH_1A1E835F0445B04D
                FROM
                    table_2547 AS p6701
                    LEFT JOIN table_2544 AS a5760 ON table_2544.column_2775 = table_2547.column_2706
                    LEFT JOIN table_2560 AS a5761 ON table_2560.column_2613 = table_2544.column_2775
                    AND table_2560.column_2640 = table_2544.column_2670
                    LEFT JOIN table_2569 AS a5762 ON table_2569.column_2814 = table_2544.column_2775
                    AND table_2569.column_2727 = table_2544.column_2670
            ),
            cte_8398e7e0_efde_43bb_8816_a6dba1a06e24 as (
                with
                    a1 as (
                        SELECT
                            table_2557.column_2735 AS gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1,
                            table_2557.column_2820 AS gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b
                        FROM
                            schema_2542.table_2557 AS b2413
                    ),
                    a2 as (
                        SELECT
                            table_2565.column_2687 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                            table_2565.column_2654 AS gt_c194d47d_8ad3_4486_ae01_7b450dae7069
                        FROM
                            schema_2542.table_2565 AS g0621
                    ),
                    p1 as (
                        SELECT
                            table_2543.column_2623 AS gt_bc2847f1_4228_41bd_99ff_34355072a6fc,
                            table_2543.column_2680 AS gt_60079311_9d9e_475d_a7b0_e38cce009176,
                            table_2543.column_2629 AS gt_9a9a4be8_a848_4aff_a60b_79430e678b64
                        FROM
                            schema_2542.table_2543 AS s7270
                    )
                SELECT DISTINCT
                    table_2544.column_2837 AS UH_4FD7FFD09F4A91C5,
                    SUM(
                        ABS(
                            (
                                CASE
                                    WHEN table_2547.column_2839 = RIGHT (table_2544.column_2760, 3) THEN 0
                                    ELSE CAST(table_2547.column_2614 as DECIMAL(38, 12))
                                END
                            ) / table_2560.column_2753
                        ) * 100
                    ) OVER (
                        PARTITION BY
                            a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1
                    ) AS UH_B57C1D76748904B8
                FROM
                    table_2547 AS p6701
                    LEFT JOIN table_2544 AS a5760 ON table_2544.column_2760 = table_2547.column_2827
                    LEFT JOIN table_2560 AS a5761 ON table_2560.column_2737 = table_2544.column_2837
            ),
            a1 as (
                SELECT
                    table_2561.column_2742 AS UH_7984D9BFB4F58688,
                    table_2561.column_2685 AS UH_52D93A543DA41E88,
                    table_2561.column_2732 AS UH_1A1E835F0445B04D
                FROM
                    table_2561 AS c6007
            ),
            a2 as (
                SELECT
                    table_2570.column_2821 AS UH_B57C1D76748904B8,
                    table_2570.column_2635 AS UH_4FD7FFD09F4A91C5
                FROM
                    table_2570 AS c7535
            ),
            p1 as (
                SELECT
                    table_2565.column_2687 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                    table_2565.column_2631 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27,
                    table_2565.column_2852 AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
                    table_2565.column_2738 AS gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec
                FROM
                    schema_2542.table_2565 AS g0621
            )
        SELECT DISTINCT
            table_2547.column_2706 AS UH_34CCFB90CCC6F02D,
            table_2547.column_2594 AS UH_2996455587BED195,
            table_2547.column_2592 AS UH_ABBF62C1F08513B5,
            SUM(table_2544.column_2584 * 100) OVER (
                PARTITION BY
                    p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ) AS UH_ECECB096A13E53A7,
            SUM(
                ABS(
                    table_2544.column_2811 / CAST(table_2547.column_2592 AS NUMBER)
                ) * 100
            ) OVER (
                PARTITION BY
                    p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ) AS UH_C0F31F5050675FDD,
            table_2560.column_2710 AS UH_355CB087330094C6,
            (table_2547.column_2702 * 100) + SUM(table_2544.column_2584 * 100) OVER (
                PARTITION BY
                    p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ) + table_2560.column_2710 AS UH_159FD7EBE29E8604,
            (table_2547.column_2702 * 100) + GREATEST (
                SUM(
                    ABS(
                        table_2544.column_2811 / CAST(table_2547.column_2592 AS NUMBER)
                    ) * 100
                ) OVER (
                    PARTITION BY
                        p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
                ),
                0
            ) + 0 AS UH_F71D209652B52DC5
        FROM
            table_2547 AS p6701
            LEFT JOIN table_2544 AS a5760 ON table_2544.column_2581 = table_2547.column_2706
            INNER JOIN table_2560 AS a5761 ON table_2560.column_2762 = table_2547.column_2706
    ),
    cte_6a6b154f_57f0_429f_9e17_b9dce8fa501b as (
        with
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
                SELECT
                    table_2564.column_2770 AS UH_FF0E34C1334A718F,
                    TO_DATE (table_2564.column_2644, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                    table_2564.column_2591 AS UH_58C81BBAE68C4209,
                    CAST(table_2564.column_2767 as DECIMAL(38, 3)) AS UH_0964E7490E69F16D,
                    table_2564.column_2704 AS UH_F07E6870C5573841,
                    left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 7) AS UH_3BD6A59C9DBBA13C,
                    left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 4) AS UH_56C8047C9B06D885,
                    CONCAT (
                        table_2564.column_2704,
                        ' ',
                        table_2564.column_2770
                    ) AS UH_B28070E7B7E22086,
                    FIRST_VALUE (table_2564.column_2767) OVER (
                        PARTITION BY
                            i2070.L_1__TICKER__C5F4C49604
                        ORDER BY
                            i2070.L_1__DATE__830D4106E6
                    ) AS UH_50E3C1F21076A04D
                FROM
                    schema_2542.table_2564 AS i2070
                ORDER BY
                    UH_FF0E34C1334A718F ASC
            ),
            cte_c4c724f9_8c43_4447_844c_2a112abf3f23 as (
                with
                    cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
                        SELECT
                            table_2564.column_2770 AS UH_FF0E34C1334A718F,
                            TO_DATE (table_2564.column_2644, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                            table_2564.column_2591 AS UH_58C81BBAE68C4209,
                            CAST(table_2564.column_2767 as DECIMAL(38, 3)) AS UH_0964E7490E69F16D,
                            table_2564.column_2704 AS UH_F07E6870C5573841,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 7) AS UH_3BD6A59C9DBBA13C,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 4) AS UH_56C8047C9B06D885,
                            CONCAT (
                                table_2564.column_2704,
                                ' ',
                                table_2564.column_2770
                            ) AS UH_B28070E7B7E22086,
                            FIRST_VALUE (table_2564.column_2767) OVER (
                                PARTITION BY
                                    i2070.L_1__TICKER__C5F4C49604
                                ORDER BY
                                    i2070.L_1__DATE__830D4106E6
                            ) AS UH_50E3C1F21076A04D
                        FROM
                            schema_2542.table_2564 AS i2070
                        ORDER BY
                            UH_FF0E34C1334A718F ASC
                    )
                SELECT
                    table_2568.column_2576 AS UH_1909EAC2D231DD6B,
                    table_2568.column_2653 AS UH_92F772F39AD988ED,
                    table_2568.column_2610 AS UH_514FC83227D4EDF2,
                    case
                        when table_2568.column_2576 = last_day (table_2568.column_2576, column_2790) then 1
                        else 0
                    end AS UH_0101D066957E052A,
                    left (dateadd ('YEAR', 1, table_2568.column_2576), 4) AS UH_BC19CABF07D4C656
                FROM
                    table_2568 AS c0316
                WHERE
                    column_2587 ILIKE '1'
                ORDER BY
                    UH_1909EAC2D231DD6B ASC
            ),
            cte_da66352d_6ce2_49ce_8226_5a3bbb41c2a3 as (
                with
                    cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
                        SELECT
                            table_2564.column_2770 AS UH_FF0E34C1334A718F,
                            TO_DATE (table_2564.column_2644, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                            table_2564.column_2591 AS UH_58C81BBAE68C4209,
                            CAST(table_2564.column_2767 as DECIMAL(38, 3)) AS UH_0964E7490E69F16D,
                            table_2564.column_2704 AS UH_F07E6870C5573841,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 7) AS UH_3BD6A59C9DBBA13C,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 4) AS UH_56C8047C9B06D885,
                            CONCAT (
                                table_2564.column_2704,
                                ' ',
                                table_2564.column_2770
                            ) AS UH_B28070E7B7E22086,
                            FIRST_VALUE (table_2564.column_2767) OVER (
                                PARTITION BY
                                    i2070.L_1__TICKER__C5F4C49604
                                ORDER BY
                                    i2070.L_1__DATE__830D4106E6
                            ) AS UH_50E3C1F21076A04D
                        FROM
                            schema_2542.table_2564 AS i2070
                        ORDER BY
                            UH_FF0E34C1334A718F ASC
                    )
                SELECT
                    table_2568.column_2576 AS UH_EE0C82CB92CDCF2E,
                    table_2568.column_2653 AS UH_6A8801D276D99093,
                    table_2568.column_2610 AS UH_40DCE4CBD24F0A7C,
                    case
                        when table_2568.column_2576 = last_day (table_2568.column_2576, column_2607) then 1
                        else 0
                    end AS UH_96368C264D40B643,
                    left (dateadd ('MONTH', 1, table_2568.column_2576), 7) AS UH_F7DF7E630AFA1D81
                FROM
                    table_2568 AS c0316
                WHERE
                    column_2651 ILIKE '1'
                ORDER BY
                    UH_EE0C82CB92CDCF2E ASC
            ),
            a1 as (
                SELECT
                    table_2546.column_2683 AS UH_92F772F39AD988ED,
                    table_2546.column_2639 AS UH_BC19CABF07D4C656
                FROM
                    table_2546 AS c4651
            ),
            a2 as (
                SELECT
                    table_2559.column_2671 AS UH_F7DF7E630AFA1D81,
                    table_2559.column_2776 AS UH_6A8801D276D99093
                FROM
                    table_2559 AS c1344
            ),
            p1 as (
                SELECT
                    table_2568.column_2576 AS UH_65CE5957F5592694,
                    table_2568.column_2749 AS UH_B28070E7B7E22086,
                    table_2568.column_2610 AS UH_F07E6870C5573841,
                    table_2568.column_2653 AS UH_0964E7490E69F16D,
                    table_2568.column_2598 AS UH_56C8047C9B06D885,
                    table_2568.column_2842 AS UH_3BD6A59C9DBBA13C
                FROM
                    table_2568 AS c0316
            )
        SELECT
            table_2547.column_2615 AS UH_687D1D46F8A5B651,
            table_2547.column_2658 AS UH_28F4A35747EA0274,
            table_2547.column_2708 AS UH_AA1FCF59717B80AB,
            table_2547.column_2579 AS UH_7FE40783F09C637F,
            (
                (table_2547.column_2579 / table_2560.column_2645) - 1
            ) * 100 AS UH_4050277F68110E5E,
            table_2544.column_2652 AS UH_5CEE650DC0BAF835,
            (
                (table_2547.column_2579 / table_2544.column_2652) - 1
            ) * 100 AS UH_7AC21AAF3B07A1AB
        FROM
            table_2547 AS p6701
            LEFT JOIN table_2544 AS a5760 ON table_2544.column_2713 = table_2547.column_2694
            LEFT JOIN table_2560 AS a5761 ON table_2560.column_2699 = table_2547.column_2588
        ORDER BY
            UH_687D1D46F8A5B651 ASC
    ),
    cte_d4667322_38d8_47f6_93a6_6cabea506e8e as (
        SELECT
            table_2550.column_2634 AS UH_439A8F94F02832BA,
            table_2550.column_2612 AS UH_8C49F48C4CCB8698,
            table_2550.column_2733 AS UH_61BF1508FDC779BB,
            table_2550.column_2688 AS UH_B3492BAE64560E42,
            table_2550.column_2850 AS UH_D08F1496FC258AB1,
            table_2550.column_2660 AS UH_5A79454169A76BFF,
            table_2550.column_2593 AS UH_99FDB2C7CFC4AC5C,
            table_2550.column_2740 AS UH_0D21AED63876730E,
            table_2550.column_2748 AS UH_40861D00A51BA340,
            table_2550.column_2802 AS UH_F8BEE82FA285C0C1,
            table_2550.column_2676 AS UH_C8036DC63CE84EC1,
            table_2550.column_2751 AS UH_EB79B2A898A3F169,
            table_2550.column_2715 AS UH_0A4378EE90118B47,
            table_2550.column_2703 AS UH_7692DD7CB1AB5580,
            table_2550.column_2625 AS UH_4C0A00613DF168D6,
            table_2550.column_2596 AS UH_0A2A153BAAF5BAE3,
            TO_DATE (table_2550.column_2634, 'DD/MM/YYYY') AS UH_3DA72BEEDBE8750F
        FROM
            schema_2542.table_2550 AS m0243
        ORDER BY
            UH_439A8F94F02832BA ASC
    ),
    cte_a8e7867c_f64f_4584_a1e2_a8a7d202b67d as (
        with
            cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
                SELECT
                    TO_DATE (table_2558.column_2604, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
                    table_2558.column_2783 AS UH_4A953737BEE1C143,
                    table_2558.column_2825 AS UH_5E59A8E4B632AD08,
                    table_2558.column_2600 AS UH_ACD23440ED0B3A1C,
                    table_2558.column_2665 AS UH_5B4AFC1D55960E91,
                    table_2558.column_2766 AS UH_3B298B340A3AA73E,
                    CASE
                        WHEN table_2558.column_2766 = '' THEN 0
                        ELSE CAST(table_2558.column_2766 AS DECIMAL(38, 12))
                    END AS UH_C5591FBB04AAC642,
                    CAST(table_2558.column_2665 AS DECIMAL(38, 12)) AS UH_B1D0046780C85DFA,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 7) AS UH_DCE9829EEDFB87EE,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 4) AS UH_C30D64DC99F0374D
                FROM
                    schema_2542.table_2558 AS m5636
                ORDER BY
                    UH_5D80FE84FD58ACF9 ASC
            )
        SELECT
            table_2552.column_2618 AS UH_90E3B790134DD8EC,
            table_2552.column_2695 AS UH_7B3DD5E2F3BDFFAE,
            table_2552.column_2575 AS UH_4395CAC50FEDD42A,
            table_2552.column_2641 AS UH_7D97C0D42EB6A550,
            table_2552.column_2648 AS UH_562914F30A07C8A1,
            table_2552.column_2583 AS UH_FB087243CF5CF40D,
            table_2552.column_2590 AS UH_778A91686286894F,
            table_2552.column_2611 AS UH_1EA45AB23443BF5A,
            case
                when table_2552.column_2618 = last_day (table_2552.column_2618, column_2607) then 1
                else 0
            end AS UH_2C7819A556F16884,
            left (dateadd ('MONTH', 1, table_2552.column_2618), 7) AS UH_381C1BE83905280E
        FROM
            table_2552 AS c7447
        WHERE
            column_2666 ILIKE '1'
        ORDER BY
            UH_90E3B790134DD8EC ASC
    ),
    cte_b5bf0778_f4e1_465f_8bc7_3311ae5d6986 as (
        with
            cte_a62bd555_7a64_4a7c_8d4c_d7b4833e3cd2 as (
                SELECT
                    TO_DATE (table_2558.column_2604, 'DD/MM/YYYY') AS UH_5D80FE84FD58ACF9,
                    table_2558.column_2783 AS UH_4A953737BEE1C143,
                    table_2558.column_2825 AS UH_5E59A8E4B632AD08,
                    table_2558.column_2600 AS UH_ACD23440ED0B3A1C,
                    table_2558.column_2665 AS UH_5B4AFC1D55960E91,
                    table_2558.column_2766 AS UH_3B298B340A3AA73E,
                    CASE
                        WHEN table_2558.column_2766 = '' THEN 0
                        ELSE CAST(table_2558.column_2766 AS DECIMAL(38, 12))
                    END AS UH_C5591FBB04AAC642,
                    CAST(table_2558.column_2665 AS DECIMAL(38, 12)) AS UH_B1D0046780C85DFA,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 7) AS UH_DCE9829EEDFB87EE,
                    left (TO_DATE (table_2558.column_2604, 'DD/MM/YYYY'), 4) AS UH_C30D64DC99F0374D
                FROM
                    schema_2542.table_2558 AS m5636
                ORDER BY
                    UH_5D80FE84FD58ACF9 ASC
            )
        SELECT
            table_2552.column_2618 AS UH_4E0ACF9B987FBEE3,
            table_2552.column_2695 AS UH_61F8CF8BC3911D4F,
            table_2552.column_2575 AS UH_95D21B5E8DF3FE7A,
            table_2552.column_2641 AS UH_CAC8C78DC7FA3DB2,
            table_2552.column_2648 AS UH_ABB88326A4828A22,
            table_2552.column_2583 AS UH_4FEE7460995E5F91,
            table_2552.column_2590 AS UH_015FA9333CE61928,
            table_2552.column_2611 AS UH_6C6A665A9720BD13,
            case
                when table_2552.column_2618 = last_day (table_2552.column_2618, column_2790) then 1
                else 0
            end AS UH_F9B3D31E28F3E01B,
            left (dateadd ('YEAR', 1, table_2552.column_2618), 4) AS UH_A82129841CC09D28
        FROM
            table_2552 AS c7447
        WHERE
            column_2768 ILIKE '%1%'
        ORDER BY
            UH_4E0ACF9B987FBEE3 ASC
    ),
    cte_9f66c556_1f65_4dc3_8e76_5a440580d83a as (
        with
            cte_9948168a_fdf9_4830_b7c5_e1c4517192b0 as (
                SELECT DISTINCT
                    CASE
                        WHEN table_2553.column_2795 = 'LDMLONG' THEN 'FUND1'
                        ELSE NULL
                    END AS UH_D6A8948F8B678837,
                    LEFT (table_2553.column_2609, 6) AS UH_0CD05BAA62496454,
                    MAX(
                        LAST_DAY (
                            TO_DATE (table_2553.column_2609, 'YYYYMMDD'),
                            column_2846
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
                            WHEN table_2553.column_2701 > 0 THEN table_2553.column_2701
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
                            WHEN table_2553.column_2701 < 0 THEN table_2553.column_2701
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
                                WHEN table_2553.column_2701 > 0 THEN table_2553.column_2701
                                ELSE 0
                            END
                        ) - (
                            CASE
                                WHEN table_2553.column_2701 < 0 THEN table_2553.column_2701
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
                                ABS(table_2553.column_2701) / table_2553.column_2647
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
                    FIRST_VALUE (table_2553.column_2647) OVER (
                        PARTITION BY
                            t4634.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                            LEFT (t4634.L_1__DATE_ID__0BE2505CFE, 6)
                        ORDER BY
                            t4634.L_1__DATE_ID__0BE2505CFE
                    ) AS UH_0615DE85B613C8A2
                FROM
                    schema_2542.table_2553 AS t4634
                ORDER BY
                    UH_0CD05BAA62496454 ASC
            )
        SELECT
            table_2572.column_2664 AS UH_8C8B21C2A4D05334,
            table_2572.column_2723 AS UH_ECE3615915F146D1,
            table_2572.column_2682 AS UH_435E4C4A2012AF1F,
            table_2572.column_2603 AS UH_4E6FB1CAD1F9D4A5,
            table_2572.column_2700 AS UH_88D4F973916A28BB,
            table_2572.column_2630 AS UH_D89DC59DE3440953,
            table_2572.column_2642 AS UH_F3A6E2A6F6F0B0D7,
            (
                (table_2572.column_2630 / table_2572.column_2642) / 2
            ) * 100 AS UH_3065C273BCC343E0,
            table_2572.column_2627 AS UH_5FA5BFD8200340AF,
            NVL (
                LEAST (
                    table_2572.column_2603,
                    ABS(table_2572.column_2700)
                ) / table_2572.column_2642,
                0
            ) AS UH_90F8460CAACD4013,
            (
                LEAST (
                    SUM(table_2572.column_2603) OVER (
                        ORDER BY
                            c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                            AND CURRENT ROW
                    ),
                    ABS(
                        SUM(table_2572.column_2700) OVER (
                            ORDER BY
                                c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                                AND CURRENT ROW
                        )
                    )
                )
            ) / AVG(table_2572.column_2642) OVER (
                ORDER BY
                    c2300.UH_0CD05BAA62496454 ROWS BETWEEN 11 PRECEDING
                    AND CURRENT ROW
            ) AS UH_4ACD3F273B71139F
        FROM
            table_2572 AS c2300
        ORDER BY
            UH_435E4C4A2012AF1F ASC
    ),
    cte_195838c3_9953_4579_94f5_35bc6a28b77f as (
        with
            cte_30adc85e_aba8_4f67_ae71_b8809fba22bb as (
                with
                    cte_81baaa39_4f00_4c39_a840_f9fdffada5ed as (
                        SELECT
                            table_2556.column_2750 AS UH_DC244AE0330745DE,
                            table_2556.column_2781 AS UH_20EA696BEEE3A12C,
                            table_2556.column_2669 AS UH_D33132BF664C12A7,
                            table_2556.column_2771 AS UH_8AF0B8BDF6DB49F7,
                            table_2556.column_2678 AS UH_3645E57371D21B66,
                            table_2556.column_2787 AS UH_CF3A719C9185BF4B,
                            table_2556.column_2693 AS UH_F36E87222C14AC5B,
                            table_2556.column_2750 != 'FX' AS UH_B5658A9B439B51DA,
                            SUM(table_2556.column_2693) OVER (
                                PARTITION BY
                                    l3411.L_1__FUND_GROUP_CODE__A9BAC3C5A3,
                                    l3411.L_1__FX_CURRENCY_CODE__A7916A885E
                            ) AS UH_59B1562A197A6018
                        FROM
                            schema_2542.table_2556 AS l3411
                        WHERE
                            column_2758 ILIKE 'true'
                        ORDER BY
                            UH_DC244AE0330745DE ASC
                    ),
                    a1 as (
                        SELECT
                            table_2574.column_2774 AS gt_29cbb412_7bbb_454e_aa87_e19755e782fe,
                            table_2574.column_2785 AS gt_17d2d30d_0dc4_4415_b658_73d576b98e10,
                            table_2574.column_2743 AS gt_2f2f7f2e_7b22_4dad_ba95_bed69b8f574f
                        FROM
                            schema_2542.table_2574 AS l6607
                    ),
                    a2 as (
                        SELECT
                            table_2556.column_2669 AS gt_dc37a044_0478_4e58_a045_7b746d0b3466,
                            table_2556.column_2771 AS gt_7fed1589_a818_4ebf_848a_da15187575ac
                        FROM
                            schema_2542.table_2556 AS l3411
                    ),
                    a3 as (
                        SELECT
                            table_2549.column_2756 AS UH_59B1562A197A6018,
                            table_2549.column_2605 AS UH_D33132BF664C12A7,
                            table_2549.column_2711 AS UH_8AF0B8BDF6DB49F7
                        FROM
                            table_2549 AS c5340
                    ),
                    p1 as (
                        SELECT
                            table_2565.column_2687 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                            table_2565.column_2852 AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
                            table_2565.column_2631 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27
                        FROM
                            schema_2542.table_2565 AS g0621
                    )
                SELECT DISTINCT
                    table_2547.column_2706 AS UH_7984D9BFB4F58688,
                    table_2547.column_2592 AS UH_4C2F680C0C7751CD,
                    table_2544.column_2677 AS UH_F2E3823313DB74F6,
                    table_2544.column_2670 AS UH_5790AB51C3D61ED8,
                    table_2569.column_2675 AS UH_5DA416C9B0A910F3,
                    ABS(
                        CASE
                            WHEN CONCAT (table_2547.column_2706, table_2547.column_2594) = CONCAT (table_2544.column_2775, table_2544.column_2670) THEN 0
                            ELSE CAST(table_2544.column_2677 AS DECIMAL(38, 12)) / CAST(table_2547.column_2592 AS NUMBER (38))
                        END
                    ) AS UH_52D93A543DA41E88,
                    CASE
                        WHEN table_2544.column_2677 > 0
                        AND table_2569.column_2675 < 0 THEN GREATEST (
                            table_2544.column_2677 + table_2569.column_2675,
                            0
                        )
                        ELSE CASE
                            WHEN table_2544.column_2677 < 0
                            AND table_2569.column_2675 > 0 THEN LEAST (
                                table_2544.column_2677 + table_2569.column_2675,
                                0
                            )
                            ELSE table_2544.column_2677
                        END
                    END AS UH_1A1E835F0445B04D
                FROM
                    table_2547 AS p6701
                    LEFT JOIN table_2544 AS a5760 ON table_2544.column_2775 = table_2547.column_2706
                    LEFT JOIN table_2560 AS a5761 ON table_2560.column_2613 = table_2544.column_2775
                    AND table_2560.column_2640 = table_2544.column_2670
                    LEFT JOIN table_2569 AS a5762 ON table_2569.column_2814 = table_2544.column_2775
                    AND table_2569.column_2727 = table_2544.column_2670
            ),
            cte_8398e7e0_efde_43bb_8816_a6dba1a06e24 as (
                with
                    a1 as (
                        SELECT
                            table_2557.column_2735 AS gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1,
                            table_2557.column_2820 AS gt_c3b61d50_3b58_4ab0_9751_224a9ce2e28b
                        FROM
                            schema_2542.table_2557 AS b2413
                    ),
                    a2 as (
                        SELECT
                            table_2565.column_2687 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                            table_2565.column_2654 AS gt_c194d47d_8ad3_4486_ae01_7b450dae7069
                        FROM
                            schema_2542.table_2565 AS g0621
                    ),
                    p1 as (
                        SELECT
                            table_2543.column_2623 AS gt_bc2847f1_4228_41bd_99ff_34355072a6fc,
                            table_2543.column_2680 AS gt_60079311_9d9e_475d_a7b0_e38cce009176,
                            table_2543.column_2629 AS gt_9a9a4be8_a848_4aff_a60b_79430e678b64
                        FROM
                            schema_2542.table_2543 AS s7270
                    )
                SELECT DISTINCT
                    table_2544.column_2837 AS UH_4FD7FFD09F4A91C5,
                    SUM(
                        ABS(
                            (
                                CASE
                                    WHEN table_2547.column_2839 = RIGHT (table_2544.column_2760, 3) THEN 0
                                    ELSE CAST(table_2547.column_2614 as DECIMAL(38, 12))
                                END
                            ) / table_2560.column_2753
                        ) * 100
                    ) OVER (
                        PARTITION BY
                            a5760.gt_b54663a5_7f69_4c54_9378_9ed92a60e1d1
                    ) AS UH_B57C1D76748904B8
                FROM
                    table_2547 AS p6701
                    LEFT JOIN table_2544 AS a5760 ON table_2544.column_2760 = table_2547.column_2827
                    LEFT JOIN table_2560 AS a5761 ON table_2560.column_2737 = table_2544.column_2837
            ),
            a1 as (
                SELECT
                    table_2561.column_2742 AS UH_7984D9BFB4F58688,
                    table_2561.column_2685 AS UH_52D93A543DA41E88,
                    table_2561.column_2732 AS UH_1A1E835F0445B04D
                FROM
                    table_2561 AS c6007
            ),
            a2 as (
                SELECT
                    table_2570.column_2821 AS UH_B57C1D76748904B8,
                    table_2570.column_2635 AS UH_4FD7FFD09F4A91C5
                FROM
                    table_2570 AS c7535
            ),
            p1 as (
                SELECT
                    table_2565.column_2687 AS gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c,
                    table_2565.column_2631 AS gt_8c158412_3f3a_4002_9059_5d9567cebe27,
                    table_2565.column_2852 AS gt_b3f2a5a4_212e_4ede_9865_a65c88d2ed71,
                    table_2565.column_2738 AS gt_d3057e86_ebcf_4dc0_9c04_f0b830fc53ec
                FROM
                    schema_2542.table_2565 AS g0621
            )
        SELECT DISTINCT
            table_2547.column_2706 AS UH_34CCFB90CCC6F02D,
            table_2547.column_2594 AS UH_2996455587BED195,
            table_2547.column_2592 AS UH_ABBF62C1F08513B5,
            SUM(table_2544.column_2584 * 100) OVER (
                PARTITION BY
                    p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ) AS UH_ECECB096A13E53A7,
            SUM(
                ABS(
                    table_2544.column_2811 / CAST(table_2547.column_2592 AS NUMBER)
                ) * 100
            ) OVER (
                PARTITION BY
                    p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ) AS UH_C0F31F5050675FDD,
            table_2560.column_2710 AS UH_355CB087330094C6,
            (table_2547.column_2702 * 100) + SUM(table_2544.column_2584 * 100) OVER (
                PARTITION BY
                    p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
            ) + table_2560.column_2710 AS UH_159FD7EBE29E8604,
            (table_2547.column_2702 * 100) + GREATEST (
                SUM(
                    ABS(
                        table_2544.column_2811 / CAST(table_2547.column_2592 AS NUMBER)
                    ) * 100
                ) OVER (
                    PARTITION BY
                        p6701.gt_ed6b6ee5_249e_4521_99ae_7a8f90e15e6c
                ),
                0
            ) + 0 AS UH_F71D209652B52DC5
        FROM
            table_2547 AS p6701
            LEFT JOIN table_2544 AS a5760 ON table_2544.column_2581 = table_2547.column_2706
            INNER JOIN table_2560 AS a5761 ON table_2560.column_2762 = table_2547.column_2706
    ),
    cte_6a6b154f_57f0_429f_9e17_b9dce8fa501b as (
        with
            cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
                SELECT
                    table_2564.column_2770 AS UH_FF0E34C1334A718F,
                    TO_DATE (table_2564.column_2644, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                    table_2564.column_2591 AS UH_58C81BBAE68C4209,
                    CAST(table_2564.column_2767 as DECIMAL(38, 3)) AS UH_0964E7490E69F16D,
                    table_2564.column_2704 AS UH_F07E6870C5573841,
                    left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 7) AS UH_3BD6A59C9DBBA13C,
                    left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 4) AS UH_56C8047C9B06D885,
                    CONCAT (
                        table_2564.column_2704,
                        ' ',
                        table_2564.column_2770
                    ) AS UH_B28070E7B7E22086,
                    FIRST_VALUE (table_2564.column_2767) OVER (
                        PARTITION BY
                            i2070.L_1__TICKER__C5F4C49604
                        ORDER BY
                            i2070.L_1__DATE__830D4106E6
                    ) AS UH_50E3C1F21076A04D
                FROM
                    schema_2542.table_2564 AS i2070
                ORDER BY
                    UH_FF0E34C1334A718F ASC
            ),
            cte_c4c724f9_8c43_4447_844c_2a112abf3f23 as (
                with
                    cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
                        SELECT
                            table_2564.column_2770 AS UH_FF0E34C1334A718F,
                            TO_DATE (table_2564.column_2644, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                            table_2564.column_2591 AS UH_58C81BBAE68C4209,
                            CAST(table_2564.column_2767 as DECIMAL(38, 3)) AS UH_0964E7490E69F16D,
                            table_2564.column_2704 AS UH_F07E6870C5573841,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 7) AS UH_3BD6A59C9DBBA13C,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 4) AS UH_56C8047C9B06D885,
                            CONCAT (
                                table_2564.column_2704,
                                ' ',
                                table_2564.column_2770
                            ) AS UH_B28070E7B7E22086,
                            FIRST_VALUE (table_2564.column_2767) OVER (
                                PARTITION BY
                                    i2070.L_1__TICKER__C5F4C49604
                                ORDER BY
                                    i2070.L_1__DATE__830D4106E6
                            ) AS UH_50E3C1F21076A04D
                        FROM
                            schema_2542.table_2564 AS i2070
                        ORDER BY
                            UH_FF0E34C1334A718F ASC
                    )
                SELECT
                    table_2568.column_2576 AS UH_1909EAC2D231DD6B,
                    table_2568.column_2653 AS UH_92F772F39AD988ED,
                    table_2568.column_2610 AS UH_514FC83227D4EDF2,
                    case
                        when table_2568.column_2576 = last_day (table_2568.column_2576, column_2790) then 1
                        else 0
                    end AS UH_0101D066957E052A,
                    left (dateadd ('YEAR', 1, table_2568.column_2576), 4) AS UH_BC19CABF07D4C656
                FROM
                    table_2568 AS c0316
                WHERE
                    column_2587 ILIKE '1'
                ORDER BY
                    UH_1909EAC2D231DD6B ASC
            ),
            cte_da66352d_6ce2_49ce_8226_5a3bbb41c2a3 as (
                with
                    cte_923fa2ea_0085_4216_acb4_86a376b3e04a as (
                        SELECT
                            table_2564.column_2770 AS UH_FF0E34C1334A718F,
                            TO_DATE (table_2564.column_2644, 'DD/MM/YYYY') AS UH_65CE5957F5592694,
                            table_2564.column_2591 AS UH_58C81BBAE68C4209,
                            CAST(table_2564.column_2767 as DECIMAL(38, 3)) AS UH_0964E7490E69F16D,
                            table_2564.column_2704 AS UH_F07E6870C5573841,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 7) AS UH_3BD6A59C9DBBA13C,
                            left (TO_DATE (table_2564.column_2644, 'DD/MM/YYYY'), 4) AS UH_56C8047C9B06D885,
                            CONCAT (
                                table_2564.column_2704,
                                ' ',
                                table_2564.column_2770
                            ) AS UH_B28070E7B7E22086,
                            FIRST_VALUE (table_2564.column_2767) OVER (
                                PARTITION BY
                                    i2070.L_1__TICKER__C5F4C49604
                                ORDER BY
                                    i2070.L_1__DATE__830D4106E6
                            ) AS UH_50E3C1F21076A04D
                        FROM
                            schema_2542.table_2564 AS i2070
                        ORDER BY
                            UH_FF0E34C1334A718F ASC
                    )
                SELECT
                    table_2568.column_2576 AS UH_EE0C82CB92CDCF2E,
                    table_2568.column_2653 AS UH_6A8801D276D99093,
                    table_2568.column_2610 AS UH_40DCE4CBD24F0A7C,
                    case
                        when table_2568.column_2576 = last_day (table_2568.column_2576, column_2607) then 1
                        else 0
                    end AS UH_96368C264D40B643,
                    left (dateadd ('MONTH', 1, table_2568.column_2576), 7) AS UH_F7DF7E630AFA1D81
                FROM
                    table_2568 AS c0316
                WHERE
                    column_2651 ILIKE '1'
                ORDER BY
                    UH_EE0C82CB92CDCF2E ASC
            ),
            a1 as (
                SELECT
                    table_2546.column_2683 AS UH_92F772F39AD988ED,
                    table_2546.column_2639 AS UH_BC19CABF07D4C656
                FROM
                    table_2546 AS c4651
            ),
            a2 as (
                SELECT
                    table_2559.column_2671 AS UH_F7DF7E630AFA1D81,
                    table_2559.column_2776 AS UH_6A8801D276D99093
                FROM
                    table_2559 AS c1344
            ),
            p1 as (
                SELECT
                    table_2568.column_2576 AS UH_65CE5957F5592694,
                    table_2568.column_2749 AS UH_B28070E7B7E22086,
                    table_2568.column_2610 AS UH_F07E6870C5573841,
                    table_2568.column_2653 AS UH_0964E7490E69F16D,
                    table_2568.column_2598 AS UH_56C8047C9B06D885,
                    table_2568.column_2842 AS UH_3BD6A59C9DBBA13C
                FROM
                    table_2568 AS c0316
            )
        SELECT
            table_2547.column_2615 AS UH_687D1D46F8A5B651,
            table_2547.column_2658 AS UH_28F4A35747EA0274,
            table_2547.column_2708 AS UH_AA1FCF59717B80AB,
            table_2547.column_2579 AS UH_7FE40783F09C637F,
            (
                (table_2547.column_2579 / table_2560.column_2645) - 1
            ) * 100 AS UH_4050277F68110E5E,
            table_2544.column_2652 AS UH_5CEE650DC0BAF835,
            (
                (table_2547.column_2579 / table_2544.column_2652) - 1
            ) * 100 AS UH_7AC21AAF3B07A1AB
        FROM
            table_2547 AS p6701
            LEFT JOIN table_2544 AS a5760 ON table_2544.column_2713 = table_2547.column_2694
            LEFT JOIN table_2560 AS a5761 ON table_2560.column_2699 = table_2547.column_2588
        ORDER BY
            UH_687D1D46F8A5B651 ASC
    ),
    cte_d4667322_38d8_47f6_93a6_6cabea506e8e as (
        SELECT
            table_2550.column_2634 AS UH_439A8F94F02832BA,
            table_2550.column_2612 AS UH_8C49F48C4CCB8698,
            table_2550.column_2733 AS UH_61BF1508FDC779BB,
            table_2550.column_2688 AS UH_B3492BAE64560E42,
            table_2550.column_2850 AS UH_D08F1496FC258AB1,
            table_2550.column_2660 AS UH_5A79454169A76BFF,
            table_2550.column_2593 AS UH_99FDB2C7CFC4AC5C,
            table_2550.column_2740 AS UH_0D21AED63876730E,
            table_2550.column_2748 AS UH_40861D00A51BA340,
            table_2550.column_2802 AS UH_F8BEE82FA285C0C1,
            table_2550.column_2676 AS UH_C8036DC63CE84EC1,
            table_2550.column_2751 AS UH_EB79B2A898A3F169,
            table_2550.column_2715 AS UH_0A4378EE90118B47,
            table_2550.column_2703 AS UH_7692DD7CB1AB5580,
            table_2550.column_2625 AS UH_4C0A00613DF168D6,
            table_2550.column_2596 AS UH_0A2A153BAAF5BAE3,
            TO_DATE (table_2550.column_2634, 'DD/MM/YYYY') AS UH_3DA72BEEDBE8750F
        FROM
            schema_2542.table_2550 AS m0243
        ORDER BY
            UH_439A8F94F02832BA ASC
    ),
    a1 as (
        SELECT
            table_2566.column_2784 AS UH_1EA45AB23443BF5A,
            table_2566.column_2759 AS UH_381C1BE83905280E,
            table_2566.column_2755 AS UH_90E3B790134DD8EC
        FROM
            table_2566 AS c3632
    ),
    a2 as (
        SELECT
            table_2563.column_2858 AS UH_6C6A665A9720BD13,
            table_2563.column_2674 AS UH_A82129841CC09D28
        FROM
            table_2563 AS c0771
    ),
    a3 as (
        SELECT
            table_2554.column_2853 AS gt_30e2ab2b_eb34_44e6_ab5f_b38c561e4c40,
            table_2554.column_2794 AS gt_676e3f9f_7754_4d65_a746_9086ac757f9b,
            table_2554.column_2649 AS gt_b2c3ef95_e41e_44d3_b62d_335353b9a758,
            table_2554.column_2796 AS gt_ecfc8339_adf3_4df3_a33d_387a059573c1
        FROM
            schema_2542.table_2554 AS s0665
    ),
    a4 as (
        SELECT
            table_2548.column_2589 AS UH_90F8460CAACD4013,
            table_2548.column_2788 AS UH_8C8B21C2A4D05334,
            table_2548.column_2582 AS UH_ECE3615915F146D1
        FROM
            table_2548 AS c1526
    ),
    a5 as (
        SELECT
            table_2567.column_2745 AS UH_159FD7EBE29E8604,
            table_2567.column_2621 AS UH_F71D209652B52DC5,
            table_2567.column_2712 AS UH_34CCFB90CCC6F02D
        FROM
            table_2567 AS c6736
    ),
    a6 as (
        SELECT
            table_2571.column_2602 AS UH_28F4A35747EA0274,
            table_2571.column_2809 AS UH_7FE40783F09C637F,
            table_2571.column_2650 AS UH_4050277F68110E5E,
            table_2571.column_2668 AS UH_7AC21AAF3B07A1AB,
            table_2571.column_2595 AS UH_687D1D46F8A5B651
        FROM
            table_2571 AS c0225
    ),
    a7 as (
        SELECT
            table_2562.column_2691 AS UH_0D21AED63876730E,
            table_2562.column_2679 AS UH_8C49F48C4CCB8698,
            table_2562.column_2657 AS UH_3DA72BEEDBE8750F
        FROM
            table_2562 AS c1743
    ),
    p1 as (
        SELECT
            table_2552.column_2618 AS UH_5D80FE84FD58ACF9,
            table_2552.column_2611 AS UH_B1D0046780C85DFA,
            table_2552.column_2734 AS UH_DCE9829EEDFB87EE,
            table_2552.column_2779 AS UH_C30D64DC99F0374D,
            table_2552.column_2641 AS UH_ACD23440ED0B3A1C
        FROM
            table_2552 AS c7447
    )
SELECT
    table_2547.column_2617 AS UH_893755EB52A1E609,
    table_2569.column_2789 AS UH_3CFC32F7423065E7,
    table_2569.column_2729 AS UH_18374F59B3703054,
    table_2569.column_2798 AS UH_787C3497B41AB746,
    year (table_2547.column_2617) AS UH_FED4353AF3C9754B,
    table_2547.column_2616 AS UH_DC4634F03C3103A3,
    table_2544.column_2577 AS UH_CA923BDFCB7A9286,
    table_2560.column_2585 AS UH_C0432D2D3BE5226A,
    (
        (table_2547.column_2616 / table_2544.column_2577) - 1
    ) * 100 AS UH_50337B98AD8E493C,
    POWER(
        LEAST (
            (
                (
                    (table_2547.column_2616 / table_2544.column_2577) - 1
                )
            ) - (POWER(1.1, 1 / 12) - 1),
            0
        ),
        2
    ) AS UH_07FF96528E8A5ECB,
    table_2547.column_2616 / MAX(table_2547.column_2616) OVER (
        PARTITION BY
            p6701.UH_ACD23440ED0B3A1C
        ORDER BY
            p6701.UH_5D80FE84FD58ACF9 ROWS BETWEEN UNBOUNDED PRECEDING
            AND CURRENT ROW
    ) - 1 AS UH_62227B828A7194C2,
    (
        (table_2547.column_2616 / table_2560.column_2585) - 1
    ) * 100 AS UH_60BEF4ED0F4CA718,
    table_2555.column_2601 AS UH_6B68542F0EC9B9BF,
    table_2555.column_2823 AS UH_BDF22E0940EA9F42,
    table_2555.column_2628 AS UH_FC01B6F984025CFF,
    POWER(
        LEAST (
            (table_2555.column_2628 / 100) - (POWER(1.1, 1 / 12) - 1),
            0
        ),
        2
    ) AS UH_47F35F809703222A,
    table_2555.column_2823 / MAX(table_2555.column_2823) OVER (
        PARTITION BY
            p6701.UH_ACD23440ED0B3A1C
        ORDER BY
            p6701.UH_5D80FE84FD58ACF9 ROWS BETWEEN UNBOUNDED PRECEDING
            AND CURRENT ROW
    ) - 1 AS UH_4702036A801925F0,
    table_2555.column_2580 AS UH_14E4561D98B7ACAC,
    table_2573.column_2836 AS UH_043D884C47A898BB,
    table_2551.column_2757 AS UH_1BFDC847EB80AC0C,
    table_2551.column_2761 AS UH_ACECAE7C8E263166,
    (
        (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100
    ) - table_2555.column_2628 AS UH_E5710D7D44FD0EB0,
    (
        (
            1 + (
                (
                    (table_2547.column_2616 / table_2544.column_2577) - 1
                )
            )
        ) / (1 + (table_2555.column_2628 / 100)) - 1
    ) * 100 AS UH_081C46831EB9808D,
    table_2545.column_2578 AS UH_DE5FAAB85196DCAD,
    (
        CASE
            WHEN table_2545.column_2578 = '' THEN 0
            ELSE CAST(table_2545.column_2578 AS DECIMAL(38, 12))
        END * 100
    ) - table_2555.column_2628 AS UH_28DAA6683053AB8B,
    CASE
        WHEN (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100 > 0 THEN (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100
        ELSE NULL
    END AS UH_C48E81316ECF09FD,
    CASE
        WHEN (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100 < 0 THEN (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100
        ELSE NULL
    END AS UH_E66467DE4E3C3D94,
    CASE
        WHEN (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100 > 0 THEN table_2555.column_2628
        ELSE NULL
    END AS UH_A085FE48B96BDD7E,
    CASE
        WHEN (
            (table_2547.column_2616 / table_2544.column_2577) - 1
        ) * 100 < 0 THEN table_2555.column_2628
        ELSE NULL
    END AS UH_0FFC953F8E95E49F
FROM
    table_2547 AS p6701
    LEFT JOIN table_2544 AS a5760 ON table_2544.column_2622 = table_2547.column_2720
    LEFT JOIN table_2560 AS a5761 ON table_2560.column_2721 = table_2547.column_2860
    INNER JOIN table_2569 AS a5762 ON table_2569.column_2798 = table_2547.column_2643
    INNER JOIN table_2573 AS a5763 ON table_2573.column_2778 = table_2569.column_2789
    AND table_2573.column_2619 = table_2544.column_2804
    INNER JOIN table_2551 AS a5764 ON table_2551.column_2646 = table_2569.column_2789
    LEFT JOIN table_2555 AS a5765 ON table_2555.column_2601 = table_2569.column_2803
    AND table_2555.column_2769 = table_2547.column_2617
    LEFT JOIN table_2545 AS a5766 ON table_2545.column_2724 = table_2569.column_2798
    AND table_2545.column_2586 = table_2547.column_2617
WHERE
    case
        when table_2547.column_2617 = last_day (table_2547.column_2617, column_2607) then 1
        else 0
    end ILIKE '%1%'
ORDER BY
    UH_893755EB52A1E609 ASC