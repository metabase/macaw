package com.metabase.macaw;

import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.TableStatement;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;

//FromItemVisitor
//import net.sf.jsqlparser.statement.select.FromItemVisitor;

//ExpressionVisitor
//no import

//SelectItemVisitor
//import net.sf.jsqlparser.statement.select.SelectItemVisitor;

//StatementVisitor
//import net.sf.jsqlparser.statement.StatementVisitor;

//GroupByVisitor
// import net.sf.jsqlparser.statement.select.GroupByVisitor;


public class TableExtractor implements SelectVisitor {



    public void visit(ParenthesedSelect parenthesedSelect) {

    }

    public void visit(PlainSelect plainSelect) {

    }

    public void visit(SetOperationList setOpList) {

    }

    public void visit(WithItem withItem) {

    }

    public void visit(Values aThis) {

    }

    public void visit(LateralSubSelect lateralSubSelect) {

    }

    public void visit(TableStatement tableStatement) {

    }
}
