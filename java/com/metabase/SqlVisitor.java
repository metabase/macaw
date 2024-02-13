package com.metabase.macaw;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

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
