# pymacaw

Python port of [Macaw](https://github.com/metabase/macaw), a SQL analysis library using [SQLGlot](https://github.com/tobymao/sqlglot).

## Features

- **Query Component Extraction**: Extract tables, columns, wildcards, and mutation commands from SQL queries with context tracking
- **Query Rewriting**: Rename schemas, tables, and columns while preserving query structure
- **AST Conversion**: Convert SQL to Python dictionaries for analysis
- **Multi-Dialect Support**: Works with 20+ SQL dialects via SQLGlot

## Installation

```bash
cd python
pip install -e .
```

For development:

```bash
pip install -e ".[dev]"
```

## Usage

### Extract Components

```python
from pymacaw import query_to_components

result = query_to_components("SELECT o.id, o.total FROM orders o WHERE o.status = 'active'")

# result contains:
# - tables: Set of table references with context
# - columns: Set of column references with context
# - source_columns: Deduplicated column identifiers
# - table_wildcards: t.* patterns
# - has_wildcard: SELECT * presence
# - mutation_commands: DDL/DML operations
```

### Rename Identifiers

```python
from pymacaw import replace_names

sql = "SELECT a.x, b.y FROM a JOIN b ON a.id = b.a_id"

result = replace_names(
    sql,
    {
        "tables": {(("table", "a"),): "users"},
        "columns": {(("table", "a"), ("column", "x")): "name"},
    }
)
# Result: "SELECT users.name, b.y FROM users JOIN b ON users.id = b.a_id"
```

### Convert to AST

```python
from pymacaw import to_ast

ast = to_ast("SELECT id FROM users WHERE active = 1")
# Returns nested dict with type, select, from, where, etc.
```

## API Reference

### `query_to_components(sql, dialect=None, preserve_identifiers=True, strip_contexts=False)`

Extract all SQL components from a query.

**Parameters:**
- `sql`: SQL query string
- `dialect`: SQLGlot dialect name (e.g., "postgres", "mysql", "snowflake")
- `preserve_identifiers`: If True, keep quotes in identifiers
- `strip_contexts`: If True, only include immediate context

**Returns:** `ComponentsResult` dict with tables, columns, wildcards, mutations

### `replace_names(sql, renames, dialect=None, case_insensitive=None, quotes_preserve_case=False, allow_unused=False)`

Rename schemas, tables, and columns in SQL.

**Parameters:**
- `sql`: Original SQL query
- `renames`: Dict with `schemas`, `tables`, `columns` mappings
- `dialect`: SQLGlot dialect for output
- `case_insensitive`: `"upper"`, `"lower"`, or `"agnostic"`
- `quotes_preserve_case`: Preserve case for quoted identifiers
- `allow_unused`: Don't error on unused renames

**Returns:** Rewritten SQL string

### `to_ast(sql, dialect=None, with_instance=False)`

Convert SQL to Python dict representation.

**Parameters:**
- `sql`: SQL query string
- `dialect`: SQLGlot dialect
- `with_instance`: Include SQLGlot expression objects

**Returns:** Nested dict representing the AST

## Convenience Functions

```python
from pymacaw.core import (
    query_to_tables,    # Extract just table identifiers
    query_to_columns,   # Extract just column identifiers
    has_wildcard,       # Check for SELECT *
    get_mutations,      # Get mutation commands
)
```

## Differences from Clojure Macaw

1. **Formatting**: Best-effort preservation using SQLGlot's transform API (some normalization may occur)
2. **Quote handling**: SQLGlot may normalize some quote styles
3. **Error format**: Python exceptions instead of error maps
4. **Type system**: Python TypedDict instead of Clojure maps with Malli

## Running Tests

```bash
cd python
pytest
```

## License

EPL-2.0 (same as Macaw)
