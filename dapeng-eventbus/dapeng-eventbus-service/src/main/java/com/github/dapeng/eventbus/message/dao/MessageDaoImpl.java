package com.github.dapeng.eventbus.message.dao;

import com.github.dapeng.eventbus.message.EventStore;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author maple.lei
 */
public class MessageDaoImpl extends JdbcDaoSupport implements IMessageDao {


    @Override
    public List<EventStore> listMessages() {

        final String querySQl = "SELECT * FROM common_event";
        List<EventStore> eventInfos = this.getJdbcTemplate().query(querySQl, new MessageRowMapper());
        return eventInfos;

    }


    @Override
    public int saveMessageToDB(String eventType, byte[] event) {
        final String executeSql = "INSERT INTO  common_event set event_type=?, event_binary=?";
        int result = this.getJdbcTemplate().update(executeSql, eventType, event);
        return result;
    }

    @Override
    public void deleteMessage(Long eventId) {

    }

    @Override
    public void deleteBatchMessage(List<EventStore> eventStores) {
        final String executeSql = "DELETE FROM common_event WHERE id = ?";
        eventStores.forEach(eventStore -> {
            this.getJdbcTemplate().update(executeSql, eventStore.getId());
        });
    }


    class MessageRowMapper implements RowMapper<EventStore> {

        @Override
        public EventStore mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventStore store = new EventStore();
            store.setId(rs.getLong("id"));
            store.setEventType(rs.getString("event_type"));
            store.setEventBinary(rs.getBytes("event_binary"));
            store.setUpdateAt(rs.getTimestamp("updated_at"));
            return store;
        }
    }
}


