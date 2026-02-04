"""
pymacaw - Python port of Macaw SQL analysis library using SQLGlot.

Main entry points:
- query_to_components: Extract tables, columns, wildcards, and mutations from SQL
- replace_names: Rename schemas, tables, and columns in SQL
- to_ast: Convert SQL to Python dict representation
"""

from pymacaw.core import query_to_components, replace_names, to_ast
from pymacaw.types import (
    Context,
    MutationCommand,
    CaseInsensitivity,
    TableIdent,
    ColumnIdent,
    ComponentWithContext,
    ComponentsResult,
    RenameSpec,
)

__all__ = [
    # Core functions
    "query_to_components",
    "replace_names",
    "to_ast",
    # Types
    "Context",
    "MutationCommand",
    "CaseInsensitivity",
    "TableIdent",
    "ColumnIdent",
    "ComponentWithContext",
    "ComponentsResult",
    "RenameSpec",
]

__version__ = "0.1.0"
