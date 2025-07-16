package org.hao.compiler.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.NotebookDocumentService;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * JavaLanguageServer 是语言服务器的主类，实现 LSP4J 的 LanguageServer 接口。
 * 它负责协调文本文档服务、工作区服务，并处理客户端的初始化、关闭等请求。
 *
 * <p>通常用于构建 Java 编写的服务端语言服务器，与 Monaco Editor / VS Code 等客户端对接。</p>
 */
@Slf4j
public class JavaLanguageServer implements LanguageServer {

    private final JavaWorkspaceService javaWorkspaceService = new JavaWorkspaceService();
    private final JavaTextDocumentService javaTextDocumentService = new JavaTextDocumentService();

    /**
     * 客户端连接时调用，返回服务器能力声明。
     * 包括支持的 LSP 特性、是否需要打开/保存事件等。
     */
    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        log.info("Initializing language server...");

        // 构建服务器能力
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setCompletionProvider(new CompletionOptions(Boolean.TRUE, Arrays.asList(".", "(")));
        capabilities.setHoverProvider(Boolean.TRUE);
        capabilities.setDocumentSymbolProvider(Boolean.TRUE);
        capabilities.setWorkspaceSymbolProvider(Boolean.TRUE);
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        // 构建 InitializeResult 响应
        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 在客户端确认初始化完成后调用，可用于加载项目资源或触发后台任务。
     */
    @Override
    public void initialized(InitializedParams params) {
        log.info("Language server initialized.");
        LanguageServer.super.initialized(params);
    }

    /**
     * 客户端请求关闭服务器时调用。
     * 可以在此释放资源、保存状态等。
     */
    @Override
    public CompletableFuture<Object> shutdown() {
        log.info("Shutting down language server...");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 客户端断开连接后调用，通知服务器退出。
     */
    @Override
    public void exit() {
        log.info("Language server exiting.");
        System.exit(0);
    }

    /**
     * 获取 Notebook 文档服务（可选，默认不支持）。
     */
    @Override
    public NotebookDocumentService getNotebookDocumentService() {
        return LanguageServer.super.getNotebookDocumentService();
    }

    /**
     * 获取文本文档服务，提供补全、悬停、符号等功能。
     */
    @Override
    public TextDocumentService getTextDocumentService() {
        return javaTextDocumentService;
    }

    /**
     * 获取工作区服务，提供配置变更、文件监听、命令执行等功能。
     */
    @Override
    public WorkspaceService getWorkspaceService() {
        return javaWorkspaceService;
    }

    /**
     * 处理进度取消请求（如取消长时间运行的任务）。
     */
    @Override
    public void cancelProgress(WorkDoneProgressCancelParams params) {
        log.info("Progress canceled: {}", params.getToken());
        LanguageServer.super.cancelProgress(params);
    }

    /**
     * 设置日志追踪级别（调试用途）。
     */
    @Override
    public void setTrace(SetTraceParams params) {
        log.info("Setting trace level: {}", params.getValue());
        LanguageServer.super.setTrace(params);
    }
}
