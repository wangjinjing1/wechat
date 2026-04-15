package com.luisa.wechat.chat.persistence;

import com.luisa.wechat.chat.persistence.model.ChatGroupRecord;
import com.luisa.wechat.chat.persistence.model.GroupSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupMapper {

    @Insert("""
            INSERT INTO chat_groups (name, owner_id, avatar_url, created_at)
            VALUES (#{name}, #{ownerId}, #{avatarUrl}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatGroupRecord group);

    @Select("""
            SELECT id, name, owner_id, avatar_url, created_at
            FROM chat_groups
            WHERE id = #{groupId}
            """)
    ChatGroupRecord findById(@Param("groupId") Long groupId);

    @Select("""
            SELECT g.id,
                   g.name,
                   g.avatar_url,
                   g.owner_id,
                   COUNT(gm.id) AS member_count
            FROM group_members member_link
            JOIN chat_groups g ON g.id = member_link.group_id
            LEFT JOIN group_members gm ON gm.group_id = g.id
            WHERE member_link.user_id = #{userId}
            GROUP BY g.id, g.name, g.avatar_url, g.owner_id
            ORDER BY g.name ASC
            """)
    List<GroupSummaryRow> listGroupsForUser(@Param("userId") Long userId);
}
