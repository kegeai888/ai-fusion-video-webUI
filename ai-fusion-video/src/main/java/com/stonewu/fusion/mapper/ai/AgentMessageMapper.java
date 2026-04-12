package com.stonewu.fusion.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonewu.fusion.entity.ai.AgentMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {

    @Select("SELECT COALESCE(MAX(message_order), 0) FROM afv_agent_message WHERE conversation_id = #{conversationId} AND deleted = false")
    int findMaxMessageOrder(String conversationId);
}
