package com.xypu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xypu.entity.dto.VideoQueryDTO;
import com.xypu.entity.po.VideoInfo;
import com.xypu.entity.vo.VideoInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoInfoMapper extends BaseMapper<VideoInfo> {

    /**
     * 分页查询视频列表，关联 user_info 取昵称，关联 video_info_file 取 fileId
     */
    IPage<VideoInfoVO> selectVideoPage(Page<VideoInfoVO> page, @Param("dto") VideoQueryDTO dto);

    /**
     * 查询单条视频详情，关联 user_info 取昵称，关联 video_info_file 取 fileId
     */
    VideoInfoVO selectVideoDetail(@Param("videoId") String videoId);
}
