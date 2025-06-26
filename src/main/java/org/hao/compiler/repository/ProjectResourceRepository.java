package org.hao.compiler.repository;

import org.hao.compiler.entity.ProjectResource;
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:58
 */
public interface ProjectResourceRepository { //extends JpaRepository<ProjectResource, Long>
    //@EntityGraph(value = "ProjectResource.withoutContent", type = EntityGraph.EntityGraphType.FETCH)
    //List<ProjectResource> findByProjectId(Long projectId);

    List<ProjectResource> findByProjectIdAndParentId(Long projectId, Long parentId);

    List<ProjectResource> findByProjectId(Long projectId);

    //@Query(value = "SELECT id, project_id AS projectId, parent_id AS parentId, name, type, null as content, create_time AS createTime FROM project_resource WHERE project_id = ?1", nativeQuery = true)
    List<ProjectResource> findByProjectIdWithoutContent(Long projectId);
}
