package com.metabase.macaw;

// Borrows substantially from JSqlParser's TablesNamesFinder

import clojure.lang.Cons;
import clojure.lang.IFn;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import clojure.lang.ISeq;
import clojure.lang.PersistentList;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterSession;
import net.sf.jsqlparser.statement.alter.AlterSystemStatement;
import net.sf.jsqlparser.statement.alter.RenameTableStatement;
import net.sf.jsqlparser.statement.alter.sequence.AlterSequence;
import net.sf.jsqlparser.statement.analyze.Analyze;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.synonym.CreateSynonym;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.show.ShowIndexStatement;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.metabase.macaw.AstWalker.CallbackKey.*;
import static com.metabase.macaw.AstWalker.QueryScopeLabel.*;

/**
 * Walks the AST, using JSqlParser's `visit()` methods. Each `visit()` method additionally calls an applicable callback
 * method provided in the `callbacks` map. Supported callbacks have a corresponding key string (see below).
 * <p>
 * Why this class? Why the callbacks?
 * <p>
 * Clojure is not good at working with Java visitors. They require over<em>riding</em> various over<em>loaded</em>
 * methods and, in the case of walking a tree (exactly what we want to do here) we of course need to call `visit()`
 * recursively.
 * <p>
 * Clojure's two main ways of dealing with this are `reify`, which does not permit type-based overloading, and `proxy`,
 * which does.  However, creating a proxy object creates a completely new object that does not inherit behavior defined
 * in the parent class. Therefore, if you have code like this:

   <code>
     (proxy
       [TablesNamesFinder]
       []
       (visit [visitable]
         (if (instance? Column visitable)
           (swap! columns conj (.getColumnName ^Column visitable))
           (let [^StatementVisitor this this]
             (proxy-super visit visitable)))))
   </code>

 * the call to `proxy-super` does <em>not</em> call the original `TablesNamesFinder.visit()`; it calls `visit()` on our
 * proxied instance. Since the tree-walking semantics are defined in the original method, this behavior does not work
 * for us.
 *
 * <hr>
 *
 * Therefore, this class provides a more convenient escape hatch for Clojure. It removes the overloading requirement for
 * the conventional visitor pattern, providing instead the `callbacks` map. This lets Clojure code use a normal Clojure
 * map and functions to implement the necessary behavior; no `reify` necessary.
 */
public class AstWalker<Acc, T> implements SelectVisitor<T>, FromItemVisitor<T>, ExpressionVisitor<T>,
       SelectItemVisitor<T>, StatementVisitor<T>, GroupByVisitor<T> {

    public enum CallbackKey {
        ALIAS,
        ALL_COLUMNS,
        ALL_TABLE_COLUMNS,
        COLUMN,
        COLUMN_QUALIFIER,
        MUTATION_COMMAND,
        TABLE,
        PSEUDO_TABLES;

        public String toString() {
            return name().toLowerCase();
        }
    }

    public interface Scope {
        long getId();
        String getType();
        String getLabel();

        static ScopeInstance fromLabel(long id, QueryScopeLabel label) {
            return new ScopeInstance(id, "query", label.getValue());
        }

        static ScopeInstance other(long id, String type, String label) {
            return new ScopeInstance(id, type, label);
        }
    }

    public static class ScopeInstance implements Scope {

        private final long id;
        private final String type;
        private final String label;

        private ScopeInstance(long id, String type, String label) {
            this.id = id;
            this.type = type;
            this.label = label;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    public enum QueryScopeLabel {
        DELETE,
        ELSE,
        FROM,
        GROUP_BY,
        HAVING,
        IF,
        INSERT,
        JOIN,
        SELECT,
        SUB_SELECT,
        UPDATE,
        WHERE,
        WITH_ITEM;

        private final String value;

        QueryScopeLabel() {
            this.value = name().toUpperCase();
        }

        public String getValue() {
            return value;
        }
    }

    private Acc acc;
    private final EnumMap<CallbackKey, IFn> callbacks;
    private ISeq contextStack = PersistentList.EMPTY;
    private long nextScopeId = 1;

    /**
     * Construct a new walker with the given `callbacks`. The `callbacks` should be a (Clojure) map of CallbackKeys to
     * reducing functions.
     * <p>
     * c.f. the Clojure wrapper in <code>macaw.walk</code>
     */
    public AstWalker(Map<CallbackKey, IFn> rawCallbacks, Acc val) {
        this.acc = val;
        this.callbacks = new EnumMap<>(rawCallbacks);
    }

    /**
     * Safely invoke the given callback by name.
     */
    @SuppressWarnings("unchecked")
    public void invokeCallback(CallbackKey key, Object visitedItem) {
        IFn callback = this.callbacks.get(key);
        if (callback != null) {
            //noinspection unchecked
            this.acc = (Acc) callback.invoke(acc, visitedItem, this.contextStack);
        }
    }

    private void pushContext(QueryScopeLabel label) {
        this.contextStack = new Cons(Scope.fromLabel(nextScopeId++, label), this.contextStack);
    }

    private void pushContext(@SuppressWarnings("SameParameterValue") String type, String label) {
        this.contextStack = new Cons(Scope.other(nextScopeId++, type, label), this.contextStack);
    }

    // This is pure sugar, but it's nice to be symmetrical with pushContext
    private void popContext() {
        this.contextStack = this.contextStack.more();
    }

    /**
     * Fold the given `statement`, using the callbacks to update the accumulator as appropriate.
     */
    public Acc fold(Statement statement) {
        maybeAcceptThis(statement);
        return this.acc;
    }

    /**
     * Walk the given `expression`, invoking the callbacks for side effects as appropriate.
     */
    public Expression walk(Expression expression) {
        expression.accept(this);
        return expression;
    }

    @Override
    public <S> T visit(Select select, S context) {
        // No pushContext(SELECT) since it's handled by the ParenthesedSelect and PlainSelect methods
        List<WithItem> withItemsList = select.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor) this, context);
            }
        }
        select.accept((SelectVisitor) this, context);
        return null;
    }

    @Override
    public <S> T visit(TranscodingFunction transcodingFunction, S context) {
        transcodingFunction.getExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(TrimFunction trimFunction, S context) {
        if (trimFunction.getExpression() != null) {
            trimFunction.getExpression().accept(this);
        }
        if (trimFunction.getFromExpression() != null) {
            pushContext(FROM);
            trimFunction.getFromExpression().accept(this);
            popContext(); // FROM
        }
        return null;
    }

    @Override
    public <S> T visit(RangeExpression rangeExpression, S context) {
        rangeExpression.getStartExpression().accept(this);
        rangeExpression.getEndExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(WithItem withItem, S context) {
        pushContext(WITH_ITEM);
        invokeCallback(PSEUDO_TABLES, withItem.getAlias());
        withItem.getSelect().accept((SelectVisitor) this, context);
        return null;
    }

    @Override
    public <S> T visit(ParenthesedSelect selectBody, S context) {
        pushContext(SUB_SELECT);
        Alias alias = selectBody.getAlias();
        if (alias != null) {
            invokeCallback(PSEUDO_TABLES, alias);
        }
        List<WithItem> withItemsList = selectBody.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                this.visit(withItem);
            }
        }
        selectBody.getSelect().accept((SelectVisitor<T>) this, context);
        popContext(); // SUB_SELECT
        return null;
    }

    @Override
    public <S> T visit(PlainSelect plainSelect, S context) {
        pushContext(SELECT);
        List<WithItem> withItemsList = plainSelect.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor<T>) this, context);
            }
        }
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                item.accept(this, context);
            }
        }

        if (plainSelect.getFromItem() != null) {
            pushContext(FROM);
            plainSelect.getFromItem().accept(this);
            popContext(); // FROM
        }

        visitJoins(plainSelect.getJoins());
        if (plainSelect.getWhere() != null) {
            pushContext(WHERE);
            plainSelect.getWhere().accept(this);
            popContext(); // WHERE
        }

        if (plainSelect.getHaving() != null) {
            pushContext(HAVING);
            plainSelect.getHaving().accept(this);
            popContext(); // HAVING
        }

        if (plainSelect.getOracleHierarchical() != null) {
            plainSelect.getOracleHierarchical().accept(this);
        }

        if (plainSelect.getGroupBy() != null) {
            // contextStack handled in visit()
            plainSelect.getGroupBy().accept(this, context);
        }
        popContext(); // SELECT
        return null;
    }

    @Override
    public void visit(Table table) {
        invokeCallback(TABLE, table);
    }

    // Could be an alias, could be a real table
    public void visitColumnQualifier(Table table) {
        invokeCallback(COLUMN_QUALIFIER, table);
    }

    @Override
    public <S> T visit(Addition addition, S context) {
        visitBinaryExpression(addition);
        return null;
    }

    @Override
    public <S> T visit(AndExpression andExpression, S context) {
        visitBinaryExpression(andExpression);
        return null;
    }

    @Override
    public <S> T visit(Between between, S context) {
        between.getLeftExpression().accept(this);
        between.getBetweenExpressionStart().accept(this);
        between.getBetweenExpressionEnd().accept(this);
        return null;
    }

    @Override
    public <S> T visit(OverlapsCondition overlapsCondition, S context) {
        overlapsCondition.getLeft().accept(this);
        overlapsCondition.getRight().accept(this);
        return null;
    }

    @Override
    public <S> T visit(Column tableColumn, S context) {
        invokeCallback(COLUMN, tableColumn);

        Table table = tableColumn.getTable();
        if (table != null && table.getName() != null) {
            // Visiting aliases (e.g., the `o` in `o.id` in `select o.id from orders o`) is unhelpful if we're trying
            // to get the set of actual table names used.
            // However, for query rewriting it is necessary.
            visitColumnQualifier(table);
        }
        return null;
    }

    @Override
    public <S> T visit(Division division, S context) {
        visitBinaryExpression(division);
        return null;
    }

    @Override
    public <S> T visit(IntegerDivision division, S context) {
        visitBinaryExpression(division);
        return null;
    }

    @Override
    public <S> T visit(DoubleValue doubleValue, S context) {

        return null;
    }

    @Override
    public <S> T visit(EqualsTo equalsTo, S context) {
        visitBinaryExpression(equalsTo);
        return null;
    }

    @Override
    public <S> T visit(Function function, S context) {
        ExpressionList<?> exprList = function.getParameters();
        if (exprList != null) {
            visit(exprList);
        }
        return null;
    }

    @Override
    public <S> T visit(GreaterThan greaterThan, S context) {
        visitBinaryExpression(greaterThan);
        return null;
    }

    @Override
    public <S> T visit(GreaterThanEquals greaterThanEquals, S context) {
        visitBinaryExpression(greaterThanEquals);
        return null;
    }

    @Override
    public <S> T visit(InExpression inExpression, S context) {
        inExpression.getLeftExpression().accept(this);
        inExpression.getRightExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(FullTextSearch fullTextSearch, S context) {

        return null;
    }

    @Override
    public <S> T visit(SignedExpression signedExpression, S context) {
        signedExpression.getExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(IsNullExpression isNullExpression, S context) {

        return null;
    }

    @Override
    public <S> T visit(IsBooleanExpression isBooleanExpression, S context) {

        return null;
    }

    @Override
    public <S> T visit(JdbcParameter jdbcParameter, S context) {

        return null;
    }

    @Override
    public <S> T visit(LikeExpression likeExpression, S context) {
        visitBinaryExpression(likeExpression);
        return null;
    }

    @Override
    public <S> T visit(ExistsExpression existsExpression, S context) {
        existsExpression.getRightExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(MemberOfExpression memberOfExpression, S context) {
        memberOfExpression.getLeftExpression().accept(this);
        memberOfExpression.getRightExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(LongValue longValue, S context) {

        return null;
    }

    @Override
    public <S> T visit(MinorThan minorThan, S context) {
        visitBinaryExpression(minorThan);
        return null;
    }

    @Override
    public <S> T visit(MinorThanEquals minorThanEquals, S context) {
        visitBinaryExpression(minorThanEquals);
        return null;
    }

    @Override
    public <S> T visit(Multiplication multiplication, S context) {
        visitBinaryExpression(multiplication);
        return null;
    }

    @Override
    public <S> T visit(NotEqualsTo notEqualsTo, S context) {
        visitBinaryExpression(notEqualsTo);
        return null;
    }

    @Override
    public <S> T visit(DoubleAnd doubleAnd, S context) {
        visitBinaryExpression(doubleAnd);
        return null;
    }

    @Override
    public <S> T visit(Contains contains, S context) {
        visitBinaryExpression(contains);
        return null;
    }

    @Override
    public <S> T visit(ContainedBy containedBy, S context) {
        visitBinaryExpression(containedBy);
        return null;
    }

    @Override
    public <S> T visit(NullValue nullValue, S context) {

        return null;
    }

    @Override
    public <S> T visit(OrExpression orExpression, S context) {
        visitBinaryExpression(orExpression);
        return null;
    }

    @Override
    public <S> T visit(XorExpression xorExpression, S context) {
        visitBinaryExpression(xorExpression);
        return null;
    }

//    @Override
//    public <S> T visit(Parenthesis parenthesis, S context) {
//        parenthesis.getExpression().accept(this, context);
//        return null;
//    }

    @Override
    public <S> T visit(StringValue stringValue, S context) {
        return null;
    }

    @Override
    public <S> T visit(Subtraction subtraction, S context) {
        visitBinaryExpression(subtraction);
        return null;
    }

    @Override
    public <S> T visit(NotExpression notExpr, S context) {
        notExpr.getExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(BitwiseRightShift expr, S context) {
        visitBinaryExpression(expr);
        return null;
    }

    @Override
    public <S> T visit(BitwiseLeftShift expr, S context) {
        visitBinaryExpression(expr);
        return null;
    }

    public void visitBinaryExpression(BinaryExpression binaryExpression) {
        binaryExpression.getLeftExpression().accept(this);
        binaryExpression.getRightExpression().accept(this);
    }

    @Override
    public <S> T visit(ExpressionList<?> expressionList, S context) {
        for (Expression expression : expressionList) {
            // The use of a wildcard within a function means "nothing".
            if (!(expression instanceof AllColumns)) {
                expression.accept(this);
            }
        }
        return null;
    }

    @Override
    public <S> T visit(DateValue dateValue, S context) {
        return null;
    }

    @Override
    public <S> T visit(TimestampValue timestampValue, S context) {
        return null;
    }

    @Override
    public <S> T visit(TimeValue timeValue, S context) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.
     * CaseExpression)
     */
    @Override
    public void visit(CaseExpression caseExpression) {
        if (caseExpression.getSwitchExpression() != null) {
            caseExpression.getSwitchExpression().accept(this);
        }
        if (caseExpression.getWhenClauses() != null) {
            for (WhenClause when : caseExpression.getWhenClauses()) {
                when.accept(this);
            }
        }
        if (caseExpression.getElseExpression() != null) {
            caseExpression.getElseExpression().accept(this);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.WhenClause)
     */
    @Override
    public void visit(WhenClause whenClause) {
        if (whenClause.getWhenExpression() != null) {
            whenClause.getWhenExpression().accept(this);
        }
        if (whenClause.getThenExpression() != null) {
            whenClause.getThenExpression().accept(this);
        }
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        anyComparisonExpression.getSelect().accept((ExpressionVisitor) this);
    }

    @Override
    public void visit(Concat concat) {
        visitBinaryExpression(concat);
    }

    @Override
    public void visit(Matches matches) {
        visitBinaryExpression(matches);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        visitBinaryExpression(bitwiseAnd);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        visitBinaryExpression(bitwiseOr);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        visitBinaryExpression(bitwiseXor);
    }

    @Override
    public void visit(CastExpression cast) {
        cast.getLeftExpression().accept(this);
    }

    @Override
    public void visit(Modulo modulo) {
        visitBinaryExpression(modulo);
    }

    @Override
    public void visit(AnalyticExpression analytic) {
        maybeAcceptThis(analytic.getExpression());
        maybeAcceptThis(analytic.getDefaultValue());
        maybeAcceptThis(analytic.getOffset());
        maybeAcceptThis(analytic.getKeep());

        if (analytic.getFuncOrderBy() != null) {
            for (OrderByElement element : analytic.getOrderByElements()) {
                element.getExpression().accept(this);
            }
        }

        WindowElement windowElement = analytic.getWindowElement();
        if (windowElement != null) {
            WindowRange range = windowElement.getRange();
            maybeAcceptThis(range.getStart().getExpression());
            maybeAcceptThis(range.getEnd().getExpression());

            WindowOffset offset = windowElement.getOffset();
            if (offset != null) {
                maybeAcceptThis(offset.getExpression());
            }
        }
    }

    private void maybeAcceptThis(Expression expression) {
        if (expression != null) {
            expression.accept(this);
        }
    }

    private void maybeAcceptThis(Statement statement) {
        if (statement != null) {
            statement.accept(this);
        }
    }

    @Override
    public <S> T visit(SetOperationList list, S context) {
        List<WithItem> withItemsList = list.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor<T>) this, context);
            }
        }
        for (Select selectBody : list.getSelects()) {
            selectBody.accept((SelectVisitor<T>) this, context);
        }
        return null;
    }

    @Override
    public void visit(ExtractExpression eexpr) {
        if (eexpr.getExpression() != null) {
            eexpr.getExpression().accept(this);
        }
    }

    @Override
    public <S> T visit(LateralSubSelect lateralSubSelect, S context) {
        lateralSubSelect.getSelect().accept((SelectVisitor<T>) this, context);
        return null;
    }

    @Override
    public void visit(TableStatement tableStatement) {
        tableStatement.getTable().accept(this);
    }

    @Override
    public void visit(IntervalExpression iexpr) {
        if (iexpr.getExpression() != null) {
            iexpr.getExpression().accept(this);
        }
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {

    }

    @Override
    public void visit(OracleHierarchicalExpression oexpr) {
        if (oexpr.getStartExpression() != null) {
            oexpr.getStartExpression().accept(this);
        }

        if (oexpr.getConnectExpression() != null) {
            oexpr.getConnectExpression().accept(this);
        }
    }

    @Override
    public void visit(RegExpMatchOperator rexpr) {
        visitBinaryExpression(rexpr);
    }

    @Override
    public void visit(JsonExpression jsonExpr) {
        if (jsonExpr.getExpression() != null) {
            jsonExpr.getExpression().accept(this);
        }
    }

    @Override
    public void visit(JsonOperator jsonExpr) {
        visitBinaryExpression(jsonExpr);
    }

    @Override
    public void visit(AllColumns allColumns) {
        invokeCallback(ALL_COLUMNS, allColumns);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        invokeCallback(ALL_TABLE_COLUMNS, allTableColumns);
    }

    @Override
    public void visit(AllValue allValue) {

    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        visitBinaryExpression(isDistinctExpression);
    }

    @Override
    public void visit(SelectItem item) {
        // TODO: what are .getAliasColumns()? Should we look at them?
        var alias = item.getAlias();
        if (alias != null) {
            // FIXME: this is absolutely a hack, what's the best way to get around it?
            pushContext("alias", alias.getName());
            // This should be able to replace it.
            invokeCallback(ALIAS, item);
        }
        item.getExpression().accept(this);
        if (alias != null) {
            popContext();
        }
    }

    @Override
    public void visit(UserVariable var) {

    }

    @Override
    public void visit(NumericBind bind) {

    }

    @Override
    public void visit(KeepExpression aexpr) {

    }

    @Override
    public void visit(MySQLGroupConcat groupConcat) {

    }

    @Override
    public void visit(Delete delete) {
        pushContext(DELETE);
        invokeCallback(MUTATION_COMMAND, "delete");
        visit(delete.getTable());

        if (delete.getUsingList() != null) {
            for (Table using : delete.getUsingList()) {
                visit(using);
            }
        }

        visitJoins(delete.getJoins());

        if (delete.getWhere() != null) {
            pushContext(WHERE);
            delete.getWhere().accept(this);
            popContext(); // WHERE
        }
        popContext(); // DELETE
    }

    @SuppressWarnings("deprecation")
    @Override
    public <S> T visit(Update update, S context) {
        pushContext(UPDATE);
        invokeCallback(MUTATION_COMMAND, "update");
        visit(update.getTable());
        if (update.getWithItemsList() != null) {
            for (WithItem withItem : update.getWithItemsList()) {
                withItem.accept((SelectVisitor) this, context);
            }
        }

        if (update.getStartJoins() != null) {
            for (Join join : update.getStartJoins()) {
                join.getRightItem().accept(this);
            }
        }
        if (update.getExpressions() != null) {
            for (Expression expression : update.getExpressions()) {
                expression.accept(this);
            }
        }

        if (update.getFromItem() != null) {
            pushContext(FROM);
            update.getFromItem().accept(this);
            popContext(); // FROM
        }

        if (update.getJoins() != null) {
            for (Join join : update.getJoins()) {
                join.getRightItem().accept(this);
                for (Expression expression : join.getOnExpressions()) {
                    expression.accept(this);
                }
            }
        }

        if (update.getWhere() != null) {
            pushContext(WHERE);
            update.getWhere().accept(this);
            popContext(); // WHERE
        }
        popContext(); // UPDATE
        return null;
    }

    @Override
    public <S> T visit(Insert insert, S context) {
        pushContext(INSERT);
        invokeCallback(MUTATION_COMMAND, "insert");
        visit(insert.getTable());
        if (insert.getWithItemsList() != null) {
            for (WithItem withItem : insert.getWithItemsList()) {
                withItem.accept((SelectVisitor) this, context);
            }
        }
        if (insert.getSelect() != null) {
            visit(insert.getSelect());
        }
        popContext(); // INSERT
        return null;
    }

    public <S> T visit(Analyze analyze, S context) {
        visit(analyze.getTable());
        return null;
    }

    @Override
    public <S> T visit(Drop drop, S context) {
        invokeCallback(MUTATION_COMMAND, "drop");
        visit(drop.getName());
        return null;
    }

    @Override
    public <S> T visit(Truncate truncate, S context) {
        invokeCallback(MUTATION_COMMAND, "truncate");
        visit(truncate.getTable());
        return null;
    }

    @Override
    public <S> T visit(CreateIndex createIndex, S context) {
        invokeCallback(MUTATION_COMMAND, "create-index");
        return null;
    }

    @Override
    public <S> T visit(CreateSchema aThis, S context) {
        invokeCallback(MUTATION_COMMAND, "create-schema");
        return null;
    }

    @Override
    public <S> T visit(CreateTable create, S context) {
        invokeCallback(MUTATION_COMMAND, "create-table");
        visit(create.getTable());
        if (create.getSelect() != null) {
            create.getSelect().accept((SelectVisitor) this);
        }
        return null;
    }

    @Override
    public <S> T visit(CreateView createView, S context) {
        invokeCallback(MUTATION_COMMAND, "create-view");
        return null;
    }

    @Override
    public <S> T visit(Alter alter, S context) {
        invokeCallback(MUTATION_COMMAND, "alter-table");
        return null;
    }

    @Override
    public <S> T visit(Statements stmts, S context) {
            throw new AnalysisError(AnalysisErrorType.INVALID_QUERY);
        return null;
    }

    @Override
    public <S> T visit(Execute execute, S context) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        return null;
    }

    @Override
    public <S> T visit(SetStatement set, S context) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        return null;
    }

    @Override
    public <S> T visit(ResetStatement reset, S context) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        return null;
    }

    @Override
    public <S> T visit(ShowColumnsStatement set, S context) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        return null;
    }

    @Override
    public <S> T visit(ShowIndexStatement showIndex, S context) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        return null;
    }

    @Override
    public <S> T visit(RowConstructor<?> rowConstructor, S context) {
        for (Expression expr : rowConstructor) {
            expr.accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(RowGetExpression rowGetExpression, S context) {
        rowGetExpression.getExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(HexValue hexValue, S context) {

        return null;
    }

    @Override
    public <S> T visit(Merge merge, S context) {
        visit(merge.getTable());
        if (merge.getWithItemsList() != null) {
            for (WithItem withItem : merge.getWithItemsList()) {
                withItem.accept((SelectVisitor) this);
            }
        }

        if (merge.getFromItem() != null) {
            pushContext(FROM);
            merge.getFromItem().accept(this);
            popContext(); // FROM
        }
        return null;
    }

    @Override
    public <S> T visit(OracleHint hint, S context) {

        return null;
    }

    @Override
    public <S> T visit(TableFunction tableFunction, S context) {
        visit(tableFunction.getFunction());
        return null;
    }

    @Override
    public <S> T visit(AlterView alterView, S context) {
        invokeCallback(MUTATION_COMMAND, "alter-view");
        return null;
    }

    @Override
    public <S> T visit(RefreshMaterializedViewStatement materializedView, S context) {
        visit(materializedView.getView());
        return null;
    }

    @Override
    public <S> T visit(TimeKeyExpression timeKeyExpression, S context) {
        return null;
    }

    @Override
    public <S> T visit(DateTimeLiteralExpression literal, S context) {
        return null;
    }

    @Override
    public <S> T visit(Commit commit, S context) {

        return null;
    }

    @Override
    public <S> T visit(Upsert upsert, S context) {
        visit(upsert.getTable());
        if (upsert.getExpressions() != null) {
            upsert.getExpressions().accept(this);
        }
        if (upsert.getSelect() != null) {
            visit(upsert.getSelect());
        }
        return null;
    }

    @Override
    public <S> T visit(UseStatement use, S context) {
        return null;
    }

    @Override
    public <S> T visit(ParenthesedFromItem parenthesis, S context) {
        parenthesis.getFromItem().accept(this);
        // support join keyword in fromItem
        visitJoins(parenthesis.getJoins());
        return null;
    }

    @Override
    public <S> T visit(GroupByElement element, S context) {
        pushContext(GROUP_BY);
        element.getGroupByExpressionList().accept(this);
        for (ExpressionList<?> exprList : element.getGroupingSets()) {
            exprList.accept(this);
        }
        popContext(); // GROUP_BY
    }

    /**
     * visit join block
     *
     * @param parenthesis join sql block
     */
    private void visitJoins(List<Join> parenthesis) {
        pushContext(JOIN);
        if (parenthesis == null) {
            return;
        }
        for (Join join : parenthesis) {
            pushContext(FROM);
            join.getFromItem().accept(this);
            popContext(); // FROM
            join.getRightItem().accept(this);
            for (Expression expression : join.getOnExpressions()) {
                expression.accept(this);
            }
        }
        popContext(); // JOIN
    }

    @Override
    public <S> T visit(Block block, S context) {
        if (block.getStatements() != null) {
            visit(block.getStatements());
        }
        return null;
    }

    @Override
    public <S> T visit(Comment comment, S context) {
        if (comment.getTable() != null) {
            visit(comment.getTable());
        }
        if (comment.getColumn() != null) {
            Table table = comment.getColumn().getTable();
            if (table != null) {
                visit(table);
            }
        }
        return null;
    }

    @Override
    public <S> T visit(Values values, S context) {
        values.getExpressions().accept(this);
        return null;
    }

    @Override
    public <S> T visit(DescribeStatement describe, S context) {
        describe.getTable().accept(this);
        return null;
    }

    @Override
    public <S> T visit(ExplainStatement explain, S context) {
        if (explain.getStatement() != null) {
            explain.getStatement().accept((StatementVisitor) this);
        }
        return null;
    }

    @Override
    public <S> T visit(NextValExpression nextVal, S context) {

        return null;
    }

    @Override
    public <S> T visit(CollateExpression col, S context) {
        col.getLeftExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(ShowStatement aThis, S context) {

        return null;
    }

    @Override
    public <S> T visit(SimilarToExpression expr, S context) {
        visitBinaryExpression(expr);
        return null;
    }

    @Override
    public <S> T visit(DeclareStatement aThis, S context) {

        return null;
    }

    @Override
    public <S> T visit(Grant grant, S context) {
        invokeCallback(MUTATION_COMMAND, "grant");
        return null;
    }

    @Override
    public <S> T visit(ArrayExpression array, S context) {
        array.getObjExpression().accept(this);
        if (array.getStartIndexExpression() != null) {
            array.getIndexExpression().accept(this);
        }
        if (array.getStartIndexExpression() != null) {
            array.getStartIndexExpression().accept(this);
        }
        if (array.getStopIndexExpression() != null) {
            array.getStopIndexExpression().accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(ArrayConstructor array, S context) {
        for (Expression expression : array.getExpressions()) {
            expression.accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(CreateSequence createSequence, S context) {
        invokeCallback(MUTATION_COMMAND, "create-sequence");
        return null;
    }

    @Override
    public <S> T visit(AlterSequence alterSequence, S context) {
        invokeCallback(MUTATION_COMMAND, "alter-sequence");
        return null;
    }

    @Override
    public <S> T visit(CreateFunctionalStatement createFunctionalStatement, S context) {
        invokeCallback(MUTATION_COMMAND, "create-function");
        return null;
    }

    @Override
    public <S> T visit(ShowTablesStatement showTables, S context) {
        throw new UnsupportedOperationException(
                "Reading from a ShowTablesStatement is not supported");
        return null;
    }

    @Override
    public <S> T visit(TSQLLeftJoin tsqlLeftJoin, S context) {
        visitBinaryExpression(tsqlLeftJoin);
        return null;
    }

    @Override
    public <S> T visit(TSQLRightJoin tsqlRightJoin, S context) {
        visitBinaryExpression(tsqlRightJoin);
        return null;
    }

    @Override
    public <S> T visit(VariableAssignment var, S context) {
        var.getVariable().accept(this);
        var.getExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(XMLSerializeExpr aThis, S context) {

        return null;
    }

    @Override
    public <S> T visit(CreateSynonym createSynonym, S context) {
        invokeCallback(MUTATION_COMMAND, "create-synonym");
        return null;
    }

    @Override
    public <S> T visit(TimezoneExpression aThis, S context) {
        aThis.getLeftExpression().accept(this);
        return null;
    }

    @Override
    return null;
    public <S> T visit(SavepointStatement savepointStatement, S context) {}

    @Override
    public <S> T visit(RollbackStatement rollbackStatement, S context) {

        return null;
    }

    @Override
    public <S> T visit(AlterSession alterSession, S context) {
        invokeCallback(MUTATION_COMMAND, "alter-session");
        return null;
    }

    @Override
    public <S> T visit(JsonAggregateFunction expression, S context) {
        Expression expr = expression.getExpression();
        if (expr != null) {
            expr.accept(this);
        }

        expr = expression.getFilterExpression();
        if (expr != null) {
            expr.accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(JsonFunction expression, S context) {
        for (JsonFunctionExpression expr : expression.getExpressions()) {
            expr.getExpression().accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(ConnectByRootOperator connectByRootOperator, S context) {
        connectByRootOperator.getColumn().accept(this);
        return null;
    }

    public <S> T visit(IfElseStatement ifElseStatement, S context) {
        pushContext(IF);
        ifElseStatement.getIfStatement().accept(this);
        popContext(); // IF
        if (ifElseStatement.getElseStatement() != null) {
            pushContext(ELSE);
            ifElseStatement.getElseStatement().accept(this);
            popContext(); // ELSE
        }
        return null;
    }

    public <S> T visit(OracleNamedFunctionParameter oracleNamedFunctionParameter, S context) {
        oracleNamedFunctionParameter.getExpression().accept(this);
        return null;
    }

    @Override
    public <S> T visit(RenameTableStatement renameTableStatement, S context) {
        invokeCallback(MUTATION_COMMAND, "rename-table");
        for (Map.Entry<Table, Table> e : renameTableStatement.getTableNames()) {
            e.getKey().accept(this);
            e.getValue().accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(PurgeStatement purgeStatement, S context) {
        invokeCallback(MUTATION_COMMAND, "purge");
        if (purgeStatement.getPurgeObjectType() == PurgeObjectType.TABLE) {
            ((Table) purgeStatement.getObject()).accept(this);
        }
        return null;
    }

    @Override
    public <S> T visit(AlterSystemStatement alterSystemStatement, S context) {
        invokeCallback(MUTATION_COMMAND, "alter-system");
        return null;
    }

    @Override
    public <S> T visit(UnsupportedStatement unsupportedStatement, S context) {
        return null;
    }

    @Override
    public <S> T visit(GeometryDistance geometryDistance, S context) {
        visitBinaryExpression(geometryDistance);
        return null;
    }
}
