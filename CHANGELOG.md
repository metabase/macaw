# 0.1.31
* `replace-names` now supports `:schemas` in the renames map

# 0.1.29

* Now each component is a map rather than a string, with tables being `{:table "foo" :schema "bar"}` (`:schema` is
  present only if known) and columns `{:column "foo" :table "bar" :schema "baz"}` (`:table` and `:schema` being
  optional). So `:columns` will look like that:

```
#{{:component {:column "foo" :table "bar"} :context ["SELECT"]}}
```

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
