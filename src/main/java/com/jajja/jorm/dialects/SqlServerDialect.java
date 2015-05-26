package com.jajja.jorm.dialects;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.jajja.jorm.Query;
import com.jajja.jorm.Record;
import com.jajja.jorm.Symbol;
import com.jajja.jorm.Table;
import com.jajja.jorm.Record.ResultMode;

public class SqlServerDialect extends Dialect {
    private static final HashMap<Integer, ExceptionType> exceptionMap = new HashMap<Integer, ExceptionType>();
    private final boolean isOutputSupported;

    static {
        //mssql.addError(547, ExceptionType.FOREIGN_KEY_VIOLATION);       // %ls statement conflicted with %ls %ls constraint '%.*ls'. The conflict occurred in database '%.*ls', table '%.*ls'%ls%.*ls%ls.
        exceptionMap.put(2601, ExceptionType.UNIQUE_VIOLATION);     // Cannot insert duplicate key row in object '%.*ls' with unique index '%.*ls'.
        exceptionMap.put(2627, ExceptionType.UNIQUE_VIOLATION);     // Violation of %ls constraint '%.*ls'. Cannot insert duplicate key in object '%.*ls'.
        exceptionMap.put(547, ExceptionType.CHECK_VIOLATION);       // %ls statement conflicted with %ls %ls constraint '%.*ls'. The conflict occurred in database '%.*ls', table '%.*ls'%ls%.*ls%ls.
        exceptionMap.put(1205, ExceptionType.DEADLOCK_DETECTED);    // Transaction (Process ID %d) was deadlocked on {%Z} resources with another process and has been chosen as the deadlock victim. Rerun the transaction.
        exceptionMap.put(1222, ExceptionType.LOCK_TIMEOUT);         // Lock request time out period exceeded.
    }

    SqlServerDialect(String database, Connection connection) throws SQLException {
        super(database, connection);

        DatabaseMetaData metaData = connection.getMetaData();
        isOutputSupported = metaData.getDatabaseMajorVersion() >= 2006;

        feature(Feature.BATCH_INSERTS);
    }

    @Override
    public ReturnSetSyntax getReturnSetSyntax() {
        return isOutputSupported ? ReturnSetSyntax.OUTPUT : ReturnSetSyntax.NONE;
    }

    private final static Pattern sqlServerForeignKeyPattern = Pattern.compile("^The [A-Z ]+ statement conflicted with the FOREIGN KEY constraint");

    @Override
    public ExceptionType getExceptionType(SQLException sqlException) {
        ExceptionType type = exceptionMap.get(sqlException.getErrorCode());
        if (ExceptionType.CHECK_VIOLATION.equals(type)) {
            if (sqlServerForeignKeyPattern.matcher(sqlException.getMessage()).matches()) {
                type = ExceptionType.FOREIGN_KEY_VIOLATION;
            }
        }
        return type != null ? type : ExceptionType.UNKNOWN;
    }

    @Override
    public String getNowFunction() {
        return "getdate()";
    }

    @Override
    public String getNowQuery() {
        return "SELECT getdate()";
    }

    @Override
    protected void appendInsertHead(Query query, Table table, Set<Symbol> symbols, List<Record> records, ResultMode mode) {
        query.append(" OUTPUT");
        if (mode == ResultMode.ID_ONLY) {
            boolean isFirst = true;
            for (Symbol symbol : table.getPrimaryKey().getSymbols()) {
                if (isFirst) {
                    isFirst = false;
                    query.append(" INSERTED.#1#", symbol);
                } else {
                    query.append(", INSERTED.#1#", symbol);
                }
            }
        } else {
            query.append(" INSERTED.*");
        }
    }

    @Override
    public int getMaxParameterMarkers() {
        return 2500;
    }

}
