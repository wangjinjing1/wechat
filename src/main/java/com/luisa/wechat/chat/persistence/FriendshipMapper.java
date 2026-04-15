package com.luisa.wechat.chat.persistence;

import com.luisa.wechat.chat.persistence.model.UserSummaryRow;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface FriendshipMapper {

    @Select("""
            SELECT COUNT(1)
            FROM friendships
            WHERE owner_id = #{ownerId} AND friend_id = #{friendId}
            """)
    int countRelation(@Param("ownerId") Long ownerId, @Param("friendId") Long friendId);

    @Insert("""
            INSERT INTO friendships (owner_id, friend_id, created_at)
            VALUES (#{ownerId}, #{friendId}, #{createdAt})
            """)
    int insert(@Param("ownerId") Long ownerId, @Param("friendId") Long friendId, @Param("createdAt") Instant createdAt);

    @org.apache.ibatis.annotations.Update("""
            UPDATE friendships
            SET friend_remark = #{friendRemark}
            WHERE owner_id = #{ownerId} AND friend_id = #{friendId}
            """)
    int updateRemark(@Param("ownerId") Long ownerId,
                     @Param("friendId") Long friendId,
                     @Param("friendRemark") String friendRemark);

    @Select("""
            SELECT u.id,
                   u.username,
                   u.display_name,
                   u.avatar_url,
                   f.friend_remark,
                   TRUE AS friend
            FROM friendships f
            JOIN users u ON u.id = f.friend_id
            WHERE f.owner_id = #{ownerId}
            ORDER BY u.display_name ASC
            """)
    List<UserSummaryRow> listFriends(@Param("ownerId") Long ownerId);

    @Delete("""
            DELETE FROM friendships
            WHERE (owner_id = #{leftUserId} AND friend_id = #{rightUserId})
               OR (owner_id = #{rightUserId} AND friend_id = #{leftUserId})
            """)
    int deletePair(@Param("leftUserId") Long leftUserId, @Param("rightUserId") Long rightUserId);
}
