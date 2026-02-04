"""Type definitions for pymacaw."""

from enum import Enum
from typing import TypedDict, Optional, Set, FrozenSet, Any


class Context(str, Enum):
    """Syntactic contexts where SQL components appear."""

    SELECT = "SELECT"
    FROM = "FROM"
    WHERE = "WHERE"
    JOIN = "JOIN"
    GROUP_BY = "GROUP_BY"
    HAVING = "HAVING"
    ORDER_BY = "ORDER_BY"
    INSERT = "INSERT"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    SUB_SELECT = "SUB_SELECT"
    WITH_ITEM = "WITH_ITEM"
    IF = "IF"
    ELSE = "ELSE"


class MutationCommand(str, Enum):
    """Database-mutating operations."""

    INSERT = "insert"
    UPDATE = "update"
    DELETE = "delete"
    DROP = "drop"
    TRUNCATE = "truncate"
    PURGE = "purge"
    CREATE_TABLE = "create-table"
    CREATE_VIEW = "create-view"
    CREATE_INDEX = "create-index"
    CREATE_SCHEMA = "create-schema"
    CREATE_SEQUENCE = "create-sequence"
    CREATE_FUNCTION = "create-function"
    CREATE_SYNONYM = "create-synonym"
    ALTER_TABLE = "alter-table"
    ALTER_VIEW = "alter-view"
    ALTER_SEQUENCE = "alter-sequence"
    ALTER_SESSION = "alter-session"
    ALTER_SYSTEM = "alter-system"
    RENAME_TABLE = "rename-table"
    GRANT = "grant"


class CaseInsensitivity(str, Enum):
    """Case insensitivity modes for identifier matching."""

    UPPER = "upper"  # SQL-92 standard: identifiers implicitly uppercase
    LOWER = "lower"  # Postgres et al: identifiers implicitly lowercase
    AGNOSTIC = "agnostic"  # Case ignored when comparing


class TableIdent(TypedDict, total=False):
    """Table identifier with optional schema."""

    table: str  # Required
    schema: Optional[str]


class ColumnIdent(TypedDict, total=False):
    """Column identifier with optional qualifications."""

    column: str  # Required
    table: Optional[str]
    schema: Optional[str]
    alias: Optional[str]


class ComponentWithContext(TypedDict):
    """A component with its context stack."""

    component: Any  # TableIdent, ColumnIdent, str, or bool
    context: tuple[str, ...]  # Stack of Context values, innermost first


class ComponentsResult(TypedDict):
    """Result from query_to_components."""

    tables: FrozenSet[ComponentWithContext]
    columns: FrozenSet[ComponentWithContext]
    source_columns: FrozenSet[tuple]  # Deduplicated, context-free column tuples
    table_wildcards: FrozenSet[ComponentWithContext]
    has_wildcard: FrozenSet[ComponentWithContext]
    mutation_commands: FrozenSet[ComponentWithContext]


class TableRenameTarget(TypedDict, total=False):
    """Target for table rename - can specify new schema."""

    table: str  # Required
    schema: Optional[str]  # If present, changes/adds schema; if None, removes schema


class RenameSpec(TypedDict, total=False):
    """Specification for rename operations."""

    schemas: dict[str, Optional[str]]  # old_schema -> new_schema (None removes)
    tables: dict[tuple, str | TableRenameTarget]  # old -> new
    columns: dict[tuple, str]  # old -> new column name


class ParseError(TypedDict):
    """Error result from parsing."""

    error: str
    context: dict[str, Any]


# Type alias for results that may be errors
ComponentsOrError = ComponentsResult | ParseError
