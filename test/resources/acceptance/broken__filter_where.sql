  select
    e.instance_id
    , percentile_cont(0.75) within group (order by e.running_time) as p75_time
    -- the parser trips up on the opening bracket in the `filter (where ...)` clause
    , percentile_cont(0.75) within group (order by e.running_time) filter (where e.error = '') as p75_success_time
  from execution e
  group by 1
