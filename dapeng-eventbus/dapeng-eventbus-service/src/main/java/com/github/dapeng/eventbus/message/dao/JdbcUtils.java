package com.github.dapeng.eventbus.message.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.SmartDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年02月28日 下午12:00
 */
public class JdbcUtils {
    private static Logger logger = LoggerFactory.getLogger(JdbcUtils.class);
    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<Connection>();


    /**
     * fetch connection from a datasource
     *
     * @param dataSource
     * @return
     * @throws CannotGetJdbcConnectionException
     */
    public static Connection doGetConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
        try {
            Assert.notNull(dataSource, "No DataSource specified");
            // Else we either got no holder or an empty thread-bound holder here.
            logger.debug("Fetching JDBC Connection from DataSource");
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
        }
    }

    public static Connection getConnection(DataSource dataSource) {
        // 得到当前线程上绑定的连接
        Connection conn = connectionHolder.get();
        if (conn == null) {
            conn = doGetConnection(dataSource);
            connectionHolder.set(conn);
        }
        return conn;
    }


    public static void releaseConnection(Connection con, DataSource dataSource) {
        try {
            if (con == null) {
                return;
            }
            logger.debug("Returning JDBC Connection to DataSource");
            con.close();
        } catch (SQLException ex) {
            logger.debug("Could not close JDBC Connection", ex);
        } catch (Throwable ex) {
            logger.debug("Unexpected exception on closing JDBC Connection", ex);
        }
    }

}
