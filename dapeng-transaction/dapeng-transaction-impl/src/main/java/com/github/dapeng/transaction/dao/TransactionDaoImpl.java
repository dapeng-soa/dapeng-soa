package com.github.dapeng.transaction.dao;

import com.github.dapeng.transaction.api.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;
import java.util.List;

/**
 * Created by tangliu on 17/7/28.
 */
public class TransactionDaoImpl extends JdbcDaoSupport implements ITransactionDao {


    /**
     * 插入记录，返回id
     *
     * @param g
     * @return
     */
    @Override
    public Integer insert(TGlobalTransaction g) {

        final String strSql = "insert into global_transactions(status, curr_sequence, created_at, created_by, updated_by) values(?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.getJdbcTemplate().update(conn -> {
            int i = 0;
            PreparedStatement ps = conn.prepareStatement(strSql);
            ps = conn.prepareStatement(strSql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(++i, g.getStatus().getValue());
            ps.setInt(++i, g.getCurrSequence());
            ps.setTimestamp(++i, new Timestamp(g.getCreatedAt() == null ? new java.util.Date().getTime() : g.getCreatedAt().getTime()));
            ps.setInt(++i, g.getCreatedBy());
            ps.setInt(++i, g.getCreatedBy());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * 插入子事务过程记录，返回id
     *
     * @param gp
     * @return
     */
    @Override
    public Integer insert(TGlobalTransactionProcess gp) {

        final String strSql = "insert into global_transaction_process(transaction_id,transaction_sequence,status,expected_status,service_name,version_name,method_name,rollback_method_name," +
                "request_json, response_json,redo_times,next_redo_time, created_by, updated_by,created_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.getJdbcTemplate().update(conn -> {
            int i = 0;
            PreparedStatement ps = conn.prepareStatement(strSql);
            ps = conn.prepareStatement(strSql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(++i, gp.getTransactionId());
            ps.setInt(++i, gp.getTransactionSequence());
            ps.setInt(++i, gp.getStatus().getValue());
            ps.setInt(++i, gp.getExpectedStatus().getValue());
            ps.setString(++i, gp.getServiceName());
            ps.setString(++i, gp.getVersionName());
            ps.setString(++i, gp.getMethodName());
            ps.setString(++i, gp.getRollbackMethodName());
            ps.setString(++i, gp.getRequestJson());
            ps.setString(++i, gp.getResponseJson());
            ps.setInt(++i, gp.getRedoTimes());
            ps.setTimestamp(++i, new Timestamp(gp.getNextRedoTime().getTime()));
            ps.setInt(++i, gp.getCreatedBy() == null ? 0 : gp.getCreatedBy());
            ps.setInt(++i, gp.getUpdatedBy() == null ? 0 : gp.getUpdatedBy());
            ps.setTimestamp(++i, new Timestamp(gp.getCreatedAt() == null ? new java.util.Date().getTime() : gp.getCreatedAt().getTime()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }


    @Override
    public TGlobalTransaction getGlobalByIdForUpdate(Integer id) {

        return this.getJdbcTemplate().queryForObject("select * from global_transactions where id=? for update", new Object[]{id}, new int[]{Types.INTEGER}, new GlobalTransactionMapper());
    }

    @Override
    public TGlobalTransactionProcess getProcessByIdForUpdate(Integer id) {

        return this.getJdbcTemplate().queryForObject("select * from global_transaction_process where id=? for update", new Object[]{id}, new int[]{Types.INTEGER}, new GlobalTransactionProcessMapper());
    }

    /**
     * 查找所有的失败的或者未知的事务过程记录
     * 升序
     *
     * @param transactionId
     * @return
     */
    @Override
    public List<TGlobalTransactionProcess> findFailedProcess(Integer transactionId) {
        return this.getJdbcTemplate().query("select * from global_transaction_process where transaction_id=? and (status=3 or status=4) order by transaction_sequence asc", new Object[]{transactionId}, new GlobalTransactionProcessMapper());
    }

    /**
     * 查找所有的成功的或者未知的事务过程记录
     *
     * @param transactionId
     * @return
     */
    @Override
    public List<TGlobalTransactionProcess> findSucceedProcess(Integer transactionId) {
        return this.getJdbcTemplate().query("select * from global_transaction_process where transaction_id=? and (status=2 or status=4) order by transaction_sequence desc", new Object[]{transactionId}, new GlobalTransactionProcessMapper());
    }

    /**
     * 查询所有失败，或者部分回滚的全局事务记录
     *
     * @return
     */
    @Override
    public List<TGlobalTransaction> findFailedGlobals() {
        return this.getJdbcTemplate().query("select * from global_transactions where status=3 or status=5", new GlobalTransactionMapper());
    }


    /**
     * 查找所有状态为成功，但子过程中有失败的全局事务记录
     */
    @Override
    public List<TGlobalTransaction> findSuccessWithFailedProcessGlobals() {
        return this.getJdbcTemplate().query("select g.* from global_transactions g INNER JOIN global_transaction_process p ON g.id = p.transaction_id where g.status=2 and (p.status=3 or p.status=4) GROUP BY g.id", new GlobalTransactionMapper());
    }

    @Override
    public void updateProcessRollbackTime(Integer id, Integer redoTimes, java.util.Date nextRedoTime) {
        this.getJdbcTemplate().update("update global_transaction_process set redo_times = ?, next_redo_time=? where id=?",
                new Object[]{redoTimes, nextRedoTime, id}, new int[]{Types.INTEGER, Types.TIMESTAMP, Types.INTEGER});
    }


    /**
     * update global_transactions
     * set
     * status = ${status.getValue},
     * curr_sequence = ${currSequence},
     * updated_at = ${updatedAt}
     * where id = ${transactionId}
     **/
    @Override
    public void updateGlobalTransactionStatusAndCurrSeq(Integer status, Integer currSequence, Integer id) {
        this.getJdbcTemplate().update("update global_transactions set status=?, curr_sequence=? where id=?", new Object[]{status, currSequence, id}, new int[]{Types.INTEGER, Types.INTEGER, Types.INTEGER});
    }

    /**
     * update global_transaction_process
     * set
     * status = ${status.getValue},
     * response_json = ${responseJson},
     * updated_at = ${updatedAt}
     * where id = ${processId}
     *
     * @param id
     * @param status
     * @param response
     */
    @Override
    public void updateProcess(Integer id, Integer status, String response) {
        this.getJdbcTemplate().update("update global_transaction_process set status=?, response_json=? where id=?", new Object[]{status, response, id}, new int[]{Types.INTEGER, Types.VARCHAR, Types.INTEGER});
    }


    /**
     * 更新事务过程的期望状态
     *
     * @param id
     * @param expectedStatus
     */
    @Override
    public void updateProcessExpectedStatus(Integer id, Integer expectedStatus) {
        this.getJdbcTemplate().update("update global_transaction_process set expected_status=? where id=?", new Object[]{expectedStatus, id}, new int[]{Types.INTEGER, Types.INTEGER});
    }


    class GlobalTransactionMapper implements RowMapper<TGlobalTransaction> {

        @Override
        public TGlobalTransaction mapRow(ResultSet rs, int i) throws SQLException {
            TGlobalTransaction g = new TGlobalTransaction();
            g.setId(rs.getInt("id"));
            g.setCurrSequence(rs.getInt("curr_sequence"));
            g.setCreatedBy(rs.getInt("created_by"));
            g.setCreatedAt(rs.getDate("created_at"));
            g.setStatus(TGlobalTransactionsStatus.findByValue(rs.getInt("status")));
            g.setUpdatedAt(rs.getDate("updated_at"));
            g.setUpdatedBy(rs.getInt("updated_by"));
            return g;
        }
    }


    class GlobalTransactionProcessMapper implements RowMapper<TGlobalTransactionProcess> {

        @Override
        public TGlobalTransactionProcess mapRow(ResultSet rs, int i) throws SQLException {
            TGlobalTransactionProcess gp = new TGlobalTransactionProcess();
            gp.setId(rs.getInt("id"));
            gp.setUpdatedAt(rs.getTimestamp("updated_at"));
            gp.setCreatedBy(rs.getInt("created_by"));
            gp.setCreatedAt(rs.getTimestamp("created_at"));
            gp.setExpectedStatus(TGlobalTransactionProcessExpectedStatus.findByValue(rs.getInt("expected_status")));
            gp.setMethodName(rs.getString("method_name"));
            gp.setNextRedoTime(rs.getTimestamp("next_redo_time"));
            gp.setRedoTimes(rs.getInt("redo_times"));
            gp.setRequestJson(rs.getString("request_json"));
            gp.setResponseJson(rs.getString("response_json"));
            gp.setRollbackMethodName(rs.getString("rollback_method_name"));
            gp.setServiceName(rs.getString("service_name"));
            gp.setStatus(TGlobalTransactionProcessStatus.findByValue(rs.getInt("status")));
            gp.setTransactionId(rs.getInt("transaction_id"));
            gp.setTransactionSequence(rs.getInt("transaction_sequence"));
            gp.setUpdatedBy(rs.getInt("updated_by"));
            gp.setVersionName(rs.getString("version_name"));

            return gp;
        }
    }

}
