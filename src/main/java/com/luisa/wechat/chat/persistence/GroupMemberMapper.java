package com.luisa.wechat.chat.persistence;

import com.luisa.wechat.chat.persistence.model.GroupMemberRecord;
import com.luisa.wechat.chat.persistence.model.GroupMemberRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupMemberMapper {

    @Insert("""
            INSERT INTO group_members (group_id, user_id, role, joined_at)
            VALUES (#{groupId}, #{userId}, #{role}, #{joinedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GroupMemberRecord member);

    @Select("""
            SELECT id, group_id, user_id, role, joined_at
            FROM group_members
            WHERE group_id = #{groupId} AND user_id = #{userId}
            """)
    GroupMemberRecord findByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Select("""
            SELECT u.id AS user_id,
                   u.username,
                   u.display_name,
                   u.avatar_url,
                   gm.role
            FROM group_members gm
            JOIN users u ON u.id = gm.user_id
            WHERE gm.group_id = #{groupId}
            ORDER BY gm.role DESC, u.display_name ASC
            """)
    List<GroupMemberRow> listMembers(@Param("groupId") Long groupId);

    @Select("""
            SELECT user_id
            FROM group_members
            WHERE group_id = #{groupId}
            """)
    List<Long> listMemberIds(@Param("groupId") Long groupId);

    @Delete("""
            DELETE FROM group_members
            WHERE group_id = #{groupId} AND user_id = #{userId}
            """)
    int delete(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
