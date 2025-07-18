package org.hao.compiler.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.hao.compiler.util.JdkVersionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * JavaTextDocumentService 实现了 LSP 的 TextDocumentService 接口，
 * 用于处理与 Java 文本文档相关的操作，如：
 * - 打开/保存/关闭文档
 * - 代码补全
 * - 悬停提示
 * - 符号结构
 * - 语法诊断
 *
 * <p>适用于对接 Monaco Editor / VS Code 等支持 LSP 的客户端。</p>
 */
@Slf4j
public class JavaTextDocumentService implements TextDocumentService {

    // 缓存文档内容：URI -> Text
    private final Map<String, String> documentCache = new HashMap<>();

    /**
     * 当文档被打开时调用，缓存内容并进行分析。
     */
    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem document = params.getTextDocument();
        String uri = document.getUri();
        String text = document.getText();
        log.info("Document opened: {}", uri);
        documentCache.put(uri, text);
        analyzeDocument(uri);
    }

    /**
     * 当文档内容发生变化时调用，更新缓存并重新分析。
     */
    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String newText = params.getContentChanges().get(0).getText();
        log.info("Document changed: {}", uri);
        documentCache.put(uri, newText);
        analyzeDocument(uri);
    }

    /**
     * 当文档关闭时调用，从缓存中移除。
     */
    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        log.info("Document closed: {}", uri);
        documentCache.remove(uri);
    }

    /**
     * 当文档保存时调用，可用于持久化或编译输出。
     */
    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        log.info("Document saved: {}", uri);
    }

    /**
     * 请求代码补全建议，返回关键字、函数等补全项。
     */
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        log.info("Request for completion at: {}", params.getPosition());

        List<CompletionItem> items = new ArrayList<>();
        items.add(createCompletionItem("System.out.println", CompletionItemKind.Function));
        items.add(createCompletionItem("for", CompletionItemKind.Keyword));
        items.add(createCompletionItem("if", CompletionItemKind.Keyword));
        items.add(createCompletionItem("while", CompletionItemKind.Keyword));
        items.add(createCompletionItem("class", CompletionItemKind.Class));

        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    /**
     * 构建单个补全项。
     */
    private CompletionItem createCompletionItem(String label, CompletionItemKind kind) {
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        item.setKind(kind);
        item.setData(label); // 可用于 resolve 或 insertText
        return item;
    }

    /**
     * 请求悬停提示，返回变量/方法信息。
     */
    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        log.info("Request for hover at: {}", params.getPosition());

        MarkupContent content = new MarkupContent();
        content.setKind(MarkupKind.PLAINTEXT);
        content.setValue("This is a placeholder for variable or method info.");
        return CompletableFuture.completedFuture(new Hover(content));
    }

    /**
     * 请求文档符号列表（如类、方法等结构信息）。
     */
    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
        String uri = params.getTextDocument().getUri();

        SymbolInformation symbolInfo = new SymbolInformation();
        symbolInfo.setName("MyClass");
        symbolInfo.setKind(SymbolKind.Class);
        symbolInfo.setLocation(new Location(uri, new Range(new Position(0, 0), new Position(0, 5))));
        symbols.add(Either.forLeft(symbolInfo));

        return CompletableFuture.completedFuture(symbols);
    }

    /**
     * 分析文档内容，生成诊断信息（如 TODO 标记）。
     */
    private void analyzeDocument(String uri) {
        String text = documentCache.get(uri);
        if (text == null) return;

        List<Diagnostic> diagnostics = new ArrayList<>();
        int lineNumber = 0;

        for (String line : text.split("\n")) {
            if (line.contains("TODO")) {
                Diagnostic diagnostic = new Diagnostic();
                Range range = new Range(
                        new Position(lineNumber, line.indexOf("TODO")),
                        new Position(lineNumber, line.indexOf("TODO") + 4)
                );
                diagnostic.setRange(range);
                diagnostic.setMessage("TODO found");
                diagnostic.setSeverity(DiagnosticSeverity.Warning);
                diagnostics.add(diagnostic);
            }
            lineNumber++;
        }

        publishDiagnostics(uri, diagnostics);
    }

    /**
     * 发送诊断信息给客户端（如 Monaco）。
     * 注意：实际应通过 LanguageServer 接口调用。
     */
    private void publishDiagnostics(String uri, List<Diagnostic> diagnostics) {
        log.warn("Publishing diagnostics for {}: {}", uri, diagnostics);
    }

    private void parseJavaCode(String uri, String code) {
        ASTParser parser = ASTParser.newParser(JdkVersionUtils.getMajorJavaVersion());
        parser.setSource(code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        // 设置编译器选项（支持 record、switch 表达式等新特性）
        parser.setCompilerOptions(JavaCore.getOptions());

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // 示例：打印语法错误
        for (IProblem problem : cu.getProblems()) {
            System.out.println("Problem: " + problem.getMessage());
        }

        // 后续可扩展 AST 遍历分析
        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                System.out.println("Found method: " + node.getName());
                return super.visit(node);
            }
        });
    }

}
