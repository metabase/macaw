"""Component extraction from SQL queries using SQLGlot scope analysis."""

from typing import Optional
from sqlglot import exp
from sqlglot.optimizer.scope import traverse_scope, Scope, ScopeType

from pymacaw.types import (
    Context,
    MutationCommand,
    ComponentsResult,
    TableIdent,
    ColumnIdent,
)
from pymacaw.util import ident_to_tuple, strip_quotes


def _scope_type_to_context(scope: Scope) -> list[str]:
    """Convert SQLGlot scope type to Macaw context."""
    if scope.scope_type == ScopeType.ROOT:
        expr = scope.expression
        if isinstance(expr, exp.Select):
            return [Context.SELECT.value]
        elif isinstance(expr, exp.Insert):
            return [Context.INSERT.value]
        elif isinstance(expr, exp.Update):
            return [Context.UPDATE.value]
        elif isinstance(expr, exp.Delete):
            return [Context.DELETE.value]
        return [Context.SELECT.value]  # Default
    elif scope.scope_type == ScopeType.SUBQUERY:
        return [Context.SUB_SELECT.value]
    elif scope.scope_type == ScopeType.CTE:
        return [Context.WITH_ITEM.value]
    elif scope.scope_type == ScopeType.DERIVED_TABLE:
        return [Context.SUB_SELECT.value]
    return []


def _get_clause_context(expr: exp.Expression, parent: exp.Expression) -> Optional[str]:
    """Determine what clause an expression is in based on its parent."""
    if isinstance(parent, exp.Select):
        # Check which part of the select this expression is in
        if expr in (parent.expressions or []):
            return Context.SELECT.value
        if parent.args.get("from") and _is_descendant_of(expr, parent.args["from"]):
            return Context.FROM.value
        if parent.args.get("where") and _is_descendant_of(expr, parent.args["where"]):
            return Context.WHERE.value
        if parent.args.get("group") and _is_descendant_of(expr, parent.args["group"]):
            return Context.GROUP_BY.value
        if parent.args.get("having") and _is_descendant_of(expr, parent.args["having"]):
            return Context.HAVING.value
        if parent.args.get("order") and _is_descendant_of(expr, parent.args["order"]):
            return Context.ORDER_BY.value
        # Check joins
        for join in parent.args.get("joins") or []:
            if _is_descendant_of(expr, join):
                return Context.JOIN.value
    elif isinstance(parent, exp.Join):
        return Context.JOIN.value
    elif isinstance(parent, exp.Where):
        return Context.WHERE.value
    elif isinstance(parent, exp.Group):
        return Context.GROUP_BY.value
    elif isinstance(parent, exp.Having):
        return Context.HAVING.value
    elif isinstance(parent, exp.Order):
        return Context.ORDER_BY.value
    elif isinstance(parent, exp.From):
        return Context.FROM.value

    return None


def _is_descendant_of(expr: exp.Expression, ancestor: exp.Expression) -> bool:
    """Check if expr is a descendant of ancestor."""
    current = expr
    while current is not None:
        if current is ancestor:
            return True
        current = current.parent
    return False


def _build_context_stack(scope: Scope, expr: exp.Expression) -> tuple[str, ...]:
    """Build context stack by walking up from expression."""
    context = []

    # Walk up from expression to scope root
    current = expr
    visited = set()

    while current is not None and current is not scope.expression:
        if id(current) in visited:
            break
        visited.add(id(current))

        parent = current.parent
        if parent:
            clause = _get_clause_context(current, parent)
            if clause and (not context or context[-1] != clause):
                context.append(clause)
        current = parent

    # Add scope-level context
    scope_context = _scope_type_to_context(scope)
    context.extend(scope_context)

    # Walk up parent scopes
    parent_scope = scope.parent
    while parent_scope is not None:
        scope_context = _scope_type_to_context(parent_scope)
        context.extend(scope_context)
        parent_scope = parent_scope.parent

    return tuple(context)


def _extract_table_ident(table: exp.Table, preserve_identifiers: bool) -> TableIdent:
    """Extract TableIdent from SQLGlot Table expression."""
    name = table.name
    schema = table.db

    if not preserve_identifiers:
        name = strip_quotes(name)
        if schema:
            schema = strip_quotes(schema)

    result: TableIdent = {"table": name}
    if schema:
        result["schema"] = schema
    return result


def _resolve_table_from_alias(
    alias: str, scope: Scope, preserve_identifiers: bool
) -> Optional[TableIdent]:
    """Resolve a table alias to its actual table."""
    source = scope.sources.get(alias)
    if isinstance(source, exp.Table):
        return _extract_table_ident(source, preserve_identifiers)
    elif isinstance(source, Scope):
        # Subquery or CTE - no real table
        return None
    return None


def _extract_column_ident(
    column: exp.Column, scope: Scope, preserve_identifiers: bool
) -> ColumnIdent:
    """Extract ColumnIdent, resolving aliases to real tables."""
    col_name = column.name
    table_name = column.table

    # Check for schema in column parts (e.g., public.towns.id)
    # SQLGlot stores this in column.parts when fully qualified
    column_schema = None
    if hasattr(column, 'parts') and len(column.parts) >= 3:
        # parts[0] is schema, parts[1] is table, parts[2] is column
        column_schema = column.parts[0].name if hasattr(column.parts[0], 'name') else str(column.parts[0])
        table_name = column.parts[1].name if hasattr(column.parts[1], 'name') else str(column.parts[1])

    if not preserve_identifiers:
        col_name = strip_quotes(col_name)
        if table_name:
            table_name = strip_quotes(table_name)
        if column_schema:
            column_schema = strip_quotes(column_schema)

    result: ColumnIdent = {"column": col_name}

    if column_schema:
        # Fully qualified column - use schema directly
        result["schema"] = column_schema
        result["table"] = table_name
    elif table_name:
        # Check if it's an alias
        source = scope.sources.get(table_name)
        if isinstance(source, exp.Table):
            # Real table via alias
            if preserve_identifiers:
                result["table"] = source.name
                if source.db:
                    result["schema"] = source.db
            else:
                result["table"] = strip_quotes(source.name)
                if source.db:
                    result["schema"] = strip_quotes(source.db)
        elif isinstance(source, Scope):
            # Subquery/CTE - use the alias
            result["table"] = table_name
        else:
            # Not found in sources - use as-is
            result["table"] = table_name
    elif len(scope.sources) == 1:
        # Single table inference
        source_name, source = next(iter(scope.sources.items()))
        if isinstance(source, exp.Table):
            if preserve_identifiers:
                result["table"] = source.name
                if source.db:
                    result["schema"] = source.db
            else:
                result["table"] = strip_quotes(source.name)
                if source.db:
                    result["schema"] = strip_quotes(source.db)

    return result


def _detect_mutations(parsed: exp.Expression) -> list[str]:
    """Detect mutation commands in the query."""
    mutations = []

    if isinstance(parsed, exp.Insert):
        mutations.append(MutationCommand.INSERT.value)
    elif isinstance(parsed, exp.Update):
        mutations.append(MutationCommand.UPDATE.value)
    elif isinstance(parsed, exp.Delete):
        mutations.append(MutationCommand.DELETE.value)
    elif isinstance(parsed, exp.Drop):
        mutations.append(MutationCommand.DROP.value)
    elif isinstance(parsed, exp.Create):
        kind = parsed.args.get("kind")
        if kind == "TABLE":
            mutations.append(MutationCommand.CREATE_TABLE.value)
        elif kind == "VIEW":
            mutations.append(MutationCommand.CREATE_VIEW.value)
        elif kind == "INDEX":
            mutations.append(MutationCommand.CREATE_INDEX.value)
        elif kind == "SCHEMA":
            mutations.append(MutationCommand.CREATE_SCHEMA.value)
        elif kind == "SEQUENCE":
            mutations.append(MutationCommand.CREATE_SEQUENCE.value)
        elif kind == "FUNCTION":
            mutations.append(MutationCommand.CREATE_FUNCTION.value)
        else:
            # Generic create
            mutations.append(f"create-{(kind or 'unknown').lower()}")
    elif isinstance(parsed, exp.Alter):
        # Try to determine what kind of alter
        mutations.append(MutationCommand.ALTER_TABLE.value)
    elif isinstance(parsed, exp.Command):
        # Handle special commands like TRUNCATE, PURGE, etc.
        cmd = parsed.this
        if isinstance(cmd, str):
            cmd_lower = cmd.lower()
            if "truncate" in cmd_lower:
                mutations.append(MutationCommand.TRUNCATE.value)
            elif "purge" in cmd_lower:
                mutations.append(MutationCommand.PURGE.value)
            elif "grant" in cmd_lower:
                mutations.append(MutationCommand.GRANT.value)
            elif "rename" in cmd_lower:
                mutations.append(MutationCommand.RENAME_TABLE.value)

    # Check for additional mutation types via isinstance
    for node in parsed.walk():
        if isinstance(node, exp.AlterColumn):
            if MutationCommand.ALTER_TABLE.value not in mutations:
                mutations.append(MutationCommand.ALTER_TABLE.value)

    return mutations


def _freeze_component(component: dict, context: tuple) -> tuple:
    """Convert component to a hashable frozen structure."""
    return (ident_to_tuple(component), context)


def _infer_schemas(
    tables: set[tuple], source_columns: set[tuple]
) -> set[tuple]:
    """Infer schema for tables based on qualified column references."""
    # Build schema map from columns that have schemas
    schema_map = {}
    for col_tuple in source_columns:
        col = dict(col_tuple)
        if col.get("schema") and col.get("table"):
            schema_map[col["table"]] = col["schema"]

    # Update tables that lack schema but have one available
    result = set()
    for table_tuple in tables:
        comp_tuple, context = table_tuple
        table_dict = dict(comp_tuple)

        if not table_dict.get("schema") and table_dict.get("table") in schema_map:
            table_dict["schema"] = schema_map[table_dict["table"]]

        result.add((ident_to_tuple(table_dict), context))

    return result


def extract_components(
    parsed: exp.Expression,
    preserve_identifiers: bool = True,
    strip_contexts: bool = False,
) -> ComponentsResult:
    """
    Extract all components from parsed SQL using scope analysis.

    Uses SQLGlot's traverse_scope for proper alias resolution and context tracking.
    """
    tables: set[tuple] = set()
    columns: set[tuple] = set()
    source_columns: set[tuple] = set()
    table_wildcards: set[tuple] = set()
    has_wildcard: set[tuple] = set()
    mutation_commands: set[tuple] = set()

    # Track CTE names to filter from table results
    cte_names: set[str] = set()

    # Detect mutation commands at top level
    mutations = _detect_mutations(parsed)
    for cmd in mutations:
        mutation_commands.add((cmd, ()))

    # Traverse all scopes
    try:
        scopes = list(traverse_scope(parsed))
    except Exception:
        # If scope traversal fails, fall back to basic extraction
        scopes = []

    for scope in scopes:
        # Collect CTE names to filter later
        for cte in scope.cte_sources:
            cte_names.add(cte)

        # Extract tables (excluding CTEs)
        for source_name, source in scope.sources.items():
            if isinstance(source, exp.Table):
                if source.name not in cte_names:
                    ident = _extract_table_ident(source, preserve_identifiers)
                    if strip_contexts:
                        context = (_get_clause_context(source, source.parent) or Context.FROM.value,)
                    else:
                        context = _build_context_stack(scope, source)
                    tables.add(_freeze_component(ident, context))

        # Extract columns
        for column in scope.columns:
            # Skip COUNT(*) - it doesn't really read columns
            parent = column.parent
            if isinstance(parent, exp.Star):
                continue

            ident = _extract_column_ident(column, scope, preserve_identifiers)
            if strip_contexts:
                context = (_get_clause_context(column, column.parent) or Context.SELECT.value,)
            else:
                context = _build_context_stack(scope, column)
            columns.add(_freeze_component(ident, context))

            # Add to source_columns (de-aliased, no context)
            if ident.get("table") and ident["table"] not in cte_names:
                source_columns.add(ident_to_tuple(ident))

        # Detect wildcards in SELECT
        if isinstance(scope.expression, exp.Select):
            for select_item in scope.expression.expressions or []:
                if isinstance(select_item, exp.Star):
                    # Check if it's inside a COUNT - if so, skip
                    parent = select_item.parent
                    while parent and not isinstance(parent, exp.Select):
                        if isinstance(parent, exp.Count):
                            break
                        parent = parent.parent
                    else:
                        if strip_contexts:
                            context = (Context.SELECT.value,)
                        else:
                            context = _build_context_stack(scope, select_item)
                        has_wildcard.add((True, context))
                elif isinstance(select_item, exp.Column) and select_item.name == "*":
                    # Table wildcard like t.*
                    table_alias = select_item.table
                    if table_alias:
                        resolved = _resolve_table_from_alias(
                            table_alias, scope, preserve_identifiers
                        )
                        if resolved:
                            if strip_contexts:
                                context = (Context.SELECT.value,)
                            else:
                                context = _build_context_stack(scope, select_item)
                            table_wildcards.add(_freeze_component(resolved, context))

    # If no scopes found, do basic extraction
    if not scopes:
        for table in parsed.find_all(exp.Table):
            if table.name and table.name not in cte_names:
                ident = _extract_table_ident(table, preserve_identifiers)
                tables.add(_freeze_component(ident, (Context.FROM.value,)))

        for column in parsed.find_all(exp.Column):
            ident: ColumnIdent = {"column": column.name}
            if column.table:
                ident["table"] = column.table
            columns.add(_freeze_component(ident, (Context.SELECT.value,)))
            if ident.get("table"):
                source_columns.add(ident_to_tuple(ident))

        for star in parsed.find_all(exp.Star):
            # Check if inside COUNT
            parent = star.parent
            in_count = False
            while parent:
                if isinstance(parent, exp.Count):
                    in_count = True
                    break
                parent = parent.parent
            if not in_count:
                has_wildcard.add((True, (Context.SELECT.value,)))

    # Schema inference
    tables = _infer_schemas(tables, source_columns)

    return ComponentsResult(
        tables=frozenset(tables),
        columns=frozenset(columns),
        source_columns=frozenset(source_columns),
        table_wildcards=frozenset(table_wildcards),
        has_wildcard=frozenset(has_wildcard),
        mutation_commands=frozenset(mutation_commands),
    )
