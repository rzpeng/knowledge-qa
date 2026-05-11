package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.common.annotation.DataPermission;
import com.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(tableAlias = "kd")
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
}
