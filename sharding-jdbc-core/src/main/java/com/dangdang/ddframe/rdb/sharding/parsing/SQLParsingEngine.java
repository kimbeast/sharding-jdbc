/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.parsing;

import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.constant.DatabaseType;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Symbol;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.SQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.SQLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.mysql.MySQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.oracle.OracleParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.postgresql.PostgreSQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.sqlserver.SQLServerParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.delete.DeleteParserFactory;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.insert.InsertParserFactory;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.select.SelectParserFactory;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.update.UpdateParserFactory;
import lombok.RequiredArgsConstructor;

/**
 * SQL解析引擎.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class SQLParsingEngine {
    
    private final DatabaseType dbType;
    
    private final String sql;
    
    private final ShardingRule shardingRule;
    
    /**
     * 解析SQL.
     * 
     * @return SQL语句对象
     */
    public SQLStatement parse() {
        SQLParser sqlParser = getSQLParser();
        sqlParser.skipIfEqual(Symbol.SEMI);
        if (sqlParser.equalAny(DefaultKeyword.WITH)) {
            skipWith(sqlParser);
        }
        if (sqlParser.equalAny(DefaultKeyword.SELECT)) {
            return SelectParserFactory.newInstance(sqlParser).parse();
        }
        if (sqlParser.equalAny(DefaultKeyword.INSERT)) {
            return InsertParserFactory.newInstance(shardingRule, sqlParser).parse();
        }
        if (sqlParser.equalAny(DefaultKeyword.UPDATE)) {
            return UpdateParserFactory.newInstance(sqlParser).parse();
        }
        if (sqlParser.equalAny(DefaultKeyword.DELETE)) {
            return DeleteParserFactory.newInstance(sqlParser).parse();
        }
        throw new SQLParsingUnsupportedException(sqlParser.getLexer().getCurrentToken().getType());
    }
    
    private SQLParser getSQLParser() {
        switch (dbType) {
            case H2:
            case MySQL:
                return new MySQLParser(sql, shardingRule);
            case Oracle:
                return new OracleParser(sql, shardingRule);
            case SQLServer:
                return new SQLServerParser(sql, shardingRule);
            case PostgreSQL:
                return new PostgreSQLParser(sql, shardingRule);
            default:
                throw new UnsupportedOperationException(dbType.name());
        }
    }
    
    private void skipWith(final SQLParser sqlParser) {
        sqlParser.getLexer().nextToken();
        do {
            sqlParser.skipUntil(DefaultKeyword.AS);
            sqlParser.accept(DefaultKeyword.AS);
            sqlParser.skipParentheses();
        } while (sqlParser.skipIfEqual(Symbol.COMMA));
    }
}
