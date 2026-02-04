"""Utility functions for pymacaw."""

from typing import Optional, Any
from pymacaw.types import CaseInsensitivity


def normalize_identifier(name: Optional[str], case_mode: Optional[CaseInsensitivity]) -> Optional[str]:
    """Normalize identifier for matching based on case sensitivity mode."""
    if name is None:
        return None
    if case_mode is None:
        return name
    if case_mode == CaseInsensitivity.LOWER:
        return name.lower()
    if case_mode == CaseInsensitivity.UPPER:
        return name.upper()
    # AGNOSTIC - return as-is, comparison handles it
    return name


def strip_quotes(name: str) -> str:
    """Remove quote characters from an identifier."""
    if not name or len(name) < 2:
        return name

    first, last = name[0], name[-1]
    if (first == '"' and last == '"') or (first == "'" and last == "'"):
        return name[1:-1]
    if first == "`" and last == "`":
        return name[1:-1]
    if first == "[" and last == "]":
        return name[1:-1]
    return name


def get_quote_style(name: str) -> Optional[str]:
    """Detect the quote style used for an identifier."""
    if not name or len(name) < 2:
        return None

    first, last = name[0], name[-1]
    if first == '"' and last == '"':
        return '"'
    if first == "`" and last == "`":
        return "`"
    if first == "[" and last == "]":
        return "["
    return None


def apply_quote_style(name: str, style: Optional[str]) -> str:
    """Apply a quote style to an identifier."""
    if style is None:
        return name
    if style == '"':
        return f'"{name}"'
    if style == "`":
        return f"`{name}`"
    if style == "[":
        return f"[{name}]"
    return name


def preserve_quotes(original: str, new_name: str) -> str:
    """Preserve the quoting style from original identifier to new name."""
    style = get_quote_style(original)
    if style:
        return apply_quote_style(new_name, style)
    return new_name


def ident_to_tuple(ident: dict) -> tuple:
    """Convert an identifier dict to a hashable tuple."""
    return tuple(sorted((k, v) for k, v in ident.items() if v is not None))


def tuple_to_ident(t: tuple) -> dict:
    """Convert a tuple back to an identifier dict."""
    return dict(t)


def ci_eq(a: Optional[str], b: Optional[str]) -> bool:
    """Case-insensitive equality comparison."""
    if a is None and b is None:
        return True
    if a is None or b is None:
        return False
    return a.lower() == b.lower()


def matches_table(
    resolved: dict,
    key: dict,
    case_mode: Optional[CaseInsensitivity],
    quotes_preserve_case: bool = False,
) -> bool:
    """
    Check if resolved table matches rename key.

    Matching priority:
    1. Exact match - key has explicit schema (including None) that matches
    2. Wildcard match - key has no schema key, matches any schema
    3. Fallback match - qualified key matches naked reference
    """
    resolved_table = resolved.get("table")
    resolved_schema = resolved.get("schema")
    key_table = key.get("table")
    key_schema = key.get("schema")
    has_schema_key = "schema" in key

    if case_mode == CaseInsensitivity.AGNOSTIC:
        table_matches = ci_eq(resolved_table, key_table)
    else:
        norm = lambda x: normalize_identifier(x, case_mode)
        table_matches = norm(resolved_table) == norm(key_table)

    if not table_matches:
        return False

    # Check schema matching
    if has_schema_key:
        # Key has explicit schema - must match exactly
        if case_mode == CaseInsensitivity.AGNOSTIC:
            return ci_eq(resolved_schema, key_schema)
        else:
            norm = lambda x: normalize_identifier(x, case_mode)
            return norm(resolved_schema) == norm(key_schema)
    else:
        # No schema key in key - wildcard match (matches any schema)
        return True


def matches_column(
    resolved: dict,
    key: dict,
    case_mode: Optional[CaseInsensitivity],
    quotes_preserve_case: bool = False,
) -> bool:
    """Check if resolved column matches rename key."""
    resolved_column = resolved.get("column")
    resolved_table = resolved.get("table")
    resolved_schema = resolved.get("schema")

    key_column = key.get("column")
    key_table = key.get("table")
    key_schema = key.get("schema")

    if case_mode == CaseInsensitivity.AGNOSTIC:
        eq = ci_eq
    else:
        norm = lambda x: normalize_identifier(x, case_mode)
        eq = lambda a, b: norm(a) == norm(b)

    # Column must match
    if not eq(resolved_column, key_column):
        return False

    # Table must match if specified in key
    if key_table is not None and not eq(resolved_table, key_table):
        return False

    # Schema must match if specified in key
    if key_schema is not None and not eq(resolved_schema, key_schema):
        return False

    return True


def find_table_rename(
    resolved: dict,
    renames: dict[tuple, Any],
    case_mode: Optional[CaseInsensitivity],
    quotes_preserve_case: bool = False,
) -> Optional[tuple]:
    """
    Find matching rename key for a table using priority matching.

    Priority:
    1. Exact match (explicit nil schema matches nil)
    2. Wildcard match (no schema key matches any)
    3. Fallback match (qualified key matches naked reference)
    """
    exact_match = None
    wildcard_match = None
    fallback_match = None

    for key_tuple in renames:
        key = tuple_to_ident(key_tuple)
        has_schema_key = "schema" in key

        if matches_table(resolved, key, case_mode, quotes_preserve_case):
            if has_schema_key:
                # Check if it's exact vs fallback
                key_schema = key.get("schema")
                resolved_schema = resolved.get("schema")

                if case_mode == CaseInsensitivity.AGNOSTIC:
                    schema_matches = ci_eq(resolved_schema, key_schema)
                else:
                    norm = lambda x: normalize_identifier(x, case_mode)
                    schema_matches = norm(resolved_schema) == norm(key_schema)

                if schema_matches:
                    exact_match = key_tuple
                else:
                    # Key has schema but resolved doesn't (or vice versa) - fallback
                    if fallback_match is None:
                        fallback_match = key_tuple
            else:
                # Wildcard - no schema key
                if wildcard_match is None:
                    wildcard_match = key_tuple

    # Return by priority
    if exact_match is not None:
        return exact_match
    if wildcard_match is not None:
        return wildcard_match
    return fallback_match


def find_column_rename(
    resolved: dict,
    renames: dict[tuple, str],
    case_mode: Optional[CaseInsensitivity],
    quotes_preserve_case: bool = False,
) -> Optional[tuple]:
    """Find matching rename key for a column."""
    for key_tuple in renames:
        key = tuple_to_ident(key_tuple)
        if matches_column(resolved, key, case_mode, quotes_preserve_case):
            return key_tuple
    return None
