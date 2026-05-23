package com.aistudy.controller;

import com.aistudy.common.result.R;
import com.aistudy.dto.CreateQuizRequest;
import com.aistudy.dto.SubmitAnswerRequest;
import com.aistudy.service.QuizService;
import com.aistudy.vo.AnswerResultVO;
import com.aistudy.vo.CreateQuizVO;
import com.aistudy.vo.QuestionDetailVO;
import com.aistudy.vo.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * 创建答题会话（输入知识 → AI 出题）
     */
    @PostMapping("/create")
    public R<CreateQuizVO> createQuiz(@Valid @RequestBody CreateQuizRequest request) {
        CreateQuizVO result = quizService.createSession(request);
        return R.ok(result);
    }

    /**
     * 获取会话下的所有题目
     */
    @GetMapping("/{sessionId}/questions")
    public R<List<QuestionVO>> getQuestions(@PathVariable Long sessionId) {
        List<QuestionVO> questions = quizService.getQuestions(sessionId);
        return R.ok(questions);
    }

    /**
     * 获取指定序号的题目
     */
    @GetMapping("/{sessionId}/question/{index}")
    public R<QuestionVO> getQuestion(@PathVariable Long sessionId, @PathVariable int index) {
        QuestionVO question = quizService.getQuestion(sessionId, index);
        return R.ok(question);
    }

    /**
     * 提交单题答案
     */
    @PostMapping("/{sessionId}/answer")
    public R<AnswerResultVO> submitAnswer(@PathVariable Long sessionId,
                                          @Valid @RequestBody SubmitAnswerRequest request) {
        AnswerResultVO result = quizService.submitAnswer(sessionId, request);
        return R.ok(result);
    }

    /**
     * 获取答题进度
     */
    @GetMapping("/{sessionId}/progress")
    public R<Map<String, Object>> getProgress(@PathVariable Long sessionId) {
        Map<String, Object> progress = quizService.getProgress(sessionId);
        return R.ok(progress);
    }

    /**
     * 结束答题
     */
    @PostMapping("/{sessionId}/finish")
    public R<Void> finishSession(@PathVariable Long sessionId) {
        quizService.finishSession(sessionId);
        return R.ok();
    }

    /**
     * 重新练习：基于原会话创建新会话
     */
    @PostMapping("/{sessionId}/retry")
    public R<CreateQuizVO> retrySession(@PathVariable Long sessionId) {
        CreateQuizVO result = quizService.retrySession(sessionId);
        return R.ok(result);
    }

    /**
     * 获取完整答题记录（含用户答案和解析）
     */
    @GetMapping("/{sessionId}/detail")
    public R<List<QuestionDetailVO>> getQuestionDetails(@PathVariable Long sessionId) {
        List<QuestionDetailVO> details = quizService.getQuestionDetails(sessionId);
        return R.ok(details);
    }
}
