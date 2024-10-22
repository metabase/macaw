package com.metabase.macaw;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Return a simplified query representation we can work with further, if possible.
 */
@SuppressWarnings({
        "PatternVariableCanBeUsed", "IfCanBeSwitch"} // don't force a newer JVM version
)
public final class BasicTableExtractor {

    public enum ErrorType {
        UNSUPPORTED_EXPRESSION,
        INVALID_QUERY,
        UNABLE_TO_PARSE
    }

    public static class AnalysisError extends RuntimeException {
        public final ErrorType errorType;
        public final Throwable cause;

        public AnalysisError(ErrorType errorType) {
            this.errorType = errorType;
            this.cause = null;
        }

        public AnalysisError(ErrorType errorType, Throwable cause) {
            this.errorType = errorType;
            this.cause = cause;
        }
    }

    public static Set<Table> getTables(Statement statement) {
        try {
            if (statement instanceof Select) {
                return getTables((Select) statement);
            }
            // This is not a query, it's probably a statement.
            throw new AnalysisError(ErrorType.INVALID_QUERY);
        } catch (IllegalArgumentException e) {
            // This query uses features that we do not yet support translating.
            throw new AnalysisError(ErrorType.UNABLE_TO_PARSE, e);
        }
    }

    private static Set<Table> getTables(Select select) {
        if (select instanceof  PlainSelect) {
            return getTables(select.getPlainSelect());
        } else {
            // We don't support more complex kinds of select statements yet.
            throw new AnalysisError(ErrorType.UNSUPPORTED_EXPRESSION);
        }
    }

    private static Set<Table> getTables(PlainSelect select) {
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
            throw new AnalysisError(ErrorType.UNABLE_TO_PARSE);
        }

        // Not currently supported
        if (select.getWithItemsList() != null) {
            throw new AnalysisError(ErrorType.UNSUPPORTED_EXPRESSION);
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
            throw new AnalysisError(ErrorType.INVALID_QUERY);
        }

        Set<Table> tables = new HashSet<>();

        for (SelectItem item : select.getSelectItems()) {
            if (item.getExpression() instanceof ParenthesedSelect) {
                // Do not allow sub-selects.
                throw new AnalysisError(ErrorType.UNSUPPORTED_EXPRESSION);
            }
        }

        Consumer<FromItem> pushOrThrow = (FromItem item) -> {
            if (item instanceof Table) {
                Table table = (Table) item;
                if (table.getName().contains("*")) {
                    // Do not allow table wildcards.
                    throw new AnalysisError(ErrorType.INVALID_QUERY);
                }
                tables.add(table);
            } else if (item instanceof TableFunction) {
                // Do not allow dynamic tables
                throw new AnalysisError(ErrorType.INVALID_QUERY);
            } else {
                // Only allow simple table references.
                throw new AnalysisError(ErrorType.UNSUPPORTED_EXPRESSION);
            }
        };

        if (select.getFromItem() != null) {
            pushOrThrow.accept(select.getFromItem());
            List<Join> joins = select.getJoins();
            if (joins != null) {
                joins.stream().map(Join::getFromItem).forEach(pushOrThrow);
            }
        }
        return tables;
    }

}
