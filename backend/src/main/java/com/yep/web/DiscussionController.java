package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.LearningDtos.FlagRequest;
import com.yep.dto.LearningDtos.ModerationRequest;
import com.yep.dto.LearningDtos.PostRequest;
import com.yep.dto.LearningDtos.PostResponse;
import com.yep.dto.LearningDtos.ThreadRequest;
import com.yep.dto.LearningDtos.ThreadResponse;
import com.yep.service.DiscussionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/activities/{activityId}/threads")
    public List<ThreadResponse> threads(@PathVariable Long activityId) {
        return discussionService.listThreads(activityId);
    }

    @PostMapping("/activities/{activityId}/threads")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<ThreadResponse> createThread(@PathVariable Long activityId,
            @Valid @RequestBody ThreadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discussionService.createThread(activityId, request));
    }

    @GetMapping("/threads/{threadId}/posts")
    public List<PostResponse> posts(@PathVariable Long threadId) {
        return discussionService.posts(threadId);
    }

    @PostMapping("/threads/{threadId}/posts")
    public ResponseEntity<PostResponse> post(@PathVariable Long threadId, @Valid @RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discussionService.addPost(threadId, request));
    }

    @PutMapping("/posts/{postId}")
    public PostResponse edit(@PathVariable Long postId, @Valid @RequestBody PostRequest request) {
        return discussionService.editPost(postId, request);
    }

    @PostMapping("/posts/{postId}/flag")
    public PostResponse flag(@PathVariable Long postId, @RequestBody(required = false) FlagRequest request) {
        return discussionService.flagPost(postId, request);
    }

    @PutMapping("/facilitator/posts/{postId}/moderate")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public PostResponse moderate(@PathVariable Long postId, @RequestBody ModerationRequest request) {
        return discussionService.moderate(postId, request);
    }

    @GetMapping("/facilitator/activities/{activityId}/flagged-posts")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public List<PostResponse> flagged(@PathVariable Long activityId) {
        return discussionService.flagged(activityId);
    }
}
