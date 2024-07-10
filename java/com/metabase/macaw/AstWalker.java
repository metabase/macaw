package com.metabase.macaw;

// Borrows substantially from JSqlParser's TablesNamesFinder

import clojure.lang.IFn;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseLeftShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseRightShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.IntegerDivision;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ContainedBy;
import net.sf.jsqlparser.expression.operators.relational.Contains;
import net.sf.jsqlparser.expression.operators.relational.DoubleAnd;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.FullTextSearch;
import net.sf.jsqlparser.expression.operators.relational.GeometryDistance;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression;
import net.sf.jsqlparser.expression.operators.relational.IsDistinctExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MemberOfExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.SimilarToExpression;
import net.sf.jsqlparser.expression.operators.relational.TSQLLeftJoin;
import net.sf.jsqlparser.expression.operators.relational.TSQLRightJoin;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Block;
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.CreateFunctionalStatement;
import net.sf.jsqlparser.statement.DeclareStatement;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.IfElseStatement;
import net.sf.jsqlparser.statement.PurgeObjectType;
import net.sf.jsqlparser.statement.PurgeStatement;
import net.sf.jsqlparser.statement.ResetStatement;
import net.sf.jsqlparser.statement.RollbackStatement;
import net.sf.jsqlparser.statement.SavepointStatement;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.ShowColumnsStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.UseStatement;
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
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedFromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.TableStatement;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.show.ShowIndexStatement;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;

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
public class AstWalker<Acc> implements SelectVisitor, FromItemVisitor, ExpressionVisitor,
       SelectItemVisitor, StatementVisitor, GroupByVisitor {

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

    private static final String NOT_SUPPORTED_YET = "Not supported yet.";

    private Acc acc;
    private final EnumMap<CallbackKey, IFn> callbacks;
    private final Deque<Scope> contextStack;
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
        this.contextStack = new ArrayDeque<>();
    }

    /**
     * Safely invoke the given callback by name.
     */
    @SuppressWarnings("unchecked")
    public void invokeCallback(CallbackKey key, Object visitedItem) {
        IFn callback = this.callbacks.get(key);
        if (callback != null) {
            //noinspection unchecked
            this.acc = (Acc) callback.invoke(acc, visitedItem, this.contextStack.toArray());
        }
    }

    private void pushContext(QueryScopeLabel label) {
        this.contextStack.push(Scope.fromLabel(nextScopeId++, label));
    }

    private void pushContext(@SuppressWarnings("SameParameterValue") String type, String label) {
        this.contextStack.push(Scope.other(nextScopeId++, type, label));
    }

    // This is pure sugar, but it's nice to be symmetrical with pushContext
    private void popContext() {
        this.contextStack.pop();
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
    public void visit(Select select) {
        // No pushContext(SELECT) since it's handled by the ParenthesedSelect and PlainSelect methods
        List<WithItem> withItemsList = select.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor) this);
            }
        }
        select.accept((SelectVisitor) this);
    }

    @Override
    public void visit(TranscodingFunction transcodingFunction) {
        transcodingFunction.getExpression().accept(this);
    }

    @Override
    public void visit(TrimFunction trimFunction) {
        if (trimFunction.getExpression() != null) {
            trimFunction.getExpression().accept(this);
        }
        if (trimFunction.getFromExpression() != null) {
            pushContext(FROM);
            trimFunction.getFromExpression().accept(this);
            popContext(); // FROM
        }
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
        rangeExpression.getStartExpression().accept(this);
        rangeExpression.getEndExpression().accept(this);
    }

    @Override
    public void visit(WithItem withItem) {
        pushContext(WITH_ITEM);
        invokeCallback(PSEUDO_TABLES, withItem.getAlias());
        withItem.getSelect().accept((SelectVisitor) this);
    }

    @Override
    public void visit(ParenthesedSelect selectBody) {
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
        selectBody.getSelect().accept((SelectVisitor) this);
        popContext(); // SUB_SELECT
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        pushContext(SELECT);
        List<WithItem> withItemsList = plainSelect.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor) this);
            }
        }
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                item.accept(this);
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
            plainSelect.getGroupBy().accept(this);
        }
        popContext(); // SELECT
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
    public void visit(Addition addition) {
        visitBinaryExpression(addition);
    }

    @Override
    public void visit(AndExpression andExpression) {
        visitBinaryExpression(andExpression);
    }

    @Override
    public void visit(Between between) {
        between.getLeftExpression().accept(this);
        between.getBetweenExpressionStart().accept(this);
        between.getBetweenExpressionEnd().accept(this);
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        overlapsCondition.getLeft().accept(this);
        overlapsCondition.getRight().accept(this);
    }

    @Override
    public void visit(Column tableColumn) {
        invokeCallback(COLUMN, tableColumn);

        Table table = tableColumn.getTable();
        if (table != null && table.getName() != null) {
            // Visiting aliases (e.g., the `o` in `o.id` in `select o.id from orders o`) is unhelpful if we're trying
            // to get the set of actual table names used.
            // However, for query rewriting it is necessary.
            visitColumnQualifier(table);
        }
    }

    @Override
    public void visit(Division division) {
        visitBinaryExpression(division);
    }

    @Override
    public void visit(IntegerDivision division) {
        visitBinaryExpression(division);
    }

    @Override
    public void visit(DoubleValue doubleValue) {

    }

    @Override
    public void visit(EqualsTo equalsTo) {
        visitBinaryExpression(equalsTo);
    }

    @Override
    public void visit(Function function) {
        ExpressionList<?> exprList = function.getParameters();
        if (exprList != null) {
            visit(exprList);
        }
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        visitBinaryExpression(greaterThan);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        visitBinaryExpression(greaterThanEquals);
    }

    @Override
    public void visit(InExpression inExpression) {
        inExpression.getLeftExpression().accept(this);
        inExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {

    }

    @Override
    public void visit(SignedExpression signedExpression) {
        signedExpression.getExpression().accept(this);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {

    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {

    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {

    }

    @Override
    public void visit(LikeExpression likeExpression) {
        visitBinaryExpression(likeExpression);
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        existsExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(MemberOfExpression memberOfExpression) {
        memberOfExpression.getLeftExpression().accept(this);
        memberOfExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(LongValue longValue) {

    }

    @Override
    public void visit(MinorThan minorThan) {
        visitBinaryExpression(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        visitBinaryExpression(minorThanEquals);
    }

    @Override
    public void visit(Multiplication multiplication) {
        visitBinaryExpression(multiplication);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        visitBinaryExpression(notEqualsTo);
    }

    @Override
    public void visit(DoubleAnd doubleAnd) {
        visitBinaryExpression(doubleAnd);
    }

    @Override
    public void visit(Contains contains) {
        visitBinaryExpression(contains);
    }

    @Override
    public void visit(ContainedBy containedBy) {
        visitBinaryExpression(containedBy);
    }

    @Override
    public void visit(NullValue nullValue) {

    }

    @Override
    public void visit(OrExpression orExpression) {
        visitBinaryExpression(orExpression);
    }

    @Override
    public void visit(XorExpression xorExpression) {
        visitBinaryExpression(xorExpression);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(StringValue stringValue) {

    }

    @Override
    public void visit(Subtraction subtraction) {
        visitBinaryExpression(subtraction);
    }

    @Override
    public void visit(NotExpression notExpr) {
        notExpr.getExpression().accept(this);
    }

    @Override
    public void visit(BitwiseRightShift expr) {
        visitBinaryExpression(expr);
    }

    @Override
    public void visit(BitwiseLeftShift expr) {
        visitBinaryExpression(expr);
    }

    public void visitBinaryExpression(BinaryExpression binaryExpression) {
        binaryExpression.getLeftExpression().accept(this);
        binaryExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(ExpressionList<?> expressionList) {
        for (Expression expression : expressionList) {
            // The use of a wildcard within a function means "nothing".
            if (!(expression instanceof AllColumns)) {
                expression.accept(this);
            }
        }
    }

    @Override
    public void visit(DateValue dateValue) {

    }

    @Override
    public void visit(TimestampValue timestampValue) {

    }

    @Override
    public void visit(TimeValue timeValue) {

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
    public void visit(SetOperationList list) {
        List<WithItem> withItemsList = list.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor) this);
            }
        }
        for (Select selectBody : list.getSelects()) {
            selectBody.accept((SelectVisitor) this);
        }
    }

    @Override
    public void visit(ExtractExpression eexpr) {
        if (eexpr.getExpression() != null) {
            eexpr.getExpression().accept(this);
        }
    }

    @Override
    public void visit(LateralSubSelect lateralSubSelect) {
        lateralSubSelect.getSelect().accept((SelectVisitor) this);
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
    public void visit(Update update) {
        pushContext(UPDATE);
        invokeCallback(MUTATION_COMMAND, "update");
        visit(update.getTable());
        if (update.getWithItemsList() != null) {
            for (WithItem withItem : update.getWithItemsList()) {
                withItem.accept((SelectVisitor) this);
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
    }

    @Override
    public void visit(Insert insert) {
        pushContext(INSERT);
        invokeCallback(MUTATION_COMMAND, "insert");
        visit(insert.getTable());
        if (insert.getWithItemsList() != null) {
            for (WithItem withItem : insert.getWithItemsList()) {
                withItem.accept((SelectVisitor) this);
            }
        }
        if (insert.getSelect() != null) {
            visit(insert.getSelect());
        }
        popContext(); // INSERT
    }

    public void visit(Analyze analyze) {
        visit(analyze.getTable());
    }

    @Override
    public void visit(Drop drop) {
        invokeCallback(MUTATION_COMMAND, "drop");
        visit(drop.getName());
    }

    @Override
    public void visit(Truncate truncate) {
        invokeCallback(MUTATION_COMMAND, "truncate");
        visit(truncate.getTable());
    }

    @Override
    public void visit(CreateIndex createIndex) {
        invokeCallback(MUTATION_COMMAND, "create-index");
    }

    @Override
    public void visit(CreateSchema aThis) {
        invokeCallback(MUTATION_COMMAND, "create-schema");
    }

    @Override
    public void visit(CreateTable create) {
        invokeCallback(MUTATION_COMMAND, "create-table");
        visit(create.getTable());
        if (create.getSelect() != null) {
            create.getSelect().accept((SelectVisitor) this);
        }
    }

    @Override
    public void visit(CreateView createView) {
        invokeCallback(MUTATION_COMMAND, "create-view");
    }

    @Override
    public void visit(Alter alter) {
        invokeCallback(MUTATION_COMMAND, "alter-table");
    }

    @Override
    public void visit(Statements stmts) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(Execute execute) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(SetStatement set) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(ResetStatement reset) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(ShowColumnsStatement set) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(ShowIndexStatement showIndex) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(RowConstructor<?> rowConstructor) {
        for (Expression expr : rowConstructor) {
            expr.accept(this);
        }
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        rowGetExpression.getExpression().accept(this);
    }

    @Override
    public void visit(HexValue hexValue) {

    }

    @Override
    public void visit(Merge merge) {
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
    }

    @Override
    public void visit(OracleHint hint) {

    }

    @Override
    public void visit(TableFunction tableFunction) {
        visit(tableFunction.getFunction());
    }

    @Override
    public void visit(AlterView alterView) {
        invokeCallback(MUTATION_COMMAND, "alter-view");
    }

    @Override
    public void visit(RefreshMaterializedViewStatement materializedView) {
        visit(materializedView.getView());
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {

    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {

    }

    @Override
    public void visit(Commit commit) {

    }

    @Override
    public void visit(Upsert upsert) {
        visit(upsert.getTable());
        if (upsert.getExpressions() != null) {
            upsert.getExpressions().accept(this);
        }
        if (upsert.getSelect() != null) {
            visit(upsert.getSelect());
        }
    }

    @Override
    public void visit(UseStatement use) {

    }

    @Override
    public void visit(ParenthesedFromItem parenthesis) {
        parenthesis.getFromItem().accept(this);
        // support join keyword in fromItem
        visitJoins(parenthesis.getJoins());
    }

    @Override
    public void visit(GroupByElement element) {
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
    public void visit(Block block) {
        if (block.getStatements() != null) {
            visit(block.getStatements());
        }
    }

    @Override
    public void visit(Comment comment) {
        if (comment.getTable() != null) {
            visit(comment.getTable());
        }
        if (comment.getColumn() != null) {
            Table table = comment.getColumn().getTable();
            if (table != null) {
                visit(table);
            }
        }
    }

    @Override
    public void visit(Values values) {
        values.getExpressions().accept(this);
    }

    @Override
    public void visit(DescribeStatement describe) {
        describe.getTable().accept(this);
    }

    @Override
    public void visit(ExplainStatement explain) {
        if (explain.getStatement() != null) {
            explain.getStatement().accept((StatementVisitor) this);
        }
    }

    @Override
    public void visit(NextValExpression nextVal) {

    }

    @Override
    public void visit(CollateExpression col) {
        col.getLeftExpression().accept(this);
    }

    @Override
    public void visit(ShowStatement aThis) {

    }

    @Override
    public void visit(SimilarToExpression expr) {
        visitBinaryExpression(expr);
    }

    @Override
    public void visit(DeclareStatement aThis) {

    }

    @Override
    public void visit(Grant grant) {
        invokeCallback(MUTATION_COMMAND, "grant");
    }

    @Override
    public void visit(ArrayExpression array) {
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
    }

    @Override
    public void visit(ArrayConstructor array) {
        for (Expression expression : array.getExpressions()) {
            expression.accept(this);
        }
    }

    @Override
    public void visit(CreateSequence createSequence) {
        invokeCallback(MUTATION_COMMAND, "create-sequence");
    }

    @Override
    public void visit(AlterSequence alterSequence) {
        invokeCallback(MUTATION_COMMAND, "alter-sequence");
    }

    @Override
    public void visit(CreateFunctionalStatement createFunctionalStatement) {
        invokeCallback(MUTATION_COMMAND, "create-function");
    }

    @Override
    public void visit(ShowTablesStatement showTables) {
        throw new UnsupportedOperationException(
                "Reading from a ShowTablesStatement is not supported");
    }

    @Override
    public void visit(TSQLLeftJoin tsqlLeftJoin) {
        visitBinaryExpression(tsqlLeftJoin);
    }

    @Override
    public void visit(TSQLRightJoin tsqlRightJoin) {
        visitBinaryExpression(tsqlRightJoin);
    }

    @Override
    public void visit(VariableAssignment var) {
        var.getVariable().accept(this);
        var.getExpression().accept(this);
    }

    @Override
    public void visit(XMLSerializeExpr aThis) {

    }

    @Override
    public void visit(CreateSynonym createSynonym) {
        invokeCallback(MUTATION_COMMAND, "create-synonym");
    }

    @Override
    public void visit(TimezoneExpression aThis) {
        aThis.getLeftExpression().accept(this);
    }

    @Override
    public void visit(SavepointStatement savepointStatement) {}

    @Override
    public void visit(RollbackStatement rollbackStatement) {

    }

    @Override
    public void visit(AlterSession alterSession) {
        invokeCallback(MUTATION_COMMAND, "alter-session");
    }

    @Override
    public void visit(JsonAggregateFunction expression) {
        Expression expr = expression.getExpression();
        if (expr != null) {
            expr.accept(this);
        }

        expr = expression.getFilterExpression();
        if (expr != null) {
            expr.accept(this);
        }
    }

    @Override
    public void visit(JsonFunction expression) {
        for (JsonFunctionExpression expr : expression.getExpressions()) {
            expr.getExpression().accept(this);
        }
    }

    @Override
    public void visit(ConnectByRootOperator connectByRootOperator) {
        connectByRootOperator.getColumn().accept(this);
    }

    public void visit(IfElseStatement ifElseStatement) {
        pushContext(IF);
        ifElseStatement.getIfStatement().accept(this);
        popContext(); // IF
        if (ifElseStatement.getElseStatement() != null) {
            pushContext(ELSE);
            ifElseStatement.getElseStatement().accept(this);
            popContext(); // ELSE
        }
    }

    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        oracleNamedFunctionParameter.getExpression().accept(this);
    }

    @Override
    public void visit(RenameTableStatement renameTableStatement) {
        invokeCallback(MUTATION_COMMAND, "rename-table");
        for (Map.Entry<Table, Table> e : renameTableStatement.getTableNames()) {
            e.getKey().accept(this);
            e.getValue().accept(this);
        }
    }

    @Override
    public void visit(PurgeStatement purgeStatement) {
        invokeCallback(MUTATION_COMMAND, "purge");
        if (purgeStatement.getPurgeObjectType() == PurgeObjectType.TABLE) {
            ((Table) purgeStatement.getObject()).accept(this);
        }
    }

    @Override
    public void visit(AlterSystemStatement alterSystemStatement) {
        invokeCallback(MUTATION_COMMAND, "alter-system");
    }

    @Override
    public void visit(UnsupportedStatement unsupportedStatement) {
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        visitBinaryExpression(geometryDistance);
    }
}
