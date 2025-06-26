package org.hao.compiler.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hao.Main;
import org.hao.compiler.entity.CreateProjectDTO;
import org.hao.compiler.entity.Project;
import org.hao.compiler.entity.ProjectResource;
import org.hao.compiler.entity.ResourceType;
import org.hao.compiler.mapper.ProjectResourceMapper;
import org.hao.compiler.repository.ProjectRepository;
import org.hao.compiler.repository.ProjectResourceRepository;
import org.hao.core.compiler.CompilerUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

//import javax.persistence.EntityGraph;
//import javax.persistence.EntityManager;
//import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.googlejavaformat.java.Formatter;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:58
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

//    private final ProjectRepository projectRepo;
//    private final ProjectResourceRepository resourceRepo;
    private final Configuration freeMarkerConfig;
    private final JdbcTemplate jdbcTemplate;
    private final ProjectResourceMapper resourceMapper;

    // 创建项目
    public Project createProject(String name, String mainClass, String creator) {
        Project project = new Project();
        project.setName(name);
        project.setMainClass(mainClass);
        project.setCreateTime(new Date());
        project.setCreator(creator);
        Db.save(project);
        return project;
        //return projectRepo.save(project);
    }

    public Project updateProject(Long projectId, CreateProjectDTO dto) {
        Project byId = Db.getById(projectId, Project.class);
        if (null == byId) return null;
        byId.setName(dto.getName());
        byId.setMainClass(dto.getMainClass());
        byId.setCreator(dto.getCreator());
        Db.updateById(byId);
        return byId;
       /* return projectRepo.findById(projectId).map(project -> {
            project.setName(dto.getName());
            project.setMainClass(dto.getMainClass());
            project.setCreator(dto.getCreator());
            return projectRepo.save(project);
        }).orElse(null);*/
    }

    public Project getProjectById(long projectId) {
        return Db.getById(projectId, Project.class);
        //return projectRepo.findById(projectId).orElse(null);
    }

    public List<Project> getProjects() {
        return Db.list(Project.class);
        //return projectRepo.findAll();
    }

    public List<ProjectResource> listProjectSource(Long projectId) {
        List<ProjectResource> list = Db.list(Wrappers.lambdaQuery(ProjectResource.class).eq(ProjectResource::getProjectId, projectId));
        return list;
        // return resourceRepo.findByProjectId(projectId);
    }

    // 添加目录
    public ProjectResource addDirectory(Long projectId, String dirName, Long parentId) {
        ProjectResource projectResource = ProjectResource.ofDir(dirName, projectId, parentId);
        Db.save(projectResource);
        return projectResource;
        //return resourceRepo.save();
    }

    // 添加文件
    public ProjectResource addFile(Long projectId, String fileName, String content, Long parentId) throws IOException, TemplateException {


        List<ProjectResource> resources = resourceMapper.findByProjectIdWithoutContent(projectId);
        //List<ProjectResource> resources = resourceRepo.findByProjectIdWithoutContent(projectId);
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
        String className = StrUtil.subBefore(fileName, ".", true);
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
        Db.save(projectResource);
        return projectResource;
        //return resourceRepo.save(projectResource);
    }

    private String getPackageName(Long projectId, Long parentId) {
        List<ProjectResource> resources = resourceMapper.findByProjectIdWithoutContent(projectId);
        // List<ProjectResource> resources = resourceRepo.findByProjectIdWithoutContent(projectId);
        Map<Long, ProjectResource> collect = resources.stream().collect(Collectors.toMap(ProjectResource::getId, Function.identity()));
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
        return packageName;
    }

    public ProjectResource moveFileName(Long projectResourceId, Long parentProjectResourceId) {
        ProjectResource projectResource = resourceMapper.selectById(projectResourceId);
        //ProjectResource projectResource = resourceRepo.findById(projectResourceId).orElse(null);
        if (null == projectResource) return null;
        ProjectResource parentProjectResource = resourceMapper.selectById(parentProjectResourceId);
        //ProjectResource parentProjectResource = resourceRepo.findById(parentProjectResourceId).orElse(null);
        if (null != parentProjectResource && !parentProjectResource.getType().equals(ResourceType.DIRECTORY)) {
            return null;
        }
        if (projectResource.getType().equals(ResourceType.FILE)) {
            String packageName = getPackageName(projectResource.getProjectId(), parentProjectResourceId);
            String content = replacePackage(projectResource.getContent(), packageName);
            projectResource.setContent(content);
        }
        projectResource.setParentId(parentProjectResourceId);
        resourceMapper.insertOrUpdate(projectResource);
        return projectResource;
        //return resourceRepo.save(projectResource);
    }

    // 获取某个项目的目录树（递归构建）
    public List<TreeVO> getProjectTree(Long projectId) {
        //  List<ProjectResource> resources = resourceRepo.findByProjectId(projectId);
        //List<ProjectResource> resources = findResourcesWithoutContent(projectId);
        List<ProjectResource> resources = resourceMapper.findByProjectIdWithoutContent(projectId);
        // List<ProjectResource> resources = resourceRepo.findByProjectIdWithoutContent(projectId);
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


    public ProjectResource getProjectSourceById(Long projectResourceId) {
        ProjectResource byId = Db.getById(projectResourceId, ProjectResource.class);
//        resourceRepo.findById(projectResourceId).orElse(null);
        return byId;
    }

    @SneakyThrows
    public ProjectResource updateFile(ProjectResource projectResource) {
        ProjectResource byId = resourceMapper.selectById(projectResource.getId());
        // ProjectResource byId = resourceRepo.getById(projectResource.getId());
        if (byId == null) return byId;
        if (StrUtil.isNotEmpty(projectResource.getContent())) {
            Formatter formatter = new Formatter(JavaFormatterOptions.defaultOptions());
            String formattedCode = formatter.formatSource(projectResource.getContent());
            projectResource.setContent(formattedCode);
        }
        resourceMapper.insertOrUpdate(projectResource);
        return projectResource;
        //return resourceRepo.save(projectResource);
    }

    public ProjectResource removeProjectSourceById(Long projectResourceId) {
        ProjectResource byId = resourceMapper.selectById(projectResourceId);
        if (byId == null) return null;
        byId.deleteById();
        return byId;
/*        return resourceRepo.findById(projectResourceId).map(resource -> {
            resourceRepo.delete(resource);
            return resource;
        }).orElse(null);*/
    }

    public ProjectResource reFileName(Long projectResourceId, String name) {
        ProjectResource byId = resourceMapper.selectById(projectResourceId);
        if (byId == null) return null;
        if (StrUtil.isNotEmpty(byId.getContent())) {
            String classNameByCode = getClassNameByCode(byId.getContent());
            String className = StrUtil.subBefore(name, ".", true);
            String replace = byId.getContent().replace(classNameByCode, className);
            byId.setContent(replace);
        }
        byId.setName(name);
        resourceMapper.insertOrUpdate(byId);
        return byId;
      /*  return resourceRepo.findById(projectResourceId).map(resource -> {
            if (StrUtil.isNotEmpty(resource.getContent())) {
                String classNameByCode = getClassNameByCode(resource.getContent());
                String className = StrUtil.subBefore(name, ".", true);
                String replace = resource.getContent().replace(classNameByCode, className);
                resource.setContent(replace);
            }
            resource.setName(name);
            resourceRepo.save(resource);
            return resource;
        }).orElse(null);*/
    }

    public List<String> getProjectSourceContentsByProjectId(long projectId) {
        List<String> contents = resourceMapper.selectObjs(Wrappers.lambdaQuery(ProjectResource.class)
                .select(ProjectResource::getContent)
                .eq(ProjectResource::getProjectId, projectId));
        //List<String> contents = jdbcTemplate.queryForList(StrUtil.format("SELECT content FROM project_resource WHERE project_id ={}", projectId), String.class);
        return contents;
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

    public String replacePackage(String codeInfo, String newPackageName) {
        if (StrUtil.isEmpty(codeInfo)) return codeInfo;
        String updatedContent = "";
        if (StrUtil.isEmpty(newPackageName)) {
            // 正则匹配 package 行，并删除它（包括前面的空白和注释）
            Pattern pattern = Pattern.compile("^\\s*package\\s+[^;]+;\\s*", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(codeInfo);
            updatedContent = matcher.replaceFirst("");
        } else {
            // 正则匹配 package 行（忽略前导空格、注释等）
            String regex = "(?m)^\\s*package\\s+[^;]+;";
            String replacement = "package " + newPackageName + ";";
            // 替换 package 语句
            updatedContent = codeInfo.replaceAll(regex, replacement);
            // 如果未替换成功（即原内容中没有 package 声明）
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(codeInfo);
            boolean flag = matcher.find();
            if (!flag) {
                // 插入新的 package 声明到文件最开始处（跳过注释或空白）
                String packageLine = "package " + newPackageName + ";\n\n";

                // 去掉头部的空白、注释等，找到第一个非空白字符的位置
                int firstNonWhitespaceIndex = 0;
                while (firstNonWhitespaceIndex < codeInfo.length() &&
                        Character.isWhitespace(codeInfo.charAt(firstNonWhitespaceIndex))) {
                    firstNonWhitespaceIndex++;
                }

                // 在第一个有效字符前插入 package 声明
                updatedContent = codeInfo.substring(0, firstNonWhitespaceIndex)
                        .concat(packageLine)
                        .concat(codeInfo.substring(firstNonWhitespaceIndex));
            }
        }
        // 写回文件
        return updatedContent;
    }

    public String getClassNameByCode(String code) {
        try {
            CompilationUnit parse = StaticJavaParser.parse(code);
            TypeDeclaration<?> type = (TypeDeclaration) parse.findAll(TypeDeclaration.class).stream().findFirst().orElseThrow(() -> {
                return new IllegalArgumentException("源码中未找到类定义");
            });
            String className = type.getNameAsString();
            return className;
        } catch (Exception var5) {
            Exception e = var5;
            throw new IllegalArgumentException("解析类名失败: " + e.getMessage(), e);
        }
    }
}
