package com.luisa.wechat.chat.mapper;

import com.luisa.wechat.chat.model.entity.ConversationStateRecord;
import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConversationStateMapper {

    @Select("""
            SELECT id, user_id, conversation_key, last_read_message_id, updated_at
            FROM conversation_states
            WHERE user_id = #{userId} AND conversation_key = #{conversationKey}
            """)
    ConversationStateRecord findByUserAndConversation(@Param("userId") Long userId, @Param("conversationKey") String conversationKey);

    @Insert("""
            INSERT INTO conversation_states (user_id, conversation_key, last_read_message_id, updated_at)
            VALUES (#{userId}, #{conversationKey}, #{lastReadMessageId}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConversationStateRecord state);

    @Update("""
            UPDATE conversation_states
            SET last_read_message_id = #{lastReadMessageId},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(@Param("id") Long id, @Param("lastReadMessageId") Long lastReadMessageId, @Param("updatedAt") Instant updatedAt);
}
