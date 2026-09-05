package com.xypu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xypu.entity.po.VideoInfoFile;
import com.xypu.mapper.VideoInfoFileMapper;
import com.xypu.service.VideoInfoFileService;
import org.springframework.stereotype.Service;

@Service
public class VideoInfoFileServiceImpl extends ServiceImpl<VideoInfoFileMapper, VideoInfoFile> implements VideoInfoFileService {

    @Override
    public VideoInfoFile getByFileId(String fileId) {
        return getById(fileId);
    }

    @Override
    public VideoInfoFile getByVideoId(String videoId) {
        return getOne(new LambdaQueryWrapper<VideoInfoFile>()
                .eq(VideoInfoFile::getVideoId, videoId));
    }

    @Override
    public void removeByVideoId(String videoId) {
        remove(new LambdaQueryWrapper<VideoInfoFile>()
                .eq(VideoInfoFile::getVideoId, videoId));
    }
}
