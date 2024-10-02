with final as (
   select
     id,
     amount_paid_cents::float / 100 as amount_paid
   from invoice
   where not is_deleted
 )

 select * from final
