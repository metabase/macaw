package com.metabase.macaw;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

@SuppressWarnings({"PatternVariableCanBeUsed", "IfCanBeSwitch"}) // don't force a newer JVM version
public final class CompoundTableExtractor {

    /**
     * BEWARE this may return duplicates if the same table is referenced multiple times in the query.
     * We need to deduplicate these with value semantics.
     * A better solution would be to have our own TableValue class to convert to, before accumulating.
     */
    public static Set<Table> getTables(Statement statement) {
        try {
            if (statement instanceof Select) {
                return accTables((Select) statement);
            }
            // This is not a query, it's probably a statement.
            throw new AnalysisError(AnalysisErrorType.INVALID_QUERY);
        } catch (IllegalArgumentException e) {
            // This query uses features that we do not yet support translating.
            throw new AnalysisError(AnalysisErrorType.UNABLE_TO_PARSE, e);
        }
    }

    private static Set<Table> accTables(Select select) {
        Set<Table> tables = new HashSet<>();
        Stack<Set<String>> cteAliasScopes = new Stack<>();
        accTables(select, tables, cteAliasScopes);
        return tables;
    }

    private static void accTables(Select select, Set<Table> tables, Stack<Set<String>> cteAliasScopes) {
        if (select instanceof PlainSelect) {
            accTables(select.getPlainSelect(), tables, cteAliasScopes);
        } else if (select instanceof ParenthesedSelect) {
            accTables(((ParenthesedSelect) select).getSelect(), tables, cteAliasScopes);
        } else if (select instanceof  SetOperationList) {
            for (Select innerSelect : ((SetOperationList) select).getSelects()) {
                accTables(innerSelect, tables, cteAliasScopes);
            }
        } else {
            // We don't support more complex kinds of select statements yet.
            throw new AnalysisError(AnalysisErrorType.UNSUPPORTED_EXPRESSION);
        }
    }

    private static void accTables(PlainSelect select, Set<Table> tables, Stack<Set<String>> cteAliasScopes) {
        // these are fine, but irrelevant
        //
        // - select.getDistinct()
        // - select.getHaving()
        // - select.getKsqlWindow()
        // - select.getMySqlHintStraightJoin()
        // - select.getMySqlSqlCacheFlag()
        // - select.getLimitBy()
        // - select.getOffset()
        // - select.getOptimizeFor()
        // - select.getOracleHint()
        // - select.getTop()
        // - select.getWait()

        // Not currently parseable
        if (select.getLateralViews() != null ||
                select.getOracleHierarchical() != null ||
                select.getWindowDefinitions() != null) {
            throw new AnalysisError(AnalysisErrorType.UNABLE_TO_PARSE);
        }

        final Set<String> cteAliases = new HashSet<>();
        cteAliasScopes.push(cteAliases);

        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                if (withItem.isRecursive()) {
                    cteAliases.add(withItem.getAlias().getName());
                }
                accTables(withItem.getSelect(), tables, cteAliasScopes);
                // No hard in adding twice to a set
                cteAliases.add(withItem.getAlias().getName());
            }
        }

        // any of these - nope out
        if (select.getFetch() != null ||
                select.getFirst() != null ||
                select.getForClause() != null ||
                select.getForMode() != null ||
                select.getForUpdateTable() != null ||
                select.getForXmlPath() != null ||
                select.getIntoTables() != null ||
                select.getIsolation() != null ||
                select.getSkip() != null) {
            throw new AnalysisError(AnalysisErrorType.INVALID_QUERY);
        }

        for (SelectItem<?> item : select.getSelectItems()) {
            Expression expr = item.getExpression();
            if (expr instanceof Select) {
                accTables((Select) expr, tables, cteAliasScopes);
            }
        }

        Consumer<FromItem> pushOrThrow = (FromItem item) -> {
            if (item instanceof Table) {
                Table table = (Table) item;
                String tableName = table.getName();
                // Skip aliases
                if (cteAliasScopes.stream().noneMatch(scope -> scope.contains(tableName))) {
                    if (tableName.contains("*")) {
                        // Do not allow table wildcards.
                        throw new AnalysisError(AnalysisErrorType.INVALID_QUERY);
                    } else {
                        tables.add(table);
                    }
                }
            } else if (item instanceof TableFunction) {
                // Do not allow dynamic tables
                throw new AnalysisError(AnalysisErrorType.INVALID_QUERY);
            } else if (item instanceof Select) {
                accTables((Select) item, tables, cteAliasScopes);
            } else if (item != null) {
                // Only allow simple table references.
                throw new AnalysisError(AnalysisErrorType.UNSUPPORTED_EXPRESSION);
            }
        };

        if (select.getFromItem() != null) {
            pushOrThrow.accept(select.getFromItem());
            List<Join> joins = select.getJoins();
            if (joins != null) {
                joins.stream().map(Join::getFromItem).forEach(pushOrThrow);
            }
        }

        cteAliasScopes.pop();
    }

}
