package com.metabase.macaw;

import com.metabase.macaw.AstWalker;
import com.metabase.macaw.AstWalker.CallbackKey;

import clojure.lang.IFn;

import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.*;
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

import static  com.metabase.macaw.AstWalker.CallbackKey.*;

public class TableExtractor<Acc> implements ExpressionVisitor, FromItemVisitor, GroupByVisitor,
       SelectItemVisitor, SelectVisitor, StatementVisitor {

    private AstWalker astWalker;

    public TableExtractor(Map<CallbackKey, IFn> rawCallbacks, Acc val) {
        this.astWalker = new AstWalker<Acc>(rawCallbacks, val);
    }

    public Expression walk(Expression expression) {
        expression.accept(this);
        return expression;
    }

    /**
     * Fold the given `statement`, using the callbacks to update the accumulator as appropriate.
     */
    public Acc fold(Statement statement) {
        maybeAcceptThis(statement);
        return (Acc)this.astWalker.getAcc();
    }


    /* <ExpressionVisitor> */

    private void visitBinaryExpression(BinaryExpression binaryExpression) {
        binaryExpression.getLeftExpression().accept(this);
        binaryExpression.getRightExpression().accept(this);
    }

    private void maybeAcceptThis(Expression expression) {
        if (expression != null) {
            expression.accept(this);
        }
    }

    private void maybeAcceptThis(FromItem fromItem) {
        if (fromItem != null) {
            fromItem.accept(this);
        }
    }

    private void maybeAcceptThis(GroupByElement element) {
        if (element != null) {
            element.accept(this);
        }
    }

    private void maybeAcceptThis(Statement statement) {
        if (statement != null) {
            statement.accept(this);
        }
    }

    @Override
    public void visit(Addition addition) {
        visitBinaryExpression(addition);
    }

    @Override
    public void visit(AllColumns allColumns) { }

    @Override
    public void visit(AllTableColumns allTableColumns) { }

    @Override
    public void visit(AllValue allValue) { }

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

    @Override
    public void visit(AndExpression andExpression) {
        visitBinaryExpression(andExpression);
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        anyComparisonExpression.getSelect().accept((ExpressionVisitor) this);
    }

    @Override
    public void visit(ArrayConstructor array) {
        for (Expression expression : array.getExpressions()) {
            expression.accept(this);
        }
    }

    @Override
    public void visit(ArrayExpression array) {
        array.getObjExpression().accept(this);
        maybeAcceptThis(array.getIndexExpression());
        maybeAcceptThis(array.getStartIndexExpression());
        maybeAcceptThis(array.getStopIndexExpression());
    }

    @Override
    public void visit(Between between) {
        between.getLeftExpression().accept(this);
        between.getBetweenExpressionStart().accept(this);
        between.getBetweenExpressionEnd().accept(this);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        visitBinaryExpression(bitwiseAnd);
    }

    @Override
    public void visit(BitwiseLeftShift expr) {
        visitBinaryExpression(expr);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        visitBinaryExpression(bitwiseOr);
    }

    @Override
    public void visit(BitwiseRightShift expr) {
        visitBinaryExpression(expr);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        visitBinaryExpression(bitwiseXor);
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        maybeAcceptThis(caseExpression.getSwitchExpression());
        if (caseExpression.getWhenClauses() != null) {
            for (WhenClause when : caseExpression.getWhenClauses()) {
                when.accept(this);
            }
        }
        maybeAcceptThis(caseExpression.getElseExpression());
    }

    @Override
    public void visit(CastExpression cast) {
        cast.getLeftExpression().accept(this);
    }

    @Override
    public void visit(CollateExpression col) {
        col.getLeftExpression().accept(this);
    }

    @Override
    public void visit(Column tableColumn) {
        // NOT doing anything since either:
        // * we have an alias, which we don't want
        // * we have a real table name, but it'll surely show up elsewhere

    }

    @Override
    public void visit(Concat concat) {
        visitBinaryExpression(concat);
    }

    @Override
    public void visit(ConnectByRootOperator aThis) {
        // not visiting the column
    }

    @Override
    public void visit(ContainedBy containedBy) {
        visitBinaryExpression(containedBy);
    }

    @Override
    public void visit(Contains contains) {
        visitBinaryExpression(contains);
    }

    @Override
    public void visit(DateTimeLiteralExpression literal) { }

    @Override
    public void visit(DateValue dateValue) { }

    @Override
    public void visit(Division division) {
        visitBinaryExpression(division);
    }

    @Override
    public void visit(DoubleAnd doubleAnd) {
        visitBinaryExpression(doubleAnd);
    }

    @Override
    public void visit(DoubleValue doubleValue) { }

    @Override
    public void visit(EqualsTo equalsTo) {
        visitBinaryExpression(equalsTo);
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        existsExpression.getRightExpression().accept(this);
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
    public void visit(ExtractExpression eexpr) {
        maybeAcceptThis(eexpr);
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) { }

    @Override
    public void visit(Function function) {
        ExpressionList<?> exprList = function.getParameters();
        if (exprList != null) {
            visit(exprList);
        }
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        visitBinaryExpression(geometryDistance);
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
    public void visit(HexValue hexValue) { }

    @Override
    public void visit(InExpression inExpression) {
        inExpression.getLeftExpression().accept(this);
        inExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(IntegerDivision division) {
        visitBinaryExpression(division);
    }

    @Override
    public void visit(IntervalExpression iexpr) {
        maybeAcceptThis(iexpr.getExpression());
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) { }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        visitBinaryExpression(isDistinctExpression);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) { }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) { }

    @Override
    public void visit(JdbcParameter jdbcParameter) { }

    @Override
    public void visit(JsonAggregateFunction aggregate) {
        maybeAcceptThis(aggregate.getExpression());
        maybeAcceptThis(aggregate.getFilterExpression());
    }

    @Override
    public void visit(JsonExpression jsonExpr) {
        maybeAcceptThis(jsonExpr.getExpression());
    }

    @Override
    public void visit(JsonFunction expression) {
        //TODO: just abort?
        for (JsonFunctionExpression e : expression.getExpressions()) {
            e.getExpression().accept(this);
        }
    }

    @Override
    public void visit(JsonOperator jsonExpr) {
        visitBinaryExpression(jsonExpr);
    }

    @Override
    public void visit(KeepExpression aexpr) { }

    @Override
    public void visit(LikeExpression likeExpression) {
        visitBinaryExpression(likeExpression);
    }

    @Override
    public void visit(LongValue longValue) { }

    @Override
    public void visit(Matches matches) {
        visitBinaryExpression(matches);
    }

    @Override
    public void visit(MemberOfExpression memberOfExpression) {
        memberOfExpression.getLeftExpression().accept(this);
        memberOfExpression.getRightExpression().accept(this);
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
    public void visit(Modulo modulo) {
        visitBinaryExpression(modulo);
    }

    @Override
    public void visit(Multiplication multiplication) {
        visitBinaryExpression(multiplication);
    }

    @Override
    public void visit(MySQLGroupConcat groupConcat) { }

    @Override
    public void visit(NextValExpression aThis) { }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        visitBinaryExpression(notEqualsTo);
    }

    @Override
    public void visit(NotExpression notExpr) {
        notExpr.getExpression().accept(this);
    }

    @Override
    public void visit(NullValue nullValue) { }

    @Override
    public void visit(NumericBind bind) { }

    @Override
    public void visit(OracleHierarchicalExpression oexpr) {
        maybeAcceptThis(oexpr.getStartExpression());
        maybeAcceptThis(oexpr.getConnectExpression());
    }

    @Override
    public void visit(OracleHint hint) { }

    @Override
    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        oracleNamedFunctionParameter.getExpression().accept(this);
    }

    @Override
    public void visit(OrExpression orExpression) {
        visitBinaryExpression(orExpression);
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        overlapsCondition.getLeft().accept(this);
        overlapsCondition.getRight().accept(this);
    }

    @Override
    public void visit(ParenthesedSelect selectBody) {
        //TODO: might want to keep track of pseudo-tables created here via alias
        List<WithItem> withItemsList = selectBody.getWithItemsList();
        if (withItemsList != null) {
            for (WithItem withItem : withItemsList) {
                this.visit(withItem);
            }
        }
        selectBody.getSelect().accept((SelectVisitor) this);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
        rangeExpression.getStartExpression().accept(this);
        rangeExpression.getEndExpression().accept(this);
    }

    @Override
    public void visit(RegExpMatchOperator rexpr) {
        visitBinaryExpression(rexpr);
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
    public void visit(Select selectBody) {
        List<WithItem> withItemsList = selectBody.getWithItemsList();
        if (withItemsList != null) {
            for (WithItem withItem : withItemsList) {
                this.visit(withItem);
            }
        }
        selectBody.accept((SelectVisitor) this);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        signedExpression.getExpression().accept(this);
    }

    @Override
    public void visit(SimilarToExpression expr) {
        visitBinaryExpression(expr);
    }

    @Override
    public void visit(StringValue stringValue) { }

    @Override
    public void visit(Subtraction subtraction) {
        visitBinaryExpression(subtraction);
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) { }

    @Override
    public void visit(TimestampValue timestampValue) { }

    @Override
    public void visit(TimeValue timeValue) { }

    @Override
    public void visit(TimezoneExpression aThis) {
        aThis.getLeftExpression().accept(this);
    }

    @Override
    public void visit(TranscodingFunction transcodingFunction) {
        transcodingFunction.getExpression().accept(this);
    }

    @Override
    public void visit(TrimFunction trimFunction) {
        maybeAcceptThis(trimFunction.getExpression());
        maybeAcceptThis(trimFunction.getFromExpression());
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
    public void visit(UserVariable var) { }

    @Override
    public void visit(VariableAssignment var) {
        var.getVariable().accept(this);
        var.getExpression().accept(this);
    }

    @Override
    public void visit(WhenClause whenClause) {
        maybeAcceptThis(whenClause.getWhenExpression());
        maybeAcceptThis(whenClause.getThenExpression());

    }

    @Override
    public void visit(XMLSerializeExpr aThis) { }

    @Override
    public void visit(XorExpression xorExpression) {
        visitBinaryExpression(xorExpression);
    }

    /* </ExpressionVisitor> */

    /* <SelectVisitor> */

    @Override
    public void visit(PlainSelect plainSelect) {
        List<WithItem> withItemsList = plainSelect.getWithItemsList();
        if (withItemsList != null) {
            for (WithItem withItem : withItemsList) {
                withItem.accept((SelectVisitor) this);
            }
        }
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                item.accept(this);
            }
        }

        maybeAcceptThis(plainSelect.getFromItem());
        maybeAcceptThis(plainSelect.getWhere());
        maybeAcceptThis(plainSelect.getHaving());
        maybeAcceptThis(plainSelect.getOracleHierarchical());
        maybeAcceptThis(plainSelect.getGroupBy());
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                join.getFromItem().accept(this);
                join.getRightItem().accept(this);
                for (Expression expression : join.getOnExpressions()) {
                    expression.accept(this);
                }
            }
        }
    }

    @Override
    public void visit(SetOperationList setOpList) {

    }

    @Override
    public void visit(WithItem withItem) {

    }

    @Override
    public void visit(Values aThis) {

    }

    @Override
    public void visit(LateralSubSelect lateralSubSelect) {
        lateralSubSelect.getSelect().accept((SelectVisitor) this);
    }

    @Override
    public void visit(TableStatement tableStatement) {

    }

    /* </SelectVisitor> */

    /* <FromItemVisitor> */

    @Override
    public void visit(Table table) {
        System.out.println("Found table: " + table.toString());
        this.astWalker.invokeCallback(TABLE, table);
    }

    @Override
    public void visit(ParenthesedFromItem parenthesis) {
        parenthesis.getFromItem().accept(this);
        List<Join> joins = parenthesis.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                join.getFromItem().accept(this);
                join.getRightItem().accept(this);
                for (Expression expression : join.getOnExpressions()) {
                    expression.accept(this);
                }
            }
        }
    }

    public void visit(TableFunction tableFunction) {
        visit(tableFunction.getFunction());
    }

    /* </FromItemVisitor> */

    /* <StatementVisitor> */
    @Override
    public void visit(Alter alter) {
    }

    @Override
    public void visit(AlterSequence alterSequence) {
    }

    @Override
    public void visit(AlterSession alterSession) {
    }

    @Override
    public void visit(AlterSystemStatement alterSystemStatement) {
    }

    @Override
    public void visit(AlterView alterView) {
    }

    @Override
    public void visit(Analyze analyze) {
    }

    @Override
    public void visit(Block block) {
    }

    @Override
    public void visit(Comment comment) {
    }

    @Override
    public void visit(Commit commit) {
    }

    @Override
    public void visit(CreateFunctionalStatement createFunctionalStatement) {
    }

    @Override
    public void visit(CreateIndex createIndex) {
    }

    @Override
    public void visit(CreateSchema aThis) {
    }

    @Override
    public void visit(CreateSequence createSequence) {
    }

    @Override
    public void visit(CreateSynonym createSynonym) {
    }

    @Override
    public void visit(CreateTable createTable) {
    }

    @Override
    public void visit(CreateView createView) {
    }

    @Override
    public void visit(DeclareStatement aThis) {
    }

    @Override
    public void visit(Delete delete) {
    }

    @Override
    public void visit(DescribeStatement describe) {
    }

    @Override
    public void visit(Drop drop) {
    }

    @Override
    public void visit(Execute execute) {
    }

    @Override
    public void visit(ExplainStatement aThis) {
    }

    @Override
    public void visit(Grant grant) {
    }

    @Override
    public void visit(IfElseStatement aThis) {
    }

    @Override
    public void visit(Insert insert) {
    }

    @Override
    public void visit(Merge merge) {
    }

    @Override
    public void visit(PurgeStatement purgeStatement) {
    }

    @Override
    public void visit(RefreshMaterializedViewStatement materializedView) {
    }

    @Override
    public void visit(RenameTableStatement renameTableStatement) {
    }

    @Override
    public void visit(ResetStatement reset) {
    }

    @Override
    public void visit(RollbackStatement rollbackStatement) {
    }

    @Override
    public void visit(SavepointStatement savepointStatement) {
    }

    @Override
    public void visit(SetStatement set) {
    }

    @Override
    public void visit(ShowColumnsStatement set) {
    }

    @Override
    public void visit(ShowIndexStatement showIndex) {
    }

    @Override
    public void visit(ShowStatement aThis) {
    }

    @Override
    public void visit(ShowTablesStatement showTables) {
    }

    @Override
    public void visit(Statements stmts) {
    }

    @Override
    public void visit(Truncate truncate) {
    }

    @Override
    public void visit(UnsupportedStatement unsupportedStatement) {
    }

    @Override
    public void visit(Update update) {
    }

    @Override
    public void visit(Upsert upsert) {
    }

    @Override
    public void visit(UseStatement use) {
    }
    /* </StatementVisitor> */

    /* <SelectItemVisitor> */

    @Override
    public void visit(SelectItem item) {
        // TODO: ignoring aliases
        item.getExpression().accept(this);
    }
    /* </SelectItemVisitor> */

    /* <GroupByVisitor> */
    @Override
    public void visit(GroupByElement element) {
        element.getGroupByExpressionList().accept(this);
        for (ExpressionList<?> exprList : element.getGroupingSets()) {
            exprList.accept(this);
        }
    }
    /* </GroupByVisitor> */
}
