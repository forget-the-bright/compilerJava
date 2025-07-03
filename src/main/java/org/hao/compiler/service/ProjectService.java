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

    private final Configuration freeMarkerConfig;
    private final JdbcTemplate jdbcTemplate;
    private final ProjectResourceMapper resourceMapper;

    /**
     * 创建一个项目并生成与之相关的资源文件。
     *
     * @param name      项目的名称，用于标识项目。
     * @param mainClass 项目的主类全限定名（包含包路径），不能为空。
     * @param creator   项目的创建者名称。
     * @return 返回创建的项目对象，包含项目的详细信息。
     * @throws RuntimeException 如果主类为空，则抛出运行时异常。
     */
    //region 项目相关
    // 创建项目
    @SneakyThrows
    public Project createProject(String name, String mainClass, String creator) {
        // 检查主类是否为空，如果为空则抛出异常
        if (StrUtil.isEmpty(mainClass)) {
            throw new RuntimeException("请输入主类");
        }

        // 初始化项目对象并设置基本信息
        Project project = new Project();
        project.setName(name);
        project.setMainClass(mainClass);
        project.setCreateTime(new Date());
        project.setCreator(creator);
        Db.save(project);

        // 将主类全限定名按点号分割，用于后续处理包路径和文件结构
        List<String> split = StrUtil.split(mainClass, ".");
        ProjectResource projectResource = null;
        String packageName = split.size() > 1
                ? split.subList(0, split.size() - 1).stream().collect(Collectors.joining("."))
                : "";

        // 遍历主类的分段，逐层创建目录或文件
        for (int i = 1; i <= split.size(); i++) {
            String fileName = split.get(i - 1);

            // 如果是最后一段，则生成主类文件
            if (i == split.size()) {
                Template template = freeMarkerConfig.getTemplate("java_main_template.ftl");

                // 使用 FreeMarker 渲染模板，生成主类文件内容
                StringWriter stringWriter = new StringWriter();
                Map<String, Object> data = new HashMap<>();
                data.put("packageName", packageName.equals("") ? "" : "package " + packageName + ";");
                data.put("className", fileName);
                template.process(data, stringWriter);
                String content = stringWriter.toString();

                // 创建主类文件资源并保存到数据库
                projectResource = ProjectResource.ofFile(fileName + ".java", content, project.getId(),
                        (null == projectResource ? 0 : projectResource.getId()));
                Db.save(projectResource);

                // 更新项目主类资源ID
                project.setMainClassId(projectResource.getId() + "");
                project.updateById();
            } else {
                // 如果不是最后一段，则创建目录资源
                projectResource = addDirectory(project.getId(), fileName,
                        (null == projectResource ? 0 : projectResource.getId()));
            }
        }

        // 返回创建的项目对象
        return project;
    }


    /**
     * 更新指定项目的相关信息。
     *
     * @param projectId 项目ID，用于定位需要更新的项目。
     * @param dto       包含更新信息的数据传输对象，包括项目名称、主类路径、创建者等信息。
     * @return 返回更新后的项目对象。如果项目不存在，则返回null。
     * <p>
     * 主要逻辑：
     * 1. 根据项目ID获取项目对象，若项目不存在则直接返回null。
     * 2. 检查主类路径是否发生变化，若发生变化则验证新主类路径对应的资源是否存在。
     * 3. 更新项目的名称、主类路径和创建者信息，并持久化到数据库中。
     */
    public Project updateProject(Long projectId, CreateProjectDTO dto) {
        // 根据项目ID获取项目对象，若项目不存在则返回null
        Project byId = getProjectById(projectId);
        if (null == byId) return null;

        String mainClass = dto.getMainClass();
        // 如果主类路径发生变化，则需要验证新主类路径对应的资源是否存在
        if (!byId.getMainClass().equals(dto.getMainClass())) {
            List<String> split = StrUtil.split(mainClass, ".");
            ProjectResource projectResource = null;

            // 遍历主类路径的每一部分，逐级查找对应的资源
            for (int i = 1; i <= split.size(); i++) {
                String fileName = split.get(i - 1);
                fileName = (i == split.size() ? fileName + ".java" : fileName);

                // 查询当前层级的资源，若资源不存在则抛出异常
                projectResource = resourceMapper
                        .getResourceByProjectIdAndParentIdAndName(projectId, (null == projectResource ? 0 : projectResource.getId()), fileName);
                if (null == projectResource) {
                    throw new RuntimeException("未找到文件");
                }
            }

            // 设置新的主类资源ID
            byId.setMainClassId(projectResource.getId() + "");
        }

        // 更新项目的名称、主类路径和创建者信息
        byId.setName(dto.getName());
        byId.setMainClass(dto.getMainClass());
        byId.setCreator(dto.getCreator());

        // 将更新后的项目信息持久化到数据库中
        Db.updateById(byId);

        return byId;
    }


    /**
     * 根据项目ID获取项目对象。
     *
     * @param projectId 项目的唯一标识符，类型为long。
     *                  该参数用于在数据库中定位特定的项目记录。
     * @return 返回与指定projectId对应的Project对象。
     * 如果未找到对应的项目记录，则返回值可能为null，
     * 具体行为取决于Db.getById方法的实现。
     */
    public Project getProjectById(long projectId) {
        // 调用Db类的getById方法，根据projectId和Project类类型从数据库中获取项目对象。
        return Db.getById(projectId, Project.class);
    }

    /**
     * 获取项目列表。
     * <p>
     * 该函数从数据库中查询并返回所有项目的列表。
     *
     * @return 返回一个包含所有项目的列表，列表中的每个元素都是 Project 类型的对象。
     * 如果数据库中没有项目，则返回一个空列表。
     */
    public List<Project> getProjects() {
        // 调用 Db.list 方法查询数据库中所有 Project 类型的记录，并返回结果列表。
        return Db.list(Project.class);
    }


    /**
     * 删除指定ID的项目，并清理与该项目相关的资源。
     *
     * @param projectId 项目的唯一标识符，用于定位需要删除的项目。
     * @return 返回被删除的项目对象。
     * <p>
     * 主要逻辑：
     * 1. 根据项目ID获取项目对象。
     * 2. 调用项目对象的删除方法。
     * 3. 清理与该项目关联的资源记录。
     */
    public Project deleteProject(Long projectId) {
        // 根据项目ID获取项目对象
        Project project = getProjectById(projectId);

        // 调用项目对象的删除方法，标记项目为已删除
        project.deleteById();

        // 删除与该项目关联的所有资源记录
        resourceMapper.delete(Wrappers.lambdaQuery(ProjectResource.class).eq(ProjectResource::getProjectId, projectId));

        return project;
    }
    //endregion

    /**
     * 根据项目ID查询项目资源列表。
     *
     * @param projectId 项目的唯一标识符，用于筛选与该项目相关的资源。
     * @return 返回一个包含项目资源的列表，列表中的每个元素都是一个 ProjectResource 对象。
     * 如果没有找到相关资源，则返回一个空列表。
     */
    //region 项目内容相关
    public List<ProjectResource> listProjectSource(Long projectId) {
        // 使用数据库查询工具类 Db.list 方法，结合条件构造器 Wrappers.lambdaQuery，
        // 查询与指定 projectId 相关的 ProjectResource 数据。
        List<ProjectResource> list = resourceMapper.findByProjectId(projectId);
        return list;
    }


    /**
     * 添加目录到项目资源中。
     *
     * @param projectId 项目的唯一标识符，用于指定目录所属的项目。
     * @param dirName   目录名称，表示要添加的目录的名字。
     * @param parentId  父目录的唯一标识符，用于指定新目录的父级目录。如果为 null，则表示该目录为顶级目录。
     * @return 返回创建的 ProjectResource 对象，包含新添加目录的相关信息。
     */
    public ProjectResource addDirectory(Long projectId, String dirName, Long parentId) {
        // 创建一个表示目录的 ProjectResource 对象
        ProjectResource projectResource = ProjectResource.ofDir(dirName, projectId, parentId);

        // 将新创建的目录对象保存到数据库中
        Db.save(projectResource);

        return projectResource;
    }

    /**
     * 添加文件到指定项目中。
     *
     * @param projectId 项目ID，用于标识文件所属的项目。
     * @param fileName  文件名，包含扩展名（如 "Example.java"）。
     * @param content   文件内容，初始内容可能会被模板渲染后的内容覆盖。
     * @param parentId  父资源ID，用于确定文件在项目资源树中的位置。如果为 null，则表示文件位于根目录。
     * @return 返回保存后的 ProjectResource 对象，包含文件的完整信息。
     * <p>
     * 功能描述：
     * 1. 根据项目ID获取项目资源列表，并构建资源ID与资源对象的映射。
     * 2. 根据父资源ID计算文件的包名路径。
     * 3. 使用 FreeMarker 模板引擎渲染文件内容。
     * 4. 创建并保存新的 ProjectResource 对象。
     */
    @SneakyThrows
    public ProjectResource addFile(Long projectId, String fileName, String content, Long parentId) {

        // 获取项目的所有资源（不包含内容），并按资源ID构建映射以便快速查找。
        List<ProjectResource> resources = resourceMapper.findByProjectIdWithoutContent(projectId);
        Map<Long, ProjectResource> collect = resources.stream().collect(Collectors.toMap(ProjectResource::getId, Function.identity()));

        // 查找父资源对象，并根据父资源及其祖先资源计算包名路径。
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

        // 提取文件名中的类名（去掉扩展名部分）。
        String className = StrUtil.subBefore(fileName, ".", true);

        // 加载 FreeMarker 模板文件，并使用 StringWriter 接收渲染后的结果。
        Template template = freeMarkerConfig.getTemplate("java_template.ftl");
        StringWriter stringWriter = new StringWriter();
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("className", className);
        template.process(data, stringWriter);

        // 使用模板渲染后的内容替换原始内容。
        content = stringWriter.toString();

        // 创建新的 ProjectResource 对象并保存到数据库。
        ProjectResource projectResource = ProjectResource.ofFile(fileName, content, projectId, parentId);
        Db.save(projectResource);
        return projectResource;
    }


    /**
     * 移动文件或目录到指定的父目录下。
     *
     * @param projectResourceId       要移动的文件或目录的ID。
     * @param parentProjectResourceId 目标父目录的ID。如果为null，则表示移动到根目录。
     * @return 返回移动后的文件或目录对象（ProjectResource）。如果移动失败，则返回null。
     *
     * 该方法的主要功能包括：
     * 1. 检查目标文件或目录是否存在；
     * 2. 验证目标父目录是否为合法的目录类型；
     * 3. 如果移动的是文件，则更新其包名并同步相关项目信息；
     * 4. 如果移动的是目录，则递归更新其子节点的包名；
     * 5. 更新数据库中的文件或目录信息。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectResource moveFileName(Long projectResourceId, Long parentProjectResourceId) {
        // 查询要移动的文件或目录
        ProjectResource projectResource = resourceMapper.selectById(projectResourceId);
        // 如果目标文件或目录不存在，直接返回null
        if (null == projectResource) return null;

        // 查询目标父目录
        ProjectResource parentProjectResource = resourceMapper.selectById(parentProjectResourceId);
        // 如果父节点存在但不是目录类型，则返回null
        if (null != parentProjectResource && !parentProjectResource.getType().equals(ResourceType.DIRECTORY)) {
            return null;
        }

        // 获取当前文件或目录所属的项目信息
        Project project = getProjectById(projectResource.getProjectId());
        if (project == null) return null;

        // 如果移动的是文件，则需要更新其包名
        if (projectResource.getType().equals(ResourceType.FILE)) {
            // 获取新的包名并替换文件内容中的包名
            String packageName = getPackageName(projectResource.getProjectId(), parentProjectResourceId);
            String content = replacePackage(projectResource.getContent(), packageName);

            // 如果当前文件是项目的主类，则更新项目的主类信息
            if (project.getMainClassId().equals(projectResource.getId() + "")) {
                String classNameByCode = getClassNameByCode(content);
                project.setMainClass(packageName + "." + classNameByCode);
                project.updateById();
            }
            projectResource.setContent(content);
        } else {
            // 如果移动的是目录，则递归更新其子节点的包名
            List<ProjectResource> list = resourceMapper.findByProjectId(projectResource.getProjectId());
            ProjectResource findIdByProjectResource = list.stream()
                    .filter(r -> r.getId().equals(projectResource.getId()))
                    .findFirst()
                    .get();

            // 递归获取当前目录的所有子节点
            List<ProjectResource> parentChildDataRecursively = MPSqlUtil.fillParentChildDataRecursively(
                    list,
                    ProjectResource::getId,
                    ProjectResource::getParentId,
                    ProjectResource::setChildren,
                    projectResource.getId() + ""
            );

            // 更新当前目录的父节点ID及其子节点信息
            findIdByProjectResource.setParentId(parentProjectResourceId);
            findIdByProjectResource.setChildren(parentChildDataRecursively);

            // 刷新目录及其子节点的包名
            refreshPackageName(list, findIdByProjectResource, project);
        }

        // 更新文件或目录的父节点ID并保存到数据库
        projectResource.setParentId(parentProjectResourceId);
        resourceMapper.insertOrUpdate(projectResource);

        return projectResource;
    }


    /**
     * 更新项目资源的包名
     * 此方法递归地更新项目资源及其子资源的包名，特别关注文件类型的资源
     * 对于文件类型的资源，它还会更新项目的主要类名（如果该文件是主要类）
     *
     * @param list 包含所有项目资源的列表
     * @param findIdByProjectResource 需要更新包名的项目资源
     * @param project 所属的项目
     */
    private void refreshPackageName(List<ProjectResource> list, ProjectResource findIdByProjectResource, Project project) {
        // 获取当前资源的子资源
        List<ProjectResource> children = findIdByProjectResource.getChildren();
        if (null == children) return;

        // 查找项目资源列表中的根资源
        ProjectResource rootResource = list.stream().filter(r -> r.getParentId() == 0L).findFirst().orElse(null);
        if (null == rootResource) return;

        // 初始化一个元组，用于存储计算包名的函数和包名字符串
        Tuple<Function<Tuple<List<ProjectResource>, ProjectResource>, String>, String> functionObjectTuple = new Tuple<>();
        // 定义一个函数，用于递归计算包名
        Function<Tuple<List<ProjectResource>, ProjectResource>, String> func = (tuple) -> {
            // 获取当前函数引用，用于递归调用
            Function<Tuple<List<ProjectResource>, ProjectResource>, String> thisFunc = functionObjectTuple.getFirst();
            List<ProjectResource> first = tuple.getFirst();
            ProjectResource second = tuple.getSecond();
            Long parentId = second.getParentId();
            // 查找父资源
            ProjectResource projectResource = first.stream().filter(r -> r.getId() == parentId).findFirst().orElse(null);
            String packageName = "";
            if (projectResource != null) {
                // 递归计算包名
                packageName = thisFunc.apply(Tuple.newTuple(first, projectResource));
            }
            // 构造完整的包名
            packageName = (StrUtil.isEmpty(packageName) ? "" : packageName + ".") + second.getName();
            return packageName;
        };
        // 将函数存储在元组中
        functionObjectTuple.setFirst(func);
        // 计算当前资源的包名
        String packageName = func.apply(Tuple.newTuple(list, findIdByProjectResource));
        // 遍历子资源，更新它们的包名
        for (ProjectResource child : children) {
            if (child.getType().equals(ResourceType.FILE)) {
                // 对于文件类型的资源，替换内容中的包名
                String content = replacePackage(child.getContent(), packageName);
                child.setContent(content);
                resourceMapper.updateById(child);
                // 如果当前文件是项目的主要类，更新项目的主要类名
                if (project.getMainClassId().equals(child.getId() + "")) {
                    String classNameByCode = getClassNameByCode(content);
                    project.setMainClass(packageName + "." + classNameByCode);
                    project.updateById();
                }
            } else {
                // 对于非文件类型的资源，递归更新包名
                refreshPackageName(list, child, project);
            }
        }
    }

    /**
     * 根据项目资源ID获取项目资源详情
     *
     * @param projectResourceId 项目资源的唯一标识符
     * @return 返回项目资源对象，如果找不到则返回null
     */
    public ProjectResource getProjectSourceById(Long projectResourceId) {
        // 通过数据库操作，根据ID获取项目资源
        ProjectResource byId = Db.getById(projectResourceId, ProjectResource.class);
        // 返回获取到的项目资源
        return byId;
    }

    // 更新文件
    @SneakyThrows
    public ProjectResource updateFile(ProjectResource projectResource) {
        // 根据ID获取资源，如果不存在则直接返回null
        ProjectResource byId = resourceMapper.selectById(projectResource.getId());
        if (byId == null) return byId;

        // 如果资源类型为目录，则清空内容，因为目录不存储内容
        if (projectResource.getType().equals(ResourceType.DIRECTORY)) {
            projectResource.setContent("");
        }

        // 如果内容不为空，则进行格式化
        if (StrUtil.isNotEmpty(projectResource.getContent())) {
            // 创建Java代码格式化器
            Formatter formatter = new Formatter(JavaFormatterOptions.defaultOptions());
            // 格式化代码
            String formattedCode = formatter.formatSource(projectResource.getContent());
            // 更新资源内容为格式化后的代码
            projectResource.setContent(formattedCode);
        }

        // 插入或更新资源
        resourceMapper.insertOrUpdate(projectResource);
        // 返回更新后的资源
        return projectResource;
    }

    /**
     * 重命名文件
     *
     * @param projectResourceId 资源的ID，用于定位要重命名的项目资源
     * @param name 新的文件名，不包含文件扩展名
     * @return 返回重命名后的项目资源对象，如果找不到对应的资源则返回null
     */
    public ProjectResource reFileName(Long projectResourceId, String name) {
        // 根据ID获取项目资源
        ProjectResource byId = resourceMapper.selectById(projectResourceId);
        // 如果资源不存在，返回null
        if (byId == null) return null;

        // 如果资源的内容不为空，尝试更新内容中的类名
        if (StrUtil.isNotEmpty(byId.getContent())) {
            // 从资源内容中获取当前类名
            String classNameByCode = getClassNameByCode(byId.getContent());
            // 从新文件名中提取类名部分，去除扩展名
            String className = StrUtil.subBefore(name, ".", true);
            // 替换内容中的类名
            String replace = byId.getContent().replace(classNameByCode, className);
            byId.setContent(replace);
        }

        // 更新资源的文件名
        byId.setName(name);
        // 插入或更新资源
        resourceMapper.insertOrUpdate(byId);
        // 返回更新后的资源对象
        return byId;
    }

    /**
     * 删除指定ID的项目资源。
     *
     * @param projectResourceId 要删除的项目资源的唯一标识符（ID）。
     * @return 返回被删除的项目资源对象。如果资源不存在或关联的项目不存在，则返回null。
     * @throws RuntimeException 如果尝试删除的资源是项目的主类，则抛出异常，提示“项目主类不能删除！”。
     */
    public ProjectResource removeProjectSourceById(Long projectResourceId) {
        // 根据ID查询项目资源，如果资源不存在则返回null
        ProjectResource byId = resourceMapper.selectById(projectResourceId);
        if (byId == null) return null;

        // 获取与该资源关联的项目，如果项目不存在则返回null
        Project project = getProjectById(byId.getProjectId());
        if (project == null) return null;

        // 检查当前资源是否为项目的主类，如果是则抛出异常
        if (project.getMainClassId().equals(Convert.toStr(projectResourceId))) {
            throw new RuntimeException("项目主类不能删除！");
        }

        // 删除资源并返回被删除的资源对象
        byId.deleteById();
        return byId;
    }

    /**
     * 获取指定项目的所有文件内容列表。
     *
     * @param projectId 项目的唯一标识符，用于查询与该项目相关的文件内容。
     * @return 包含项目所有文件内容的字符串列表。如果项目没有相关文件，则返回空列表。
     */
    public List<String> getProjectSourceContentsByProjectId(long projectId) {
        // 使用 resourceMapper 查询符合条件的文件内容
        // 条件包括：资源类型为文件（ResourceType.FILE），且属于指定项目（projectId）
        List<String> contents = resourceMapper.selectObjs(Wrappers.lambdaQuery(ProjectResource.class)
                .select(ProjectResource::getContent) // 仅选择 content 字段
                .eq(ProjectResource::getType, ResourceType.FILE) // 过滤资源类型为文件
                .eq(ProjectResource::getProjectId, projectId)); // 过滤属于指定项目的资源

        return contents; // 返回查询到的文件内容列表
    }

    /**
     * 获取某个项目的目录树（递归构建）。
     * <p>
     * 该方法通过项目ID查询项目资源列表，并根据资源的父子关系构建一个树形结构。
     * 每个资源节点会被封装为TreeVO对象，最终返回根节点列表。
     *
     * @param projectId 项目的唯一标识符，用于查询该项目下的所有资源。
     * @return 返回一个包含树形结构根节点的列表，每个根节点是一个TreeVO对象。
     */
    public List<TreeVO> getProjectTree(Long projectId) {
        // 查询项目的所有资源（不包含内容字段），并将其存储在列表中。
        List<ProjectResource> resources = resourceMapper.findByProjectIdWithoutContent(projectId);

        // 创建一个Map用于快速查找资源对应的TreeVO对象，键为资源ID。
        Map<Long, TreeVO> map = new HashMap<>();

        // 存储树的根节点列表。
        List<TreeVO> rootNodes = new ArrayList<>();

        // 将每个资源转换为TreeVO对象，并存入map中以便后续快速访问。
        resources.forEach(r -> map.put(r.getId(), new TreeVO(r)));

        // 遍历资源列表，构建树形结构。
        resources.forEach(r -> {
            // 获取当前资源对应的TreeVO节点。
            TreeVO node = map.get(r.getId());

            // 如果资源没有父节点（即parentId为空或为0），则将其视为根节点。
            if (r.getParentId() == null || r.getParentId() == 0) {
                rootNodes.add(node);
            } else {
                // 否则找到其父节点，并将当前节点添加到父节点的子节点列表中。
                TreeVO parent = map.get(r.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        });

        // 返回构建完成的树形结构根节点列表。
        return rootNodes;
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

    /**
     * 根据项目ID和父资源ID获取包名
     * 此方法通过项目ID获取所有相关资源，并根据parentId构建完整的包名
     * 包名是通过从给定的parentId开始，逐级向上获取父资源的名称，并以点号连接
     *
     * @param projectId 项目ID，用于获取项目的所有资源
     * @param parentId 父资源ID，用于定位资源树中的特定路径
     * @return 返回构建的包名字符串，如果无法找到指定的父路径，则返回空字符串
     */
    private String getPackageName(Long projectId, Long parentId) {
        // 根据项目ID获取所有相关资源，但不包括内容详情
        List<ProjectResource> resources = resourceMapper.findByProjectIdWithoutContent(projectId);

        // 将资源列表转换为Map，以便于通过ID快速访问资源对象
        Map<Long, ProjectResource> collect = resources.stream().collect(Collectors.toMap(ProjectResource::getId, Function.identity()));

        // 查找指定的父资源路径
        ProjectResource parentPath = resources.stream().filter(r -> r.getId() == parentId).findFirst().orElse(null);

        // 初始化包名变量
        String packageName = "";

        // 如果找到了指定的父路径，则开始构建包名
        if (null != parentPath) {
            packageName = parentPath.getName();

            // 从父路径的父ID开始，逐级向上构建包名
            ProjectResource projectResource = collect.get(parentPath.getParentId());
            while (true) {
                if (null == projectResource) break;
                packageName = projectResource.getName() + "." + packageName;
                projectResource = collect.get(projectResource.getParentId());
            }
        }

        // 返回构建的包名
        return packageName;
    }

    /**
     * 替换源代码中的包名
     *
     * @param codeInfo 源代码信息
     * @param newPackageName 新的包名
     * @return 更新后的源代码
     */
    public String replacePackage(String codeInfo, String newPackageName) {
        // 如果源代码为空，则直接返回
        if (StrUtil.isEmpty(codeInfo)) return codeInfo;

        String updatedContent = "";

        // 如果新包名为空，则移除现有的包声明
        if (StrUtil.isEmpty(newPackageName)) {
            // 正则匹配 package 行，并删除它（包括前面的空白和注释）
            Pattern pattern = Pattern.compile("^\\s*package\\s+[^;]+;\\s*", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(codeInfo);
            updatedContent = matcher.replaceFirst("");
        } else {
            // 如果新包名不为空，则替换现有的包声明
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

    /**
     * 通过源码获取类名
     *
     * @param code 源代码字符串，应包含完整的类定义
     * @return 类名字符串
     * @throws IllegalArgumentException 如果源码中未找到类定义或解析类名失败时抛出此异常
     */
    public String getClassNameByCode(String code) {
        try {
            // 解析源代码字符串为CompilationUnit对象
            CompilationUnit parse = StaticJavaParser.parse(code);

            // 查找TypeDeclaration对象，即类声明
            TypeDeclaration<?> type = (TypeDeclaration) parse.findAll(TypeDeclaration.class).stream().findFirst().orElseThrow(() -> {
                // 如果未找到类定义，抛出异常
                return new IllegalArgumentException("源码中未找到类定义");
            });

            // 获取类名字符串
            String className = type.getNameAsString();

            // 返回类名
            return className;
        } catch (Exception var5) {
            // 捕获解析过程中出现的任何异常
            Exception e = var5;

            // 抛出新的异常，包含原始异常信息
            throw new IllegalArgumentException("解析类名失败: " + e.getMessage(), e);
        }
    }
    //endregion
}
