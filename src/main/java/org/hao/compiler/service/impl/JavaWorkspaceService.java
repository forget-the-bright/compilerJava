package org.hao.compiler.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

/**
 * JavaWorkspaceService 实现了 LSP 的 WorkspaceService 接口，
 * 用于处理语言服务器中与“工作区”相关的行为，如：
 * - 配置变更
 * - 文件变动监听
 * - 自定义命令执行
 * - 全局符号搜索
 * <p>
 * 适用于对接 Monaco Editor / VS Code 等支持 LSP 的客户端。
 */
@Slf4j
public class JavaWorkspaceService implements WorkspaceService {

    /**
     * 当客户端发送新的配置信息时调用（例如用户修改了格式化规则或编译选项）。
     * 可以在这里解析 settings 并更新服务器端状态。
     *
     * @param params 包含新配置内容的对象
     */
    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        log.info("Received configuration change: {}", params.getSettings());
        // 示例：可以提取 params.getSettings() 中的特定配置项
    }

    /**
     * 当客户端监听的文件发生变化时调用（例如 .properties, pom.xml 等非打开文件的变化）。
     * 可用于触发重新加载或缓存刷新逻辑。
     *
     * @param params 包含文件变化列表的对象
     */
    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        params.getChanges().forEach(change -> {
            log.info("File changed in workspace: {}, type: {}", change.getUri(), change.getType());
            // 示例：根据 change.getType() 判断是创建/修改/删除，并作出响应
        });
    }

    /**
     * 执行自定义命令的方法。客户端可以通过此接口请求执行重构、生成代码等操作。
     * 默认调用父类方法返回空结果，可根据需要扩展具体逻辑。
     *
     * @param params 包含命令名和参数的信息
     * @return 返回命令执行结果（可为 null）
     */
    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        log.info("Executing command: {}", params.getCommand());

        // 示例：根据命令名称执行不同操作
        switch (params.getCommand()) {
            case "custom.compileProject":
                return CompletableFuture.completedFuture("Compilation started.");
            case "custom.formatAllFiles":
                return CompletableFuture.completedFuture("Formatting all files.");
            default:
                return CompletableFuture.completedFuture("Unknown command: " + params.getCommand());
        }
    }

}
