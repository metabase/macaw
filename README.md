[![License](https://img.shields.io/badge/license-Eclipse%20Public%20License-blue.svg?style=for-the-badge)](https://raw.githubusercontent.com/metabase/macaw/master/LICENSE)
[![GitHub last commit](https://img.shields.io/github/last-commit/metabase/second-date?style=for-the-badge)](https://github.com/metabase/macaw/commits/)

[![Clojars Project](https://clojars.org/io.github.metabase/macaw/latest-version.svg)](https://clojars.org/io.github.metabase/macaw)

<img src="./assets/logo.png" width="400px" alt="Macaw logo" />

# Macaw

Macaw is a limited Clojure wrapper for [JSqlParser](https://github.com/JSQLParser/JSqlParser). Similar to its parrot
namesake, it's intelligent, can be taught to speak SQL, and has many colors (supports many dialects).

## Rationale

JSqlParser does great work actually parsing SQL, but its use of the visitor pattern makes Clojure inter-op very
awkward. Macaw exists to make working with JSqlParser from within Clojure feel more idiomatic and pleasant, letting you
walk over a query with custom callbacks and returning persistent data structures.

Currently it's especially useful for extracting the columns, tables, and side-effecting commands from a SQL string (see
[Query Parsing](#query-parsing)) and also intelligently renaming the columns and tables used in it (see [Query
Rewriting](#query-rewriting)).

# Preliminaries

## Building

To build a local JAR, use

```
./bin/build-jar
```

This will create a JAR in the `target` directory.

## Working with the Java files

To compile the Java files, use

```
./bin/java-compile
```

If you're working on Macaw and make changes to a Java file, you must:

1. Recompile
2. Restart your Clojure REPL

for the changes to take effect.

# Usage

For detailed documentation, refer to [the Marginalia documentation here](https://metabase-dev-docs.github.io/macaw/).

## Query Parsing

For extracting information from a query, use `parsed-query` to get a parse object and `query->components` to turn it
into something useful. For example:

```clojure
;; macaw.core
(-> "SELECT total FROM orders"
    parsed-query
    query->components)
;; => {:columns           #{{:component {:column "total"}, :context ["SELECT"]}},
;;     :has-wildcard?     #{},
;;     :mutation-commands #{},
;;     :tables            #{{:component {:table "orders"}, :context ["FROM" "SELECT"]}},
;;     :table-wildcards   #{}}
```

The returned map will always have that general shape as of Macaw 0.1.30. Each of the main keys will always refer to a
set, and each set will always consist of maps with a `:component` key (described below) and a `:context` key (described
[farther below](#context)).

### Column and table `:component`s

Columns will have a `:column` key with the name as it appears in the query, and may also have a `:table` or `:schema`
key if available.

```clojure
(-> "SELECT id, orders.total, public.orders.tax FROM public.orders"
    parsed-query
    query->components
    :columns)
;; => #{{:component {:column "id"},                                       :context ["SELECT"]}
;;      {:component {:column "total", :table "orders", :schema "public"}, :context ["SELECT"]}
;;      {:component {:column "tax",   :table "orders", :schema "public"}, :context ["SELECT"]}}
```
Note that the schema for `total` was inferred, but neither the table nor the schema for `id` was inferred since there
are similar queries where it could be ambiguous (e.g., with a JOIN).

Macaw will also resolve aliases sensibly:

```clojure
(-> "SELECT o.id, o.total, u.name FROM public.orders o JOIN users u ON u.id = o.user_id"
    parsed-query
    query->components
    (select-keys [:columns :tables]))
;; => {:columns
;;     #{{:component {:column "id", :table "orders", :schema "public"}, :context ["SELECT"]}
;;       {:component {:column "total", :table "orders", :schema "public"}, :context ["SELECT"]}
;;       {:component {:column "name", :table "users"}, :context ["SELECT"]}
;;       {:component {:column "id", :table "users"}, :context ["JOIN" "SELECT"]}
;;       {:component {:column "user_id", :table "orders", :schema "public"}, :context ["JOIN" "SELECT"]}},
;;     :tables
;;     #{{:component {:table "orders", :schema "public"}, :context ["FROM" "SELECT"]}
;;       {:component {:table "users"}, :context ["FROM" "JOIN" "SELECT"]}}}
```
Note that `:tables` is similar to `:columns`, but only contains the `:table` and (if available anywhere in the query)
`:schema` keys.

### Wildcard `:component`s

The `:has-wildcard?` and `:table-wildcards` keys refer to `*`s:

```clojure
(-> "SELECT * from orders"
    parsed-query
    query->components
    :has-wildcard?)
;; => #{{:component true, :context ["SELECT"]}}
```

```clojure
(-> "SELECT o.*, u.* FROM public.orders o JOIN users u ON u.id = o.user_id"
    parsed-query
    query->components
    :table-wildcards)
;; => #{{:component {:table "users"}, :context ["SELECT"]}
;;      {:component {:table "orders", :schema "public"}, :context ["SELECT"]}}
```

The shape of `:table-wildcards` will be the same as the shape of `:tables`. The `:component` for `has-wildcard?` will
never be false.

### Mutation commands

Any commands that could change the state of the database are returned in `:mutation-commands`:

```clojure
(-> "DROP TABLE orders"
    parsed-query
    query->components
    :mutation-commands)
;; => #{{:component "drop", :context []}}
```

The list of recognized commands as of 0.1.30 is:

```bash
$ grep MUTATION_COMMAND java/com/metabase/macaw/AstWalker.java | grep -oEi '".+"' | sort
"alter-sequence"
"alter-session"
"alter-system"
"alter-table"
"alter-view"
"create-function"
"create-index"
"create-schema"
"create-sequence"
"create-synonym"
"create-table"
"create-view"
"delete"
"drop"
"grant"
"insert"
"purge"
"rename-table"
"truncate"
"update"
```

### Context

As can be seen above, every item additional has a `:context` key containing a stack (most-specific first) of the query
components in which the item was found. The definitive list of components if found in
`com.metabase.macaw.AstWalker.QueryScopeLabel`, but as of 0.1.30 the list is as follows (and is unlikely to change):

```java
DELETE,
ELSE,
FROM,
GROUP_BY,
HAVING,
IF,
INSERT,
JOIN,
SELECT,
SUB_SELECT,
UPDATE,
WHERE;
```

## Query Rewriting

Editing queries can be done with `replace-names`. It takes two arguments, the query itself (as a string) and a map of
maps. The outer keys of the map are `:schemas`, `:tables`, and `:columns`. The inner maps take the form `old-name -> new-name`. For
example:

```clojure
(replace-names "SELECT p.id, orders.total FROM people p, public.orders;"
               {:schemas  {"public" "private"}
                :tables   {"people" "users"}
                :columns  {{:table "orders" :column "total"} "amount"}})
;; => "SELECT p.id, orders.amount FROM users p, private.orders;"
```
## Error Handling

Macaw makes no effort to recover from errors. Malformed SQL strings will probably raise a
[JSqlParserException](https://javadoc.io/static/com.github.jsqlparser/jsqlparser/4.9/net/sf/jsqlparser/JSQLParserException.html),
which Macaw will forward on to your code.

# Contributing

External contributions are welcome! We require all third-party contributors to sign [our license
agreement](https://docs.google.com/a/metabase.com/forms/d/e/1FAIpQLSfc9GWyJ3F9U_4NzHLeTblgtog1FKtG3CjLshE4FAAKSdvNoQ/viewform).
Macaw's philosophy towards third-party contributions is largely similar to [that of
Metabase's](https://github.com/metabase/metabase/blob/master/docs/developers-guide/contributing.md). We'd especially
like to call out that document's YOLO method of PR submission:

> If you come up with something really cool, and want to share it with us, just submit a PR. If it hasn't gone through
> the above process, we probably won't merge it as is, but if it's compelling, we're more than willing to help you via
> code review, design review and generally OCD nitpicking so that it fits into the rest of our codebase.

Since Macaw is a *much* simpler project than Metabase (and also doesn't involve a graphical user interface) we should
be able to respond to product proposals fairly quickly.
