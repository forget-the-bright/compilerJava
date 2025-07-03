package org.hao.compiler.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
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
import org.hao.compiler.entity.CreateProjectDTO;
import org.hao.compiler.entity.Project;
import org.hao.compiler.entity.ProjectResource;
import org.hao.compiler.entity.ResourceType;
import org.hao.compiler.mapper.ProjectResourceMapper;
import org.hao.core.db.MPSqlUtil;
import org.hao.vo.Tuple;
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
import org.springframework.transaction.annotation.Transactional;

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

    //region 项目相关
    // 创建项目
    @SneakyThrows
    public Project createProject(String name, String mainClass, String creator) {
        if (StrUtil.isEmpty(mainClass)) {
            throw new RuntimeException("请输入主类");
        }
        Project project = new Project();
        project.setName(name);
        project.setMainClass(mainClass);
        project.setCreateTime(new Date());
        project.setCreator(creator);
        Db.save(project);
        List<String> split = StrUtil.split(mainClass, ".");
        ProjectResource projectResource = null;
        String packageName = split.size() > 1 ? split.subList(0, split.size() - 1).stream().collect(Collectors.joining(".")) : "";
        for (int i = 1; i <= split.size(); i++) {
            String fileName = split.get(i - 1);
            if (i == split.size()) {
                Template template = freeMarkerConfig.getTemplate("java_main_template.ftl");
                // 使用 StringWriter 接收渲染后的结果
                StringWriter stringWriter = new StringWriter();
                Map<String, Object> data = new HashMap<>();
                data.put("packageName", packageName.equals("") ? "" : "package " + packageName + ";");
                data.put("className", fileName);
                template.process(data, stringWriter);
                String content = stringWriter.toString();
                projectResource = ProjectResource.ofFile(fileName + ".java", content, project.getId(), (null == projectResource ? 0 : projectResource.getId()));
                Db.save(projectResource);
                project.setMainClassId(projectResource.getId() + "");
                project.updateById();
            } else {
                projectResource = addDirectory(project.getId(), fileName, (null == projectResource ? 0 : projectResource.getId()));
            }
        }
        return project;
        //return projectRepo.save(project);
    }

    public Project updateProject(Long projectId, CreateProjectDTO dto) {
        Project byId = Db.getById(projectId, Project.class);
        if (null == byId) return null;
        String mainClass = dto.getMainClass();
        if (!byId.getMainClass().equals(dto.getMainClass())) {
            List<String> split = StrUtil.split(mainClass, ".");
            ProjectResource projectResource = null;
            for (int i = 1; i <= split.size(); i++) {
                String fileName = split.get(i - 1);
                if (i == split.size()) {
                    projectResource = resourceMapper.getResourceByProjectIdAndParentIdAndName(projectId, (null == projectResource ? 0 : projectResource.getId()), fileName + ".java");
                } else {
                    projectResource = resourceMapper.getResourceByProjectIdAndParentIdAndName(projectId, (null == projectResource ? 0 : projectResource.getId()), fileName);
                }
                if (null == projectResource) {
                    throw new RuntimeException("未找到文件");
                }
            }
            byId.setMainClassId(projectResource.getId() + "");
        }
        byId.setName(dto.getName());
        byId.setMainClass(dto.getMainClass());
        byId.setCreator(dto.getCreator());
        Db.updateById(byId);
        return byId;
    }

    public Project getProjectById(long projectId) {
        return Db.getById(projectId, Project.class);
        //return projectRepo.findById(projectId).orElse(null);
    }

    public List<Project> getProjects() {
        return Db.list(Project.class);
        //return projectRepo.findAll();
    }
    //endregion

    //region 项目内容相关
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
    @SneakyThrows
    public ProjectResource addFile(Long projectId, String fileName, String content, Long parentId) {


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

    // 移动文件
    @Transactional(rollbackFor = Exception.class)
    public ProjectResource moveFileName(Long projectResourceId, Long parentProjectResourceId) {
        ProjectResource projectResource = resourceMapper.selectById(projectResourceId);
        // 判断子节点是否存在
        if (null == projectResource) return null;

        ProjectResource parentProjectResource = resourceMapper.selectById(parentProjectResourceId);
        // 判断父节点是否是目录
        if (null != parentProjectResource && !parentProjectResource.getType().equals(ResourceType.DIRECTORY)) {
            return null;
        }
        Project project = Db.getById(projectResource.getProjectId(), Project.class);
        if (project == null) return null;
        // 如果子节点是文件, 则替换包名
        if (projectResource.getType().equals(ResourceType.FILE)) {
            String packageName = getPackageName(projectResource.getProjectId(), parentProjectResourceId);
            String content = replacePackage(projectResource.getContent(), packageName);
            if (project.getMainClassId().equals(projectResource.getId() + "")) {
                String classNameByCode = getClassNameByCode(content);
                project.setMainClass(packageName + "." + classNameByCode);
                project.updateById();
            }
            projectResource.setContent(content);
        } else {
            List<ProjectResource> list = resourceMapper.findByProjectId(projectResource.getProjectId());
            ProjectResource findIdByProjectResource = list.stream().filter(r -> r.getId().equals(projectResource.getId())).findFirst().get();
            List<ProjectResource> parentChildDataRecursively = MPSqlUtil.fillParentChildDataRecursively(list,
                    ProjectResource::getId, ProjectResource::getParentId, ProjectResource::setChildren,
                    projectResource.getId() + "");
            findIdByProjectResource.setParentId(parentProjectResourceId);
            findIdByProjectResource.setChildren(parentChildDataRecursively);
            refreshPackageName(list, findIdByProjectResource, project);

        }
        projectResource.setParentId(parentProjectResourceId);
        resourceMapper.insertOrUpdate(projectResource);
        return projectResource;
    }

    private void refreshPackageName(List<ProjectResource> list, ProjectResource findIdByProjectResource, Project project) {
        List<ProjectResource> children = findIdByProjectResource.getChildren();
        if (null == children) return;

        ProjectResource rootResource = list.stream().filter(r -> r.getParentId() == 0L).findFirst().orElse(null);
        if (null == rootResource) return;
        Tuple<Function<Tuple<List<ProjectResource>, ProjectResource>, String>, String> functionObjectTuple = new Tuple<>();
        Function<Tuple<List<ProjectResource>, ProjectResource>, String> func = (tuple) -> {
            Function<Tuple<List<ProjectResource>, ProjectResource>, String> thisFunc = functionObjectTuple.getFirst();
            List<ProjectResource> first = tuple.getFirst();
            ProjectResource second = tuple.getSecond();
            Long parentId = second.getParentId();
            ProjectResource projectResource = first.stream().filter(r -> r.getId() == parentId).findFirst().orElse(null);
            String packageName = "";
            if (projectResource != null) {
                packageName = thisFunc.apply(Tuple.newTuple(first, projectResource));
            }
            packageName = (StrUtil.isEmpty(packageName) ? "" : packageName + ".") + second.getName();
            return packageName;
        };
        functionObjectTuple.setFirst(func);
        String packageName = func.apply(Tuple.newTuple(list, findIdByProjectResource));
        for (ProjectResource child : children) {
            if (child.getType().equals(ResourceType.FILE)) {
                String content = replacePackage(child.getContent(), packageName);
                child.setContent(content);
                resourceMapper.updateById(child);
                if (project.getMainClassId().equals(child.getId() + "")) {
                    String classNameByCode = getClassNameByCode(content);
                    project.setMainClass(packageName + "." + classNameByCode);
                    project.updateById();
                }
            } else {
                refreshPackageName(list, child, project);
            }
        }
    }

    // 获取文件详情
    public ProjectResource getProjectSourceById(Long projectResourceId) {
        ProjectResource byId = Db.getById(projectResourceId, ProjectResource.class);
//        resourceRepo.findById(projectResourceId).orElse(null);
        return byId;
    }

    // 更新文件
    @SneakyThrows
    public ProjectResource updateFile(ProjectResource projectResource) {
        ProjectResource byId = resourceMapper.selectById(projectResource.getId());
        // ProjectResource byId = resourceRepo.getById(projectResource.getId());
        if (byId == null) return byId;
        if (projectResource.getType().equals(ResourceType.DIRECTORY)) {
            projectResource.setContent("");
        }
        if (StrUtil.isNotEmpty(projectResource.getContent())) {
            Formatter formatter = new Formatter(JavaFormatterOptions.defaultOptions());
            String formattedCode = formatter.formatSource(projectResource.getContent());
            projectResource.setContent(formattedCode);
        }
        resourceMapper.insertOrUpdate(projectResource);
        return projectResource;
        //return resourceRepo.save(projectResource);
    }

    //重命名文件
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

    // 删除文件
    public ProjectResource removeProjectSourceById(Long projectResourceId) {
        ProjectResource byId = resourceMapper.selectById(projectResourceId);
        if (byId == null) return null;
        Project project = Db.getById(byId.getProjectId(), Project.class);
        if (project == null) return null;
        if (project.getMainClassId().equals(Convert.toStr(projectResourceId))) {
            throw new RuntimeException("项目主类不能删除！");
        }
        byId.deleteById();
        return byId;
    }

    // 获取项目所有文件内容列表
    public List<String> getProjectSourceContentsByProjectId(long projectId) {
        List<String> contents = resourceMapper.selectObjs(Wrappers.lambdaQuery(ProjectResource.class)
                .select(ProjectResource::getContent)
                .eq(ProjectResource::getType, ResourceType.FILE)
                .eq(ProjectResource::getProjectId, projectId));
        //List<String> contents = jdbcTemplate.queryForList(StrUtil.format("SELECT content FROM project_resource WHERE project_id ={}", projectId), String.class);
        return contents;
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

    public Project deleteProject(Long projectId) {
        Project project = getProjectById(projectId);
        project.deleteById();
        resourceMapper.delete(Wrappers.lambdaQuery(ProjectResource.class).eq(ProjectResource::getProjectId, projectId));
        return project;
    }


    //endregion

    //region 实体类,工具方法
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

    // 替换源代码中的包名
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

    // 通过源码获取类名
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
    //endregion
}
