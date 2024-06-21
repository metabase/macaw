with q as (
  select
    id
  from
    report_card
  where
    created_at > '2024-01-01'
  limit
    1
)
select
  created_at
from
  report_dashboardcard dc
  join q on dc.card_id = q.id;
