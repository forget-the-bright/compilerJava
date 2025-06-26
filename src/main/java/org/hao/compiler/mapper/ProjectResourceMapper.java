package org.hao.compiler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.hao.compiler.entity.ProjectResource;

import java.util.List;

public interface ProjectResourceMapper extends BaseMapper<ProjectResource> {

    default List<ProjectResource> findByProjectIdWithoutContent(Long projectId) {
        List<ProjectResource> resources = selectList(Wrappers
                .lambdaQuery(ProjectResource.class)
                .select(ProjectResource::getId,
                        ProjectResource::getParentId,
                        ProjectResource::getName,
                        ProjectResource::getType,
                        ProjectResource::getProjectId,
                        ProjectResource::getCreateTime,
                        ProjectResource::getContent
                ).eq(ProjectResource::getProjectId, projectId));
        return resources;
    }

    ;
}
