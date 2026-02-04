"""Core public API for pymacaw."""

from typing import Optional, Any
import sqlglot
from sqlglot import exp

from pymacaw.types import (
    ComponentsResult,
    RenameSpec,
    CaseInsensitivity,
    ParseError,
    ComponentsOrError,
)
from pymacaw.components import extract_components
from pymacaw.rewrite import apply_renames
from pymacaw.ast import convert_to_ast
from pymacaw.util import ident_to_tuple


def query_to_components(
    sql: str,
    dialect: Optional[str] = None,
    preserve_identifiers: bool = True,
    strip_contexts: bool = False,
) -> ComponentsOrError:
    """
    Extract tables, columns, wildcards, and mutations from a SQL query.

    Args:
        sql: SQL query string
        dialect: SQLGlot dialect (e.g., "postgres", "mysql", "snowflake")
        preserve_identifiers: If True, preserve identifiers verbatim including quotes.
                             If False, strip quotes for normalized output.
        strip_contexts: If True, only include the immediate context (not full stack)

    Returns:
        ComponentsResult with:
        - tables: Set of (component, context) tuples for table references
        - columns: Set of (component, context) tuples for column references
        - source_columns: Set of deduplicated column identifiers (no context)
        - table_wildcards: Set of (component, context) for t.* patterns
        - has_wildcard: Set of (True, context) for SELECT * presence
        - mutation_commands: Set of (command, context) for DDL/DML operations

    Example:
        >>> result = query_to_components("SELECT o.id FROM orders o")
        >>> # Tables: {(('table', 'orders'),), ('FROM', 'SELECT'))}
        >>> # Columns: {(('column', 'id'), ('table', 'orders')), ('SELECT',))}
    """
    try:
        parsed = sqlglot.parse_one(sql, dialect=dialect)
    except sqlglot.errors.ParseError as e:
        return ParseError(error="unable_to_parse", context={"cause": str(e)})

    return extract_components(
        parsed,
        preserve_identifiers=preserve_identifiers,
        strip_contexts=strip_contexts,
    )


def replace_names(
    sql: str,
    renames: dict[str, Any],
    dialect: Optional[str] = None,
    case_insensitive: Optional[str | CaseInsensitivity] = None,
    quotes_preserve_case: bool = False,
    allow_unused: bool = False,
) -> str:
    """
    Rename schemas, tables, and columns in a SQL query.

    Args:
        sql: Original SQL query
        renames: Dict with 'schemas', 'tables', and 'columns' rename mappings.
                 - schemas: {old_schema: new_schema} (new_schema=None removes schema)
                 - tables: {table_ident: new_name_or_ident}
                 - columns: {column_ident: new_name}

                 Identifiers can be dicts like {"table": "foo"} or {"schema": "s", "table": "t"}

        dialect: SQLGlot dialect for output
        case_insensitive: How to handle case sensitivity:
            - "upper": identifiers implicitly uppercase (SQL-92)
            - "lower": identifiers implicitly lowercase (Postgres)
            - "agnostic": case ignored when comparing
        quotes_preserve_case: If True, quoted identifiers preserve case
        allow_unused: If True, don't raise error for unused renames

    Returns:
        SQL string with renames applied

    Raises:
        ValueError: If SQL cannot be parsed or renames are unused (unless allow_unused)

    Example:
        >>> replace_names(
        ...     "SELECT a.x FROM a",
        ...     {
        ...         'tables': {('table', 'a'): 'b'},
        ...         'columns': {('column', 'x'), ('table', 'a'): 'y'}
        ...     }
        ... )
        'SELECT b.y FROM b'
    """
    try:
        parsed = sqlglot.parse_one(sql, dialect=dialect)
    except sqlglot.errors.ParseError as e:
        raise ValueError(f"Unable to parse: {e}") from e

    # Convert case_insensitive string to enum
    case_mode = None
    if case_insensitive:
        if isinstance(case_insensitive, str):
            case_mode = CaseInsensitivity(case_insensitive)
        else:
            case_mode = case_insensitive

    # Normalize rename keys to tuples
    normalized_renames: RenameSpec = {}

    if "schemas" in renames:
        normalized_renames["schemas"] = renames["schemas"]

    if "tables" in renames:
        normalized_tables = {}
        for key, value in renames["tables"].items():
            if isinstance(key, dict):
                key_tuple = ident_to_tuple(key)
            else:
                key_tuple = key
            normalized_tables[key_tuple] = value
        normalized_renames["tables"] = normalized_tables

    if "columns" in renames:
        normalized_columns = {}
        for key, value in renames["columns"].items():
            if isinstance(key, dict):
                key_tuple = ident_to_tuple(key)
            else:
                key_tuple = key
            normalized_columns[key_tuple] = value
        normalized_renames["columns"] = normalized_columns

    return apply_renames(
        sql,
        parsed,
        normalized_renames,
        dialect=dialect,
        case_insensitive=case_mode,
        quotes_preserve_case=quotes_preserve_case,
        allow_unused=allow_unused,
    )


def to_ast(
    sql: str,
    dialect: Optional[str] = None,
    with_instance: bool = False,
) -> dict[str, Any]:
    """
    Convert SQL to a Python dictionary representation.

    This is intentionally lossy - designed for analysis, not round-tripping.

    Args:
        sql: SQL query string
        dialect: SQLGlot dialect
        with_instance: If True, include SQLGlot expression objects in output

    Returns:
        Dictionary representation of the AST with keys like:
        - type: Node type (e.g., "select", "table", "column")
        - Additional keys depending on node type

    Example:
        >>> ast = to_ast("SELECT id FROM users")
        >>> ast['type']
        'select'
        >>> ast['from']['tables'][0]['table']
        'users'
    """
    parsed = sqlglot.parse_one(sql, dialect=dialect)
    return convert_to_ast(parsed, with_instance=with_instance)


# Convenience functions for common operations


def query_to_tables(
    sql: str,
    dialect: Optional[str] = None,
    preserve_identifiers: bool = True,
) -> list[dict]:
    """
    Extract just the table identifiers from a SQL query.

    Returns a list of unique table identifier dicts like {"table": "users", "schema": "public"}.
    """
    result = query_to_components(sql, dialect, preserve_identifiers)
    if "error" in result:
        return []

    # Use frozenset for deduplication, then convert back to list of dicts
    seen = set()
    tables = []
    for table_tuple in result["tables"]:
        comp_tuple, _ = table_tuple
        if comp_tuple not in seen:
            seen.add(comp_tuple)
            tables.append(dict(comp_tuple))

    return tables


def query_to_columns(
    sql: str,
    dialect: Optional[str] = None,
    preserve_identifiers: bool = True,
) -> list[dict]:
    """
    Extract just the column identifiers from a SQL query.

    Returns a list of unique column identifier dicts like {"column": "id", "table": "users"}.
    """
    result = query_to_components(sql, dialect, preserve_identifiers)
    if "error" in result:
        return []

    # Use frozenset for deduplication, then convert back to list of dicts
    seen = set()
    columns = []
    for col_tuple in result["source_columns"]:
        if col_tuple not in seen:
            seen.add(col_tuple)
            columns.append(dict(col_tuple))

    return columns


def has_wildcard(
    sql: str,
    dialect: Optional[str] = None,
) -> bool:
    """Check if the query contains SELECT *."""
    result = query_to_components(sql, dialect)
    if "error" in result:
        return False

    return len(result["has_wildcard"]) > 0


def get_mutations(
    sql: str,
    dialect: Optional[str] = None,
) -> set[str]:
    """Get the set of mutation commands in the query."""
    result = query_to_components(sql, dialect)
    if "error" in result:
        return set()

    return {cmd for cmd, _ in result["mutation_commands"]}
