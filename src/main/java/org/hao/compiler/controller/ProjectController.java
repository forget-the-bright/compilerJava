package org.hao.compiler.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hao.annotation.LogDefine;
import org.hao.compiler.entity.CreateProjectDTO;
import org.hao.compiler.entity.Project;
import org.hao.compiler.entity.ProjectResource;
import org.hao.compiler.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:59
 */
@Tag(name = "项目配置")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    //region 项目管理

    @ApiOperationSupport(order = 1, author = "wanghao")
    @Operation(summary = "创建项目")
    @PostMapping
    @LogDefine(value = "创建项目")
    public Project createProject(@RequestBody CreateProjectDTO dto) {
        return projectService.createProject(dto.getName(), dto.getMainClass(), dto.getCreator());
    }

    @ApiOperationSupport(order = 2, author = "wanghao")
    @Operation(summary = "更新项目")
    @PostMapping("{projectId}/updateProject")
    @LogDefine(value = "更新项目")

    public Project updateProject(@PathVariable Long projectId, @RequestBody CreateProjectDTO dto) {
        return projectService.updateProject(projectId, dto);
    }

    @ApiOperationSupport(order = 3, author = "wanghao")
    @Operation(summary = "项目列表")
    @PostMapping("listProject")
    @LogDefine(value = "项目列表")
    public List<Project> listProject() {
        return projectService.getProjects();
    }

    @ApiOperationSupport(order = 4, author = "wanghao")
    @Operation(summary = "获取项目树")
    @GetMapping("/{projectId}/tree")
    @LogDefine(value = "获取项目树")
    public List<ProjectService.TreeVO> getProjectTree(@PathVariable Long projectId) {
        return projectService.getProjectTree(projectId);
    }
    //endregion

    //region 项目内容管理
    @ApiOperationSupport(order = 5, author = "wanghao")
    @Operation(summary = "项目内容列表")
    @PostMapping("{projectId}/listProjectSource")
    @LogDefine(value = "项目内容列表")
    public List<ProjectResource> listProjectSource(@PathVariable Long projectId) {
        return projectService.listProjectSource(projectId);
    }

    // 添加目录
    @ApiOperationSupport(order = 6, author = "wanghao")
    @Operation(summary = "添加目录")
    @PostMapping("/{projectId}/dirs")
    @LogDefine(value = "添加目录")
    public ProjectResource addDirectory(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam(required = false) Long parentId) {
        return projectService.addDirectory(projectId, name, parentId);
    }

    // 添加文件
    @ApiOperationSupport(order = 7, author = "wanghao")
    @Operation(summary = "添加文件")
    @PostMapping("/{projectId}/files")
    @LogDefine("添加文件")
    public ProjectResource addFile(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId) throws TemplateException, IOException {
        return projectService.addFile(projectId, name, content, parentId);
    }

    @ApiOperationSupport(order = 8, author = "wanghao")
    @Operation(summary = "更新文件")
    @PostMapping("/updateFile")
    @LogDefine(value = "更新文件")
    public ProjectResource updateFile(
            @RequestBody ProjectResource projectResource) {
        return projectService.updateFile(projectResource);
    }

    @ApiOperationSupport(order = 9, author = "wanghao")
    @Operation(summary = "获取文件内容")
    @GetMapping("/{ProjectResourceId}/file")
    @LogDefine(value = "获取文件内容")
    public ProjectResource getFile(
            @PathVariable Long ProjectResourceId) {
        return projectService.getProjectSourceById(ProjectResourceId);
    }

    @ApiOperationSupport(order = 10, author = "wanghao")
    @Operation(summary = "重命名")
    @GetMapping("/{ProjectResourceId}/reFileName")
    @LogDefine(value = "重命名")
    public ProjectResource reFileName(
            @PathVariable Long ProjectResourceId,
            @RequestParam String name) {
        return projectService.reFileName(ProjectResourceId, name);
    }

    @ApiOperationSupport(order = 11, author = "wanghao")
    @Operation(summary = "文件移动")
    @GetMapping("/{ProjectResourceId}/moveFileName")
    @LogDefine(value = "文件移动")
    public ProjectResource reFileName(
            @PathVariable Long ProjectResourceId,
            @RequestParam Long parentProjectResourceId) {
        return projectService.moveFileName(ProjectResourceId, parentProjectResourceId);
    }

    @ApiOperationSupport(order = 12, author = "wanghao")
    @Operation(summary = "删除文件")
    @DeleteMapping("/{ProjectResourceId}/removeById")
    @LogDefine(value = "删除文件")
    public ProjectResource removeFileById(@PathVariable Long ProjectResourceId) {
        return projectService.removeProjectSourceById(ProjectResourceId);
    }
    //endregion

}