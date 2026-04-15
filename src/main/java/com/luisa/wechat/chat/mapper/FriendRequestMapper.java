package com.luisa.wechat.chat.mapper;

import com.luisa.wechat.chat.model.entity.FriendRequestRow;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendRequestMapper {

    @Select("""
            SELECT id, requester_id, recipient_id, created_at
            FROM friend_requests
            WHERE requester_id = #{requesterId} AND recipient_id = #{recipientId}
            """)
    FriendRequestRow findByRequesterAndRecipient(@Param("requesterId") Long requesterId,
                                                 @Param("recipientId") Long recipientId);

    @Select("""
            SELECT fr.id,
                   fr.requester_id,
                   fr.recipient_id,
                   u.username AS requester_username,
                   u.display_name AS requester_display_name,
                   u.avatar_url AS requester_avatar_url,
                   fr.created_at
            FROM friend_requests fr
            JOIN users u ON u.id = fr.requester_id
            WHERE fr.recipient_id = #{recipientId}
            ORDER BY fr.created_at DESC
            """)
    List<FriendRequestRow> listIncoming(@Param("recipientId") Long recipientId);

    @Insert("""
            INSERT INTO friend_requests (requester_id, recipient_id, created_at)
            VALUES (#{requesterId}, #{recipientId}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendRequestRow request);

    @Delete("""
            DELETE FROM friend_requests
            WHERE id = #{id}
            """)
    int deleteById(@Param("id") Long id);

    @Delete("""
            DELETE FROM friend_requests
            WHERE requester_id = #{requesterId} AND recipient_id = #{recipientId}
            """)
    int deleteByRequesterAndRecipient(@Param("requesterId") Long requesterId,
                                      @Param("recipientId") Long recipientId);
}
