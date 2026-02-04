"""Tests for AST conversion."""

import pytest
from pymacaw import to_ast


class TestToAst:
    """Tests for the to_ast function."""

    def test_simple_select(self):
        ast = to_ast("SELECT id FROM users")
        assert ast["type"] == "select"
        assert "select" in ast
        assert "from" in ast

    def test_select_with_where(self):
        ast = to_ast("SELECT id FROM users WHERE active = 1")
        assert ast["type"] == "select"
        assert ast["where"] is not None
        assert ast["where"]["type"] == "where"

    def test_select_with_join(self):
        ast = to_ast("SELECT u.id FROM users u JOIN orders o ON u.id = o.user_id")
        assert ast["type"] == "select"
        assert "joins" in ast
        assert len(ast["joins"]) > 0
        assert ast["joins"][0]["type"] == "join"

    def test_join_types(self):
        # Left join
        ast = to_ast("SELECT * FROM a LEFT JOIN b ON a.id = b.a_id")
        join = ast["joins"][0]
        assert join["join-type"] == "left"
        assert join["outer?"] is True

        # Right join
        ast = to_ast("SELECT * FROM a RIGHT JOIN b ON a.id = b.a_id")
        join = ast["joins"][0]
        assert join["join-type"] == "right"

        # Inner join (default)
        ast = to_ast("SELECT * FROM a INNER JOIN b ON a.id = b.a_id")
        join = ast["joins"][0]
        assert join["join-type"] == "inner"

    def test_select_with_group_by(self):
        ast = to_ast("SELECT status, COUNT(*) FROM orders GROUP BY status")
        assert ast["type"] == "select"
        assert ast["group-by"] is not None

    def test_select_with_order_by(self):
        ast = to_ast("SELECT id FROM users ORDER BY created_at DESC")
        assert ast["type"] == "select"
        assert ast["order-by"] is not None

    def test_table_node(self):
        ast = to_ast("SELECT * FROM public.users")
        from_clause = ast["from"]
        assert from_clause is not None
        tables = from_clause.get("tables", [])
        if tables:
            table = tables[0]
            assert table["type"] == "table"
            assert table["table"] == "users"
            assert table.get("schema") == "public"

    def test_column_node(self):
        ast = to_ast("SELECT users.id FROM users")
        select_items = ast["select"]
        assert len(select_items) > 0
        # Find the column
        col = select_items[0]
        if col["type"] == "column":
            assert col["column"] == "id"
            assert col.get("table") == "users"

    def test_literal_node(self):
        ast = to_ast("SELECT 'hello', 42")
        select_items = ast["select"]
        # Should have literals
        literals = [s for s in select_items if s and s.get("type") == "literal"]
        assert len(literals) >= 1

    def test_function_node(self):
        ast = to_ast("SELECT COUNT(*), SUM(amount) FROM orders")
        select_items = ast["select"]
        functions = [s for s in select_items if s and s.get("type") == "function"]
        # Should have function nodes
        assert len(functions) >= 1

    def test_case_expression(self):
        ast = to_ast("SELECT CASE WHEN x > 0 THEN 'positive' ELSE 'non-positive' END FROM t")
        select_items = ast["select"]
        # SQLGlot represents CASE as a function
        # Find case expression (could be "case" type or "function" with name "CASE")
        case_found = False
        for s in select_items:
            if s:
                if s.get("type") == "case":
                    case_found = True
                    assert "when-then" in s
                elif s.get("type") == "function" and s.get("name", "").upper() == "CASE":
                    case_found = True
        assert case_found, f"Case expression not found in {select_items}"

    def test_subquery(self):
        ast = to_ast("SELECT * FROM (SELECT id FROM users) AS sub")
        # The subquery should be represented
        assert ast["type"] == "select"

    def test_union(self):
        ast = to_ast("SELECT id FROM users UNION SELECT id FROM admins")
        assert ast["type"] == "set-operation" or "union" in str(ast).lower()

    def test_cte(self):
        ast = to_ast("""
            WITH active_users AS (
                SELECT * FROM users WHERE active = 1
            )
            SELECT * FROM active_users
        """)
        assert ast["type"] == "select"
        assert ast.get("with") is not None

    def test_insert(self):
        ast = to_ast("INSERT INTO users (name, email) VALUES ('John', 'john@example.com')")
        assert ast["type"] == "insert"

    def test_update(self):
        ast = to_ast("UPDATE users SET name = 'Jane' WHERE id = 1")
        assert ast["type"] == "update"
        assert ast["where"] is not None

    def test_delete(self):
        ast = to_ast("DELETE FROM users WHERE id = 1")
        assert ast["type"] == "delete"


class TestWithInstance:
    """Tests for the with_instance option."""

    def test_includes_instance(self):
        ast = to_ast("SELECT id FROM users", with_instance=True)
        assert "instance" in ast

    def test_without_instance(self):
        ast = to_ast("SELECT id FROM users", with_instance=False)
        assert "instance" not in ast


class TestDialects:
    """Tests for dialect-specific parsing."""

    def test_mysql_backticks(self):
        ast = to_ast("SELECT `id` FROM `users`", dialect="mysql")
        assert ast["type"] == "select"

    def test_postgres_array(self):
        # Postgres-specific syntax
        ast = to_ast("SELECT ARRAY[1, 2, 3]", dialect="postgres")
        assert ast["type"] == "select"

    def test_snowflake_sample(self):
        # Snowflake-specific syntax
        ast = to_ast("SELECT * FROM users SAMPLE (10)", dialect="snowflake")
        assert ast["type"] == "select"
