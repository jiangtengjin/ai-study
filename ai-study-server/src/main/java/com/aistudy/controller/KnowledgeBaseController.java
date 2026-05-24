package com.aistudy.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aistudy.common.result.BizException;
import com.aistudy.common.result.R;
import com.aistudy.dto.CreateKnowledgeBaseRequest;
import com.aistudy.service.DocumentService;
import com.aistudy.service.KnowledgeBaseService;
import com.aistudy.service.KnowledgeBaseVectorStoreService;
import com.aistudy.vo.DocumentVO;
import com.aistudy.vo.KnowledgeBaseDetailVO;
import com.aistudy.vo.KnowledgeBaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Tag(name = "知识库管理")
@RestController
@RequestMapping("/api/v1/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final KnowledgeBaseVectorStoreService vectorStoreService;

    @Operation(summary = "创建知识库")
    @PostMapping
    public R<KnowledgeBaseVO> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        KnowledgeBaseVO vo = knowledgeBaseService.createKnowledgeBase(userId, request.getName(), request.getDescription());
        return R.ok(vo);
    }

    @Operation(summary = "获取知识库列表")
    @GetMapping
    public R<List<KnowledgeBaseVO>> listKnowledgeBases() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<KnowledgeBaseVO> list = knowledgeBaseService.listKnowledgeBases(userId);
        return R.ok(list);
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/{id}")
    public R<KnowledgeBaseDetailVO> getKnowledgeBaseDetail(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        KnowledgeBaseDetailVO detail = knowledgeBaseService.getKnowledgeBaseDetail(userId, id);
        return R.ok(detail);
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    public R<Void> deleteKnowledgeBase(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.deleteKnowledgeBase(userId, id);
        return R.ok();
    }

    @Operation(summary = "上传文档到知识库")
    @PostMapping("/{id}/documents")
    public R<DocumentVO> uploadDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.checkOwnership(userId, id);

        // 上传文档
        DocumentService.DocumentUploadResult result = documentService.uploadDocument(id, file);

        // 将文档块向量化入库
        try {
            vectorStoreService.addDocuments(id, result.chunks());
        } catch (Exception e) {
            log.error("文档向量化失败: {}", file.getOriginalFilename(), e);
            throw new BizException(1002, "文档向量化失败，请检查 Embedding API 配置是否正确");
        }

        return R.ok(DocumentVO.builder()
                .id(result.documentId())
                .fileName(result.fileName())
                .fileSize(result.fileSize())
                .fileType(result.fileType())
                .status(result.status())
                .build());
    }

    @Operation(summary = "删除知识库中的文档")
    @DeleteMapping("/{id}/documents/{docId}")
    public R<Void> deleteDocument(@PathVariable Long id, @PathVariable Long docId) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.deleteDocument(userId, id, docId);
        return R.ok();
    }
}
