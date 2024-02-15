package com.metabase.macaw;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

/**
 * Clojure is not good at working with Java Visitors. They require over<em>riding</em> various over<em>loaded</em>
 * methods and, in the case of walking a tree (exactly what we want to do here) can be counted on to call `visit()`
 * recursively.
 *
 * Clojure's two main ways of dealing with this are `reify`, which does not permit overloading, and `proxy`, which does.
 * However, creating a proxy object creates a completely new object that does not inherit behavior defined in the parent
 * class. Therefore, if you have code like this:
 *
 * <code>
     (proxy
       [TablesNamesFinder]
       []
       (visit [visitable]
         (if (instance? Column visitable)
           (swap! columns conj (.getColumnName ^Column visitable))
           (let [^StatementVisitor this this]
             (proxy-super visit visitable)))))
   </code>

 * the call to `proxy-super` does <em>not</em> call `TablesNamesFinder.visit()` with the non-`Column` `visitable`; that
 * definition has been lost.
 *
 * <hr>
 *
 * Therefore, this interface was created to provide a more convenient escape hatch for Clojure. It removes the
 * overloading requirement for the conventional visitor pattern, instead providing differently-named methods for each
 * type. This lets Clojure code use `reify` to implement each method with the necessary behavior. The recursive
 * tree-walking is handled by a different class, which calls the methods defined here along the way. Think of them as
 * hooks for Clojure-land that don't affect the main behavior of the visitor.
 */
public interface SqlVisitor {

    /**
     * Called for every `Column` encountered, presumably for side effects.
     */
    public void visitColumn(Column column);

    /**
     * Called for every `Table` encountered, presumably for side effects.
     */
    public void visitTable(Table table);
}
