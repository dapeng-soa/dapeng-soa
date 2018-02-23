package com.github.dapeng.message.event.dao;

import com.github.dapeng.message.event.task.EventInfo;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author maple.lei
 */
public class MessageDaoImpl extends JdbcDaoSupport implements IMessageDao {

    @Override
    public List<EventInfo> listMessages() {

        final String querySQl = "SELECT * FROM common_event";
        List<EventInfo> eventInfos = this.getJdbcTemplate().query(querySQl, new MessageRowMapper());
        return eventInfos;

    }


    @Override
    public int saveMessageToDB(String eventType, byte[] event) {
        final String executeSql = "INSERT INTO  common_event set id = ? ,event_type=?, event_binary=?";
        Long eventId = Long.valueOf(new Random().nextInt(1000));
        int result = this.getJdbcTemplate().update(executeSql, eventId, eventType, event);

        return result;
    }

    @Override
    public int deleteMessage(Long eventId) {
        final String executeSql = "DELETE FROM common_event WHERE id = ?";
        int result = this.getJdbcTemplate().update(executeSql, eventId);
        return result;
    }


    class MessageRowMapper implements RowMapper<EventInfo> {

        @Override
        public EventInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventInfo info = new EventInfo();
            info.setId(rs.getLong("id"));
            info.setEventType(rs.getString("event_type"));
            info.setEventBinary(rs.getBytes("event_binary"));
            info.setUpdateAt(rs.getTimestamp("updated_at"));
            return info;
        }
    }
}


