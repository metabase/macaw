 OPEN ccur FOR
    'select c.category
     from ' || TABLE_NAME || ' c
     where c.deptid=' || PI_N_Dept ||
     ' and c.category not in ('|| sExcludeCategories ||')';
