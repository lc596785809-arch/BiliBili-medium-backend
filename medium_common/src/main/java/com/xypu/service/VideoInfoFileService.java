package com.xypu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xypu.entity.po.VideoInfoFile;

public interface VideoInfoFileService extends IService<VideoInfoFile> {

    /** 根据 fileId 获取视频文件信息（HLS 资源入口） */
    VideoInfoFile getByFileId(String fileId);

    /** 根据 videoId 获取对应的视频文件信息（单视频一条记录） */
    VideoInfoFile getByVideoId(String videoId);

    /** 根据 videoId 删除对应的视频文件记录（取消上传或删除视频时使用） */
    void removeByVideoId(String videoId);
}
