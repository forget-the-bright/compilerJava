package org.hao.compiler.controller;

import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    // 创建项目
    @Operation(summary = "创建项目")
    @PostMapping
    public Project createProject(@RequestBody CreateProjectDTO dto) {
        return projectService.createProject(dto.getName(), dto.getCreator());
    }

    // 添加目录
    @Operation(summary = "添加目录")
    @PostMapping("/{projectId}/dirs")
    public ProjectResource addDirectory(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam(required = false) Long parentId) {
        return projectService.addDirectory(projectId, name, parentId);
    }

    // 添加文件
    @Operation(summary = "添加文件")
    @PostMapping("/{projectId}/files")
    public ProjectResource addFile(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId) throws TemplateException, IOException {
        return projectService.addFile(projectId, name, content, parentId);
    }

    @Operation(summary = "更新文件")
    @PostMapping("/updateFile")
    public ProjectResource updateFile(
            @RequestBody ProjectResource projectResource) {
        return projectService.updateFile(projectResource);
    }

    @Operation(summary = "获取文件内容")
    @GetMapping("/{ProjectResourceId}/file")
    public ProjectResource getFile(
            @PathVariable Long ProjectResourceId) {
        return projectService.getProjectSourceById(ProjectResourceId);
    }

    @Operation(summary = "重命名")
    @GetMapping("/{ProjectResourceId}/reFileName")
    public ProjectResource reFileName(
            @PathVariable Long ProjectResourceId,
            @RequestParam String name) {
        return projectService.reFileName(ProjectResourceId,name);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{ProjectResourceId}/removeById")
    public ProjectResource removeFileById(@PathVariable Long ProjectResourceId) {
        return projectService.removeProjectSourceById(ProjectResourceId);
    }

    // 获取项目树
    @Operation(summary = "获取项目树")
    @GetMapping("/{projectId}/tree")
    public List<ProjectService.TreeVO> getProjectTree(@PathVariable Long projectId) {
        return projectService.getProjectTree(projectId);
    }
}