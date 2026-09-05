package com.xypu.controller;

import com.xypu.context.UserContext;
import com.xypu.entity.dto.SaveVideoInfoDTO;
import com.xypu.response.ResponseVO;
import com.xypu.service.VideoInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/client/video")
public class VideoController {

    @Resource
    private VideoInfoService videoInfoService;

    /**
     * 提交视频信息（标题、封面、分类、标签、简介等）
     * 填写完成后提交审核，auditStatus 从 0（草稿）变为 1（待审核）
     * videoId 必须属于当前登录用户，防止越权修改他人视频
     */
    @PostMapping("/saveVideoInfo")
    public ResponseVO<Void> saveVideoInfo(@RequestBody @Validated SaveVideoInfoDTO dto) {
        String userId = UserContext.get().getUserId();
        videoInfoService.saveVideoInfo(dto, userId);
        return ResponseVO.ok();
    }
}
