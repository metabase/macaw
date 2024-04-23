# 0.1.28

* Context is now added to each returned component in `query->components`. E.g., instead of `#{"foo" "bar"}`, it will
  return its elements as `#{{:component "foo" :context ["SELECT"]}, {:component "bar" :context ["WHERE" "SELECT"]}}`

* `has-wildcard` now returns a set of such maps. For example:

```
(query->components (parsed-query "SELECT * FROM (SELECT * FROM orders) WHERE total > 10"))
; =>
{ ...
  :has-wildcard?
  #{{:component true, :context ["SELECT" "SUB_SELECT" "FROM" "SELECT"]}
    {:component true, :context ["SELECT"]}}}
```

* The context stack can be counter-intuitive. For example, in the above query, the context stack for the `"total"`
  column from the `WHERE` clauses is `["WHERE" "JOIN" "FROM" "SELECT"]`. The vector is arranged from most- to
  least-specific. For most cases you probably only want to look at the first element and/or make assertions about the
  relative order of multiple elements.

# 0.0.1 through 0.1.27

* (The project was young enough not to merit a changelog. Refer to other documentation)
