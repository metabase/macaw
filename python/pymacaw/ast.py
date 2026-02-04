"""AST conversion to Python dictionaries."""

from typing import Any, Optional
from sqlglot import exp


def _get_join_type(join: exp.Join) -> dict[str, Any]:
    """Determine join type and properties."""
    result = {"type": "inner", "outer": False}

    side = join.args.get("side")
    kind = join.args.get("kind")

    if side:
        side_str = str(side).upper()
        if "LEFT" in side_str:
            result["type"] = "left"
            result["outer"] = True
        elif "RIGHT" in side_str:
            result["type"] = "right"
            result["outer"] = True
        elif "FULL" in side_str:
            result["type"] = "full"
            result["outer"] = True

    if kind:
        kind_str = str(kind).upper()
        if "CROSS" in kind_str:
            result["type"] = "cross"
        elif "NATURAL" in kind_str:
            result["type"] = "natural"
        elif "OUTER" in kind_str:
            result["outer"] = True

    return result


def convert_to_ast(node: Optional[exp.Expression], with_instance: bool = False) -> Optional[dict[str, Any]]:
    """
    Recursively convert SQLGlot expression to Python dict.

    This is intentionally lossy - designed for analysis, not round-tripping.
    """
    if node is None:
        return None

    result: dict[str, Any] = {"type": type(node).__name__.lower()}

    if with_instance:
        result["instance"] = node

    # Handle specific node types
    if isinstance(node, exp.Select):
        result["type"] = "select"
        result["select"] = [convert_to_ast(e, with_instance) for e in (node.expressions or [])]
        result["from"] = convert_to_ast(node.find(exp.From), with_instance)
        result["where"] = convert_to_ast(node.find(exp.Where), with_instance)

        joins = []
        for join in node.find_all(exp.Join):
            # Only include direct child joins
            if join.parent is node or (join.parent and join.parent.parent is node):
                joins.append(convert_to_ast(join, with_instance))
        if joins:
            result["joins"] = joins

        result["group-by"] = convert_to_ast(node.find(exp.Group), with_instance)
        result["having"] = convert_to_ast(node.find(exp.Having), with_instance)
        result["order-by"] = convert_to_ast(node.find(exp.Order), with_instance)

        # CTE / WITH clause
        with_clause = node.find(exp.With)
        if with_clause:
            result["with"] = convert_to_ast(with_clause, with_instance)

    elif isinstance(node, exp.From):
        result["type"] = "from"
        result["tables"] = [convert_to_ast(t, with_instance) for t in node.find_all(exp.Table)]

    elif isinstance(node, exp.Table):
        result["type"] = "table"
        result["table"] = node.name
        if node.db:
            result["schema"] = node.db
        if node.alias:
            result["table-alias"] = node.alias

    elif isinstance(node, exp.Column):
        result["type"] = "column"
        result["column"] = node.name
        if node.table:
            result["table"] = node.table

    elif isinstance(node, exp.Join):
        result["type"] = "join"
        join_info = _get_join_type(node)
        result["join-type"] = join_info["type"]
        result["outer?"] = join_info["outer"]
        result["source"] = convert_to_ast(node.this, with_instance)
        result["condition"] = convert_to_ast(node.args.get("on"), with_instance)

    elif isinstance(node, exp.Where):
        result["type"] = "where"
        result["condition"] = convert_to_ast(node.this, with_instance)

    elif isinstance(node, exp.Group):
        result["type"] = "group-by"
        result["expressions"] = [convert_to_ast(e, with_instance) for e in (node.expressions or [])]

    elif isinstance(node, exp.Having):
        result["type"] = "having"
        result["condition"] = convert_to_ast(node.this, with_instance)

    elif isinstance(node, exp.Order):
        result["type"] = "order-by"
        result["expressions"] = [convert_to_ast(e, with_instance) for e in (node.expressions or [])]

    elif isinstance(node, exp.Ordered):
        result["type"] = "ordered"
        result["expression"] = convert_to_ast(node.this, with_instance)
        result["desc"] = node.args.get("desc", False)

    elif isinstance(node, exp.Star):
        result["type"] = "wildcard"

    elif isinstance(node, exp.Literal):
        result["type"] = "literal"
        result["value"] = node.this
        if node.is_string:
            result["literal-type"] = "string"
        elif node.is_number:
            result["literal-type"] = "number"

    elif isinstance(node, exp.Binary):
        result["type"] = "binary-expression"
        result["operator"] = type(node).__name__.lower()
        result["left"] = convert_to_ast(node.left, with_instance)
        result["right"] = convert_to_ast(node.right, with_instance)

    elif isinstance(node, exp.Unary):
        result["type"] = "unary-expression"
        result["operator"] = type(node).__name__.lower()
        result["operand"] = convert_to_ast(node.this, with_instance)

    elif isinstance(node, exp.Func):
        result["type"] = "function"
        result["name"] = node.sql_name()
        # Handle function arguments carefully to avoid recursion issues
        func_args = []
        for key, arg in node.args.items():
            if arg is not None and isinstance(arg, exp.Expression):
                func_args.append(convert_to_ast(arg, with_instance))
            elif arg is not None and isinstance(arg, list):
                for item in arg:
                    if isinstance(item, exp.Expression):
                        func_args.append(convert_to_ast(item, with_instance))
        result["args"] = func_args

    elif isinstance(node, exp.Case):
        result["type"] = "case"
        ifs = []
        for if_expr in node.args.get("ifs") or []:
            ifs.append({
                "when": convert_to_ast(if_expr.args.get("this"), with_instance),
                "then": convert_to_ast(if_expr.args.get("true"), with_instance),
            })
        result["when-then"] = ifs
        result["else"] = convert_to_ast(node.args.get("default"), with_instance)

    elif isinstance(node, exp.Between):
        result["type"] = "between"
        result["expression"] = convert_to_ast(node.this, with_instance)
        result["low"] = convert_to_ast(node.args.get("low"), with_instance)
        result["high"] = convert_to_ast(node.args.get("high"), with_instance)

    elif isinstance(node, exp.In):
        result["type"] = "in"
        result["expression"] = convert_to_ast(node.this, with_instance)
        result["values"] = [convert_to_ast(v, with_instance) for v in node.expressions or []]

    elif isinstance(node, exp.Subquery):
        result["type"] = "subquery"
        result["query"] = convert_to_ast(node.this, with_instance)
        if node.alias:
            result["alias"] = node.alias

    elif isinstance(node, exp.Union):
        result["type"] = "set-operation"
        result["operation"] = "union"
        result["left"] = convert_to_ast(node.left, with_instance)
        result["right"] = convert_to_ast(node.right, with_instance)
        result["distinct"] = not node.args.get("distinct") == False

    elif isinstance(node, exp.Intersect):
        result["type"] = "set-operation"
        result["operation"] = "intersect"
        result["left"] = convert_to_ast(node.left, with_instance)
        result["right"] = convert_to_ast(node.right, with_instance)

    elif isinstance(node, exp.Except):
        result["type"] = "set-operation"
        result["operation"] = "except"
        result["left"] = convert_to_ast(node.left, with_instance)
        result["right"] = convert_to_ast(node.right, with_instance)

    elif isinstance(node, exp.With):
        result["type"] = "with"
        ctes = []
        for cte in node.expressions or []:
            ctes.append({
                "alias": cte.alias,
                "query": convert_to_ast(cte.this, with_instance),
            })
        result["ctes"] = ctes

    elif isinstance(node, exp.CTE):
        result["type"] = "cte"
        result["alias"] = node.alias
        result["query"] = convert_to_ast(node.this, with_instance)

    elif isinstance(node, exp.Alias):
        result["type"] = "alias"
        result["expression"] = convert_to_ast(node.this, with_instance)
        result["alias"] = node.alias

    elif isinstance(node, exp.Window):
        result["type"] = "analytic-expression"
        result["function"] = convert_to_ast(node.this, with_instance)
        result["partition-by"] = [convert_to_ast(p, with_instance) for p in node.args.get("partition_by") or []]
        result["order-by"] = [convert_to_ast(o, with_instance) for o in node.args.get("order") or []]

    elif isinstance(node, exp.Insert):
        result["type"] = "insert"
        result["table"] = convert_to_ast(node.find(exp.Table), with_instance)
        result["columns"] = [convert_to_ast(c, with_instance) for c in node.args.get("columns") or []]

    elif isinstance(node, exp.Update):
        result["type"] = "update"
        result["table"] = convert_to_ast(node.find(exp.Table), with_instance)
        result["set"] = [convert_to_ast(s, with_instance) for s in node.args.get("expressions") or []]
        result["where"] = convert_to_ast(node.find(exp.Where), with_instance)

    elif isinstance(node, exp.Delete):
        result["type"] = "delete"
        result["table"] = convert_to_ast(node.find(exp.Table), with_instance)
        result["where"] = convert_to_ast(node.find(exp.Where), with_instance)

    elif isinstance(node, exp.Create):
        result["type"] = "create"
        result["kind"] = node.args.get("kind")
        result["table"] = convert_to_ast(node.find(exp.Table), with_instance)

    elif isinstance(node, exp.Drop):
        result["type"] = "drop"
        result["kind"] = node.args.get("kind")
        result["table"] = convert_to_ast(node.find(exp.Table), with_instance)

    elif isinstance(node, exp.Paren):
        result["type"] = "expression-list"
        result["expression"] = convert_to_ast(node.this, with_instance)

    elif isinstance(node, exp.Tuple):
        result["type"] = "expression-list"
        result["expressions"] = [convert_to_ast(e, with_instance) for e in node.expressions or []]

    elif isinstance(node, exp.Interval):
        result["type"] = "interval"
        result["value"] = convert_to_ast(node.this, with_instance)
        result["unit"] = str(node.args.get("unit", ""))

    elif isinstance(node, exp.Placeholder):
        result["type"] = "jdbc-parameter"
        result["name"] = node.name

    elif isinstance(node, exp.Identifier):
        result["type"] = "identifier"
        result["name"] = node.name
        result["quoted"] = node.quoted

    else:
        # Generic fallback - include child nodes
        for key, value in node.args.items():
            if value is not None:
                if isinstance(value, exp.Expression):
                    result[key] = convert_to_ast(value, with_instance)
                elif isinstance(value, list):
                    result[key] = [
                        convert_to_ast(v, with_instance) if isinstance(v, exp.Expression) else v
                        for v in value
                    ]

    return result
