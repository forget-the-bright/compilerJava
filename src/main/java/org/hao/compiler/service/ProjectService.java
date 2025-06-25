package org.hao.compiler.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hao.compiler.entity.Project;
import org.hao.compiler.entity.ProjectResource;
import org.hao.compiler.entity.ResourceType;
import org.hao.compiler.repository.ProjectRepository;
import org.hao.compiler.repository.ProjectResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:58
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectResourceRepository resourceRepo;
    private final Configuration freeMarkerConfig;

    // 创建项目
    public Project createProject(String name, String creator) {
        Project project = new Project();
        project.setName(name);
        project.setCreateTime(new Date());
        project.setCreator(creator);
        return projectRepo.save(project);
    }

    // 添加目录
    public ProjectResource addDirectory(Long projectId, String dirName, Long parentId) {
        return resourceRepo.save(ProjectResource.ofDir(dirName, projectId, parentId));
    }

    // 添加文件
    public ProjectResource addFile(Long projectId, String fileName, String content, Long parentId) throws IOException, TemplateException {

        List<ProjectResource> resources = resourceRepo.findByProjectIdWithoutContent(projectId);
        Map<Long, ProjectResource> collect = resources.stream().collect(Collectors.toMap(ProjectResource::getId, Function.identity()));
        // Map<Long, List<ProjectResource>> collect = resources.stream().collect(Collectors.groupingBy(ProjectResource::getParentId));

        ProjectResource parentPath = resources.stream().filter(r -> r.getId() == parentId).findFirst().orElse(null);

        String packageName = "";
        if (null != parentPath) {
            packageName = parentPath.getName();
            ProjectResource projectResource = collect.get(parentPath.getParentId());
            while (true) {
                if (null == projectResource) break;
                packageName = projectResource.getName() + "." + packageName;
                projectResource = collect.get(projectResource.getParentId());
            }
        }
        String  className = StrUtil.subBefore(fileName, ".", true);
        // 加载模板文件（位于 src/main/resources/templates）
        Template template = freeMarkerConfig.getTemplate("java_template.ftl");

        // 使用 StringWriter 接收渲染后的结果
        StringWriter stringWriter = new StringWriter();
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("className", className);
        template.process(data, stringWriter);
        content = stringWriter.toString();
        ProjectResource projectResource = ProjectResource.ofFile(fileName, content, projectId, parentId);
        return resourceRepo.save(projectResource);
    }

    // 获取某个项目的目录树（递归构建）
    public List<TreeVO> getProjectTree(Long projectId) {
        //  List<ProjectResource> resources = resourceRepo.findByProjectId(projectId);
        //List<ProjectResource> resources = findResourcesWithoutContent(projectId);
        List<ProjectResource> resources = resourceRepo.findByProjectIdWithoutContent(projectId);
        Map<Long, TreeVO> map = new HashMap<>();
        List<TreeVO> rootNodes = new ArrayList<>();

        resources.forEach(r -> map.put(r.getId(), new TreeVO(r)));

        resources.forEach(r -> {
            TreeVO node = map.get(r.getId());
            if (r.getParentId() == null || r.getParentId() == 0) {
                rootNodes.add(node);
            } else {
                TreeVO parent = map.get(r.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        });

        return rootNodes;
    }

    private final EntityManager entityManager;

    public List<ProjectResource> findResourcesWithoutContent(Long projectId) {
        // 创建动态 EntityGraph
        EntityGraph<ProjectResource> graph = entityManager.createEntityGraph(ProjectResource.class);

        // 添加你希望加载的字段
        graph.addAttributeNodes("id", "projectId", "parentId", "name", "type", "createTime");

        // 构建查询并设置 fetchgraph
        TypedQuery<ProjectResource> query = entityManager.createQuery(
                "SELECT p FROM ProjectResource p WHERE p.projectId = :projectId", ProjectResource.class
        );
        query.setParameter("projectId", projectId); // 示例参数

        // 设置加载图
        query.setHint("javax.persistence.fetchgraph", graph);

        return query.getResultList();
    }

    public ProjectResource getProjectSourceById(Long projectResourceId) {
        return resourceRepo.findById(projectResourceId).orElse(null);
    }

    public ProjectResource updateFile(ProjectResource projectResource) {
        ProjectResource byId = resourceRepo.getById(projectResource.getId());
        if (byId == null) return byId;
        return resourceRepo.save(projectResource);
    }

    public ProjectResource removeProjectSourceById(Long projectResourceId) {
        return resourceRepo.findById(projectResourceId).map(resource -> {
            resourceRepo.delete(resource);
            return resource;
        }).orElse(null);
    }

    public ProjectResource reFileName(Long projectResourceId, String name) {
        return resourceRepo.findById(projectResourceId).map(resource -> {
            resource.setName(name);
            resourceRepo.save(resource);
            return resource;
        }).orElse(null);
    }

    // 树节点 VO
    @Data
    public static class TreeVO {
        private Long id;
        private String name;
        private ResourceType type;
        private String content;
        private Date createTime;
        private List<TreeVO> children = new ArrayList<>();

        public TreeVO(ProjectResource r) {
            this.id = r.getId();
            this.name = r.getName();
            this.type = r.getType();
            this.content = r.getContent();
            this.createTime = r.getCreateTime();
        }
    }
}
