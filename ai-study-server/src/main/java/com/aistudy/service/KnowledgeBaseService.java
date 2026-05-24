package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.Document;
import com.aistudy.entity.KnowledgeBase;
import com.aistudy.mapper.DocumentMapper;
import com.aistudy.mapper.KnowledgeBaseMapper;
import com.aistudy.vo.DocumentVO;
import com.aistudy.vo.KnowledgeBaseDetailVO;
import com.aistudy.vo.KnowledgeBaseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseVectorStoreService vectorStoreService;

    /**
     * 创建知识库
     */
    public KnowledgeBaseVO createKnowledgeBase(Long userId, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BizException(400, "知识库名称不能为空");
        }
        if (name.length() > 50) {
            throw new BizException(400, "知识库名称不能超过 50 个字符");
        }
        if (description != null && description.length() > 200) {
            throw new BizException(400, "知识库描述不能超过 200 个字符");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(userId);
        kb.setName(name.trim());
        kb.setDescription(description != null ? description.trim() : null);
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.insert(kb);

        return toVOWithCount(kb, 0);
    }

    /**
     * 获取用户的知识库列表
     */
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        List<KnowledgeBase> list = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, userId)
                        .orderByDesc(KnowledgeBase::getCreatedAt));
        return list.stream().map(kb -> toVOWithCount(kb, getActualDocCount(kb.getId()))).toList();
    }

    /**
     * 获取知识库详情（含文档列表）
     */
    public KnowledgeBaseDetailVO getKnowledgeBaseDetail(Long userId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BizException(404, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BizException(403, "无权访问该知识库");
        }

        List<Document> docs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(Document::getCreatedAt));

        List<DocumentVO> docVOs = docs.stream().map(this::toDocVO).toList();

        return KnowledgeBaseDetailVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .docCount(getActualDocCount(kb.getId()))
                .createdAt(kb.getCreatedAt())
                .documents(docVOs)
                .build();
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(Long userId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BizException(404, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BizException(403, "无权删除该知识库");
        }

        // 删除文档记录
        documentMapper.delete(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId));

        // 删除向量数据
        vectorStoreService.deleteKnowledgeBase(knowledgeBaseId);

        // 删除知识库记录
        knowledgeBaseMapper.deleteById(knowledgeBaseId);

        log.info("已删除知识库 {} 及其所有文档和向量数据", knowledgeBaseId);
    }

    /**
     * 删除知识库中的文档
     */
    @Transactional
    public void deleteDocument(Long userId, Long knowledgeBaseId, Long documentId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BizException(404, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作该知识库");
        }

        Document doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BizException(404, "文档不存在");
        }

        // 先删数据库记录（保证事务内一致）
        documentMapper.deleteById(documentId);

        // 再删向量数据（文件操作，不在事务内）
        try {
            vectorStoreService.deleteByDocumentId(knowledgeBaseId, documentId);
        } catch (Exception e) {
            log.error("向量数据删除失败（文档记录已删除）: kb={}, doc={}", knowledgeBaseId, documentId, e);
        }

        // docCount 不再维护，改为实时查询
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);

        log.info("已从知识库 {} 删除文档 {}", knowledgeBaseId, documentId);
    }

    /**
     * 获取知识库信息
     */
    public KnowledgeBase getKnowledgeBase(Long knowledgeBaseId) {
        return knowledgeBaseMapper.selectById(knowledgeBaseId);
    }

    /**
     * 校验知识库存在且属于指定用户，不存在或无权则抛异常
     */
    public void checkOwnership(Long userId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BizException(404, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作该知识库");
        }
    }

    /**
     * 获取知识库的实际文档数量（实时查询）
     */
    public int getDocCount(Long knowledgeBaseId) {
        return getActualDocCount(knowledgeBaseId);
    }

    /**
     * 从文档表获取实际文档数量
     */
    private int getActualDocCount(Long knowledgeBaseId) {
        return Math.toIntExact(documentMapper.selectCount(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId)));
    }

    private KnowledgeBaseVO toVOWithCount(KnowledgeBase kb, int docCount) {
        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .docCount(docCount)
                .createdAt(kb.getCreatedAt())
                .build();
    }

    private DocumentVO toDocVO(Document doc) {
        return DocumentVO.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
