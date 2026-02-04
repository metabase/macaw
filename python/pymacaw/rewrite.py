"""Query rewriting with SQLGlot transforms."""

from typing import Optional, Any
from sqlglot import exp
from sqlglot.optimizer.scope import traverse_scope, Scope

from pymacaw.types import CaseInsensitivity, RenameSpec, TableRenameTarget
from pymacaw.util import (
    find_table_rename,
    find_column_rename,
    tuple_to_ident,
    ident_to_tuple,
    normalize_identifier,
    preserve_quotes,
    strip_quotes,
)


def _find_schema_rename(
    schema: str,
    schema_renames: dict[str, Optional[str]],
    case_mode: Optional[CaseInsensitivity],
) -> Optional[str]:
    """Find matching schema rename key with case sensitivity handling."""
    # Exact match first
    if schema in schema_renames:
        return schema

    if case_mode:
        # Case-insensitive matching
        for key in schema_renames:
            if case_mode == CaseInsensitivity.AGNOSTIC:
                if key.lower() == schema.lower():
                    return key
            elif case_mode == CaseInsensitivity.LOWER:
                if key.lower() == schema.lower():
                    return key
            elif case_mode == CaseInsensitivity.UPPER:
                if key.upper() == schema.upper():
                    return key

    return None


def _build_alias_map(scope: Scope) -> dict[str, exp.Table]:
    """Build mapping from aliases to their source tables."""
    alias_map = {}
    for name, source in scope.sources.items():
        if isinstance(source, exp.Table):
            alias_map[name] = source
    return alias_map


def _resolve_column_table(
    column: exp.Column, scope: Scope, case_mode: Optional[CaseInsensitivity]
) -> dict:
    """Resolve a column's table through aliases."""
    resolved = {"column": column.name}

    if column.table:
        source = scope.sources.get(column.table)
        if isinstance(source, exp.Table):
            resolved["table"] = source.name
            if source.db:
                resolved["schema"] = source.db
        else:
            resolved["table"] = column.table
    elif len(scope.sources) == 1:
        # Single table inference
        source_name, source = next(iter(scope.sources.items()))
        if isinstance(source, exp.Table):
            resolved["table"] = source.name
            if source.db:
                resolved["schema"] = source.db

    return resolved


def _get_new_table_name(
    rename_value: str | TableRenameTarget | dict,
) -> tuple[str, Optional[str], bool]:
    """
    Extract new table name and schema from rename value.

    Returns (new_table, new_schema, has_schema_key)
    where has_schema_key indicates if schema should be modified.
    """
    if isinstance(rename_value, str):
        return (rename_value, None, False)
    elif isinstance(rename_value, dict):
        new_table = rename_value.get("table", rename_value.get("table"))
        if "schema" in rename_value:
            return (new_table, rename_value["schema"], True)
        else:
            return (new_table, None, False)
    return (str(rename_value), None, False)


def apply_renames(
    sql: str,
    parsed: exp.Expression,
    renames: RenameSpec,
    dialect: Optional[str] = None,
    case_insensitive: Optional[CaseInsensitivity] = None,
    quotes_preserve_case: bool = False,
    allow_unused: bool = False,
) -> str:
    """
    Apply renames while preserving formatting as much as possible.

    Uses SQLGlot's transform API for best-effort formatting preservation.
    """
    schema_renames = renames.get("schemas", {})
    table_renames = renames.get("tables", {})
    column_renames = renames.get("columns", {})

    used_renames: set[tuple[str, Any]] = set()

    # Build scope information for alias resolution
    scope_map: dict[int, Scope] = {}
    try:
        for scope in traverse_scope(parsed):
            # Map expression ids to scopes
            scope_map[id(scope.expression)] = scope
    except Exception:
        pass

    def find_scope_for_expr(expr: exp.Expression) -> Optional[Scope]:
        """Find the scope containing this expression."""
        current = expr
        while current is not None:
            if id(current) in scope_map:
                return scope_map[id(current)]
            current = current.parent
        return None

    def transform_table(node: exp.Expression) -> exp.Expression:
        """Transform table references."""
        if not isinstance(node, exp.Table):
            return node

        resolved = {
            "table": node.name,
        }
        if node.db:
            resolved["schema"] = node.db

        # Try to find a matching table rename
        match = find_table_rename(
            resolved, table_renames, case_insensitive, quotes_preserve_case
        )

        if match:
            used_renames.add(("table", match))
            rename_value = table_renames[match]
            new_table, new_schema, has_schema_key = _get_new_table_name(rename_value)

            # Preserve quote style
            new_table_quoted = preserve_quotes(node.name, new_table)

            # Determine new schema
            if has_schema_key:
                if new_schema is None:
                    # Remove schema
                    return exp.Table(
                        this=exp.Identifier(this=new_table_quoted, quoted=node.this.quoted if isinstance(node.this, exp.Identifier) else False),
                        alias=node.alias,
                    )
                else:
                    # Change/add schema
                    new_schema_quoted = preserve_quotes(node.db or "", new_schema) if node.db else new_schema
                    return exp.Table(
                        this=exp.Identifier(this=new_table_quoted, quoted=node.this.quoted if isinstance(node.this, exp.Identifier) else False),
                        db=exp.Identifier(this=new_schema_quoted, quoted=node.args.get("db").quoted if node.args.get("db") and isinstance(node.args.get("db"), exp.Identifier) else False) if new_schema_quoted else None,
                        alias=node.alias,
                    )
            else:
                # Keep original schema, just change table name
                # Also check for schema renames
                current_schema = node.db
                if current_schema and current_schema in schema_renames:
                    used_renames.add(("schema", current_schema))
                    new_schema_name = schema_renames[current_schema]
                    if new_schema_name is None:
                        # Remove schema
                        return exp.Table(
                            this=exp.Identifier(this=new_table_quoted, quoted=node.this.quoted if isinstance(node.this, exp.Identifier) else False),
                            alias=node.alias,
                        )
                    else:
                        new_schema_quoted = preserve_quotes(current_schema, new_schema_name)
                        return exp.Table(
                            this=exp.Identifier(this=new_table_quoted, quoted=node.this.quoted if isinstance(node.this, exp.Identifier) else False),
                            db=exp.Identifier(this=new_schema_quoted, quoted=node.args.get("db").quoted if node.args.get("db") and isinstance(node.args.get("db"), exp.Identifier) else False),
                            alias=node.alias,
                        )
                else:
                    # Just update table name, keep original schema
                    return exp.Table(
                        this=exp.Identifier(this=new_table_quoted, quoted=node.this.quoted if isinstance(node.this, exp.Identifier) else False),
                        db=node.args.get("db"),
                        alias=node.alias,
                    )

        # Check for schema-only renames
        schema_key = _find_schema_rename(node.db, schema_renames, case_insensitive) if node.db else None
        if schema_key:
            used_renames.add(("schema", schema_key))
            new_schema_name = schema_renames[schema_key]
            if new_schema_name is None:
                # Remove schema
                new_node = node.copy()
                new_node.set("db", None)
                return new_node
            else:
                new_schema_quoted = preserve_quotes(node.db, new_schema_name)
                new_node = node.copy()
                new_node.set("db", exp.Identifier(this=new_schema_quoted, quoted=node.args.get("db").quoted if node.args.get("db") and isinstance(node.args.get("db"), exp.Identifier) else False))
                return new_node

        return node

    def transform_column(node: exp.Expression) -> exp.Expression:
        """Transform column references."""
        if not isinstance(node, exp.Column):
            return node

        # Find scope for alias resolution
        scope = find_scope_for_expr(node)

        if scope:
            resolved = _resolve_column_table(node, scope, case_insensitive)
        else:
            resolved = {"column": node.name}
            if node.table:
                resolved["table"] = node.table

        # Try to find a matching column rename
        match = find_column_rename(
            resolved, column_renames, case_insensitive, quotes_preserve_case
        )

        if match:
            used_renames.add(("column", match))
            new_column = column_renames[match]

            # Preserve quote style
            new_column_quoted = preserve_quotes(node.name, new_column)

            new_node = node.copy()
            new_node.set("this", exp.Identifier(this=new_column_quoted, quoted=node.this.quoted if isinstance(node.this, exp.Identifier) else False))

            # Also update table reference if it was renamed
            if node.table:
                table_resolved = {"table": node.table}
                if scope:
                    source = scope.sources.get(node.table)
                    if isinstance(source, exp.Table):
                        if source.db:
                            table_resolved["schema"] = source.db

                table_match = find_table_rename(
                    table_resolved, table_renames, case_insensitive, quotes_preserve_case
                )
                if table_match:
                    rename_value = table_renames[table_match]
                    new_table, _, _ = _get_new_table_name(rename_value)
                    new_table_quoted = preserve_quotes(node.table, new_table)
                    new_node.set("table", exp.Identifier(this=new_table_quoted, quoted=node.args.get("table").quoted if node.args.get("table") and isinstance(node.args.get("table"), exp.Identifier) else False))

            return new_node

        # Check if table qualifier needs updating due to table rename
        if node.table:
            table_resolved = {"table": node.table}
            if scope:
                source = scope.sources.get(node.table)
                if isinstance(source, exp.Table):
                    table_resolved["table"] = source.name
                    if source.db:
                        table_resolved["schema"] = source.db

            table_match = find_table_rename(
                table_resolved, table_renames, case_insensitive, quotes_preserve_case
            )
            if table_match:
                rename_value = table_renames[table_match]
                new_table, _, _ = _get_new_table_name(rename_value)
                new_table_quoted = preserve_quotes(node.table, new_table)
                new_node = node.copy()
                new_node.set("table", exp.Identifier(this=new_table_quoted, quoted=node.args.get("table").quoted if node.args.get("table") and isinstance(node.args.get("table"), exp.Identifier) else False))
                return new_node

        return node

    # Apply transforms
    transformed = parsed.copy()
    transformed = transformed.transform(transform_table)
    transformed = transformed.transform(transform_column)

    # Validate all renames were used
    if not allow_unused:
        for key_tuple in table_renames:
            if ("table", key_tuple) not in used_renames:
                key = tuple_to_ident(key_tuple)
                raise ValueError(f"Unknown rename: table {key}")

        for key_tuple in column_renames:
            if ("column", key_tuple) not in used_renames:
                key = tuple_to_ident(key_tuple)
                raise ValueError(f"Unknown rename: column {key}")

        for schema in schema_renames:
            if ("schema", schema) not in used_renames:
                raise ValueError(f"Unknown rename: schema {schema}")

    # Generate SQL
    return transformed.sql(dialect=dialect)
