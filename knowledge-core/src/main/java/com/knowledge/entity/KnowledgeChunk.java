package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String content;

    private Integer chunkIndex;

    private String vectorId;

    /** Parent chunk content (parent-child chunking), used as richer LLM context */
    private String parentContent;

    /** Parent chunk index */
    private Integer parentIndex;

    private LocalDateTime createTime;
}
