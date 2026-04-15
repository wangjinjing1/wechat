package com.luisa.wechat.chat.persistence;

import com.luisa.wechat.chat.persistence.model.ChatMessageRecord;
import com.luisa.wechat.chat.persistence.model.MessageRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper {

    @Insert("""
            INSERT INTO chat_messages (
                conversation_key, scope, sender_id, group_id, content_type, content, file_name, file_url, created_at
            ) VALUES (
                #{conversationKey}, #{scope}, #{senderId}, #{groupId}, #{contentType}, #{content}, #{fileName}, #{fileUrl}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessageRecord message);

    @Select("""
            SELECT page.id,
                   page.sender_id,
                   page.sender_name,
                   page.sender_avatar_url,
                   page.content_type,
                   page.content,
                   page.file_name,
                   page.file_url,
                   page.created_at
            FROM (
                SELECT m.id,
                       m.sender_id,
                       u.display_name AS sender_name,
                       u.avatar_url AS sender_avatar_url,
                       m.content_type,
                       m.content,
                       m.file_name,
                       m.file_url,
                       m.created_at
                FROM chat_messages m
                JOIN users u ON u.id = m.sender_id
                WHERE m.conversation_key = #{conversationKey}
                  AND (#{beforeId} IS NULL OR m.id < #{beforeId})
                ORDER BY m.id DESC
                LIMIT #{limit}
            ) page
            ORDER BY page.id ASC
            """)
    List<MessageRow> listMessages(@Param("conversationKey") String conversationKey,
                                  @Param("beforeId") Long beforeId,
                                  @Param("limit") int limit);

    @Select("""
            SELECT id
            FROM chat_messages
            WHERE conversation_key = #{conversationKey}
            ORDER BY id DESC
            LIMIT 1
            """)
    Long findLastMessageId(@Param("conversationKey") String conversationKey);

    @Select("""
            SELECT COUNT(1)
            FROM chat_messages
            WHERE conversation_key = #{conversationKey}
              AND id > #{lastReadMessageId}
              AND sender_id <> #{userId}
            """)
    int countUnread(@Param("conversationKey") String conversationKey,
                    @Param("lastReadMessageId") Long lastReadMessageId,
                    @Param("userId") Long userId);
}
