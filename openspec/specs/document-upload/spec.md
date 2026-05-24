## ADDED Requirements

### Requirement: 文档上传

系统 SHALL 提供文档上传接口，支持用户上传 PDF、Word（DOC/DOCX）和 TXT 格式的文件。上传后系统 SHALL 自动提取文档的纯文本内容。

#### Scenario: 成功上传 PDF 文档
- **WHEN** 用户上传一个有效的 PDF 文件（大小 ≤ 10MB）
- **THEN** 系统调用 Tika 解析 PDF 内容，返回提取的纯文本和文档元信息（文件名、大小、页数）

#### Scenario: 成功上传 Word 文档
- **WHEN** 用户上传一个有效的 DOC/DOCX 文件（大小 ≤ 10MB）
- **THEN** 系统调用 Tika 解析 Word 内容，返回提取的纯文本和文档元信息

#### Scenario: 成功上传 TXT 文档
- **WHEN** 用户上传一个有效的 TXT 文件（大小 ≤ 10MB）
- **THEN** 系统读取文件内容，返回纯文本和文档元信息

#### Scenario: 上传不支持的文件格式
- **WHEN** 用户上传一个不支持的文件格式（如图片、视频、压缩包）
- **THEN** 系统 SHALL 返回错误提示"不支持的文件格式，请上传 PDF、Word 或 TXT 文件"

#### Scenario: 上传文件超出大小限制
- **WHEN** 用户上传的文件大小超过 10MB
- **THEN** 系统 SHALL 返回错误提示"文件大小超出限制，最大支持 10MB"

#### Scenario: 文档解析失败
- **WHEN** 用户上传的文件内容无法解析（如损坏的 PDF、加密的 Word）
- **THEN** 系统 SHALL 返回错误提示"文档解析失败，请检查文件是否正常或尝试其他格式"

### Requirement: 文档内容清洗

系统 SHALL 对提取的文档文本进行清洗，去除无意义的空白字符、特殊符号和格式噪声。

#### Scenario: 清洗多余空白
- **WHEN** 提取的文本包含连续多个空行或大量空格
- **THEN** 系统 SHALL 将连续空白压缩为单个换行或空格，保留文档结构

#### Scenario: 清洗特殊字符
- **WHEN** 提取的文本包含不可见控制字符或乱码
- **THEN** 系统 SHALL 过滤掉不可见控制字符，保留可读文本内容

### Requirement: 文档分块与向量化

系统 SHALL 将提取的文档文本进行分块处理，并通过 Embedding API 生成向量表示后存储到向量存储中。

#### Scenario: 文档分块成功
- **WHEN** 一个 10 页的 PDF 文档被上传
- **THEN** 系统使用 TokenTextSplitter 将文本分割为多个块（默认 chunkSize=800, overlap=200），每个块携带文档 ID 和知识库 ID 的元数据

#### Scenario: 向量化存储成功
- **WHEN** 文档被分块后
- **THEN** 系统调用 Embedding API 为每个文本块生成向量，并存储到该知识库对应的 VectorStore 中

#### Scenario: Embedding API 调用失败
- **WHEN** Embedding API 返回错误或超时
- **THEN** 系统 SHALL 记录错误日志，返回提示"文档处理失败，请稍后重试"，已成功处理的块保留不回滚
