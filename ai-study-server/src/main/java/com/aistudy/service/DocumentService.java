package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.Document;
import com.aistudy.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "doc", "txt");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 上传文档并提取内容
     */
    public DocumentUploadResult uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        // 1. 校验文件
        validateFile(file);

        // 2. 提取文件类型
        String fileType = extractFileType(file.getOriginalFilename());

        // 3. 保存文档记录
        Document doc = new Document();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileSize(file.getSize());
        doc.setFileType(fileType);
        doc.setStatus("processing");
        doc.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(doc);

        try {
            // 4. 提取文档内容
            String content = extractContent(file, fileType);
            log.info("文档解析成功，文件: {}, 内容长度: {}", file.getOriginalFilename(), content.length());

            // 5. 文本清洗
            String cleanedContent = cleanText(content);
            log.info("文本清洗完成，清洗后长度: {}", cleanedContent.length());

            // 6. 分块
            List<org.springframework.ai.document.Document> chunks = splitText(cleanedContent, doc.getId(), knowledgeBaseId);
            log.info("文本分块完成，块数: {}", chunks.size());

            // 7. 更新状态
            doc.setStatus("completed");
            documentMapper.updateById(doc);

            return new DocumentUploadResult(doc.getId(), doc.getFileName(), doc.getFileSize(),
                    doc.getFileType(), doc.getStatus(), chunks);

        } catch (Exception e) {
            log.error("文档处理失败: {}", file.getOriginalFilename(), e);
            doc.setStatus("failed");
            documentMapper.updateById(doc);
            throw new BizException(1001, "文档解析失败，请检查文件是否正常或尝试其他格式");
        }
    }

    /**
     * 校验文件格式和大小
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(400, "文件大小超出限制，最大支持 10MB");
        }

        String fileType = extractFileType(file.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(fileType.toLowerCase())) {
            throw new BizException(400, "不支持的文件格式，请上传 PDF、Word 或 TXT 文件");
        }
    }

    /**
     * 提取文件类型
     */
    private String extractFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 使用 Tika 提取文档内容
     */
    private String extractContent(MultipartFile file, String fileType) throws IOException {
        InputStreamResource resource = new InputStreamResource(file.getInputStream());
        DocumentReader reader = new TikaDocumentReader(resource);
        List<org.springframework.ai.document.Document> documents = reader.get();

        StringBuilder sb = new StringBuilder();
        for (org.springframework.ai.document.Document doc : documents) {
            sb.append(doc.getText()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 文本清洗：去除不可见控制字符、压缩空行、去除行尾空格
     */
    private String cleanText(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\x00-\\x09\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("(?m)[ \\t]+$", "")
                .trim();
    }

    /**
     * 使用 TokenTextSplitter 分块
     */
    private List<org.springframework.ai.document.Document> splitText(String content, Long documentId, Long knowledgeBaseId) {
        org.springframework.ai.document.Document doc = new org.springframework.ai.document.Document(content);
        doc.getMetadata().put("documentId", documentId.toString());
        doc.getMetadata().put("knowledgeBaseId", knowledgeBaseId.toString());

        TokenTextSplitter splitter = new TokenTextSplitter(800, 200, 200, 10000, true);
        return splitter.apply(List.of(doc));
    }

    /**
     * 删除文档记录
     */
    public Document getDocument(Long documentId) {
        return documentMapper.selectById(documentId);
    }

    public void deleteDocument(Long documentId) {
        documentMapper.deleteById(documentId);
    }

    /**
     * 文档上传结果
     */
    public record DocumentUploadResult(
            Long documentId,
            String fileName,
            Long fileSize,
            String fileType,
            String status,
            List<org.springframework.ai.document.Document> chunks
    ) {}
}
