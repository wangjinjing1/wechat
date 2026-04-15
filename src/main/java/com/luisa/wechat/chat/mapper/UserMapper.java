package com.luisa.wechat.chat.mapper;

import com.luisa.wechat.chat.model.entity.UserRecord;
import com.luisa.wechat.chat.model.entity.UserSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, display_name, password_hash, avatar_url, created_at
            FROM users
            WHERE id = #{id}
            """)
    UserRecord findById(@Param("id") Long id);

    @Select("""
            SELECT id, username, display_name, password_hash, avatar_url, created_at
            FROM users
            WHERE username = #{username}
            """)
    UserRecord findByUsername(@Param("username") String username);

    @Select("""
            SELECT u.id,
                   u.username,
                   u.display_name,
                   u.avatar_url,
                   CASE WHEN f.id IS NULL THEN FALSE ELSE TRUE END AS friend,
                   f.friend_remark,
                   CASE WHEN outgoing_request.id IS NULL THEN FALSE ELSE TRUE END AS outgoing_request,
                   CASE WHEN incoming_request.id IS NULL THEN FALSE ELSE TRUE END AS incoming_request
            FROM users u
            LEFT JOIN friendships f
              ON f.owner_id = #{currentUserId}
             AND f.friend_id = u.id
            LEFT JOIN friend_requests outgoing_request
              ON outgoing_request.requester_id = #{currentUserId}
             AND outgoing_request.recipient_id = u.id
            LEFT JOIN friend_requests incoming_request
              ON incoming_request.requester_id = u.id
             AND incoming_request.recipient_id = #{currentUserId}
            WHERE u.id <> #{currentUserId}
            ORDER BY u.display_name ASC
            """)
    List<UserSummaryRow> listUsers(@Param("currentUserId") Long currentUserId);

    @Select("""
            SELECT username
            FROM users
            ORDER BY id ASC
            """)
    List<String> listAllUsernames();

    @Insert("""
            INSERT INTO users (username, display_name, password_hash, avatar_url, created_at)
            VALUES (#{username}, #{displayName}, #{passwordHash}, #{avatarUrl}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserRecord user);

    @Update("""
            UPDATE users
            SET avatar_url = #{avatarUrl}
            WHERE id = #{userId}
            """)
    int updateAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash}
            WHERE id = #{userId}
            """)
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}
