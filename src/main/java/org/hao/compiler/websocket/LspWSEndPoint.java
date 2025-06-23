package org.hao.compiler.websocket;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.websocket.WebSocketEndpoint;
import org.hao.compiler.service.JavaLanguageServer;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/23 15:49
 */
public class LspWSEndPoint extends WebSocketEndpoint<LanguageClient> {
    @Override
    protected void configure(Launcher.Builder<LanguageClient> builder) {
        builder.setLocalService(new JavaLanguageServer());
        builder.setRemoteInterface(LanguageClient.class);
        builder.setExceptionHandler(throwable -> {
            System.out.println(throwable.getMessage());
            return new ResponseError(0, throwable.getMessage(), throwable);
        });
        System.out.println("Builder");
    }
}
