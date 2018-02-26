package com.github.dapeng.message.event.dao;

import com.github.dapeng.message.event.task.EventStore;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import java.sql.*;
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
    public int deleteMessage(Long eventId) {
        final String executeSql = "DELETE FROM common_event WHERE id = ?";
        int result = this.getJdbcTemplate().update(executeSql, eventId);
        return result;
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


