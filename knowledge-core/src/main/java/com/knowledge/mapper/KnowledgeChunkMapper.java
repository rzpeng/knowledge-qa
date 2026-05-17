package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.dto.Bm25SearchResult;
import com.knowledge.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    @Select("SELECT id, document_id, content, chunk_index, vector_id, " +
            "MATCH(content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS score " +
            "FROM knowledge_chunk " +
            "WHERE MATCH(content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY score DESC LIMIT #{limit}")
    List<Bm25SearchResult> searchByBm25(@Param("query") String query, @Param("limit") int limit);

    @Select("SELECT * FROM knowledge_chunk WHERE vector_id = #{vectorId} LIMIT 1")
    KnowledgeChunk selectByVectorId(@Param("vectorId") String vectorId);
}
