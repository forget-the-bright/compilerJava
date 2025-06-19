<!DOCTYPE html>
<html>
<head>
    <title>在线代码编辑器</title>
    <meta charset="UTF-8">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <!-- Golden Layout -->
    <link type="text/css" rel="stylesheet" href="https://golden-layout.com/files/latest/css/goldenlayout-base.css" />
    <link type="text/css" rel="stylesheet" href="https://golden-layout.com/files/latest/css/goldenlayout-light-theme.css" />
    <script type="text/javascript" src="https://golden-layout.com/files/latest/js/goldenlayout.min.js"></script>
    <!-- Monaco Editor loader -->
    <script src="https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs/loader.js"></script>
    <!-- xterm.js -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm/css/xterm.css">
<#--    <script src="https://cdn.jsdelivr.net/npm/xterm/lib/xterm.min.js"></script>-->
    <link rel="stylesheet" href="/complier/css/style.css">
</head>
<body>
    <div class="toolbar">
        <button id="compileSseBtn">编译运行</button>
        <button id="clearLogsBtn">清除日志</button>
    </div>
    <div id="layout-container"></div>

    <script type="module">
        import {Terminal} from 'https://esm.sh/xterm@5.3.0'
        import { FitAddon } from 'https://esm.sh/xterm-addon-fit@0.6.0';

        var editor;
        var layout;
        
        // 配置布局
        var config = {
            content: [{
                type: 'row',
                content: [{
                    type: 'column',
                    width: 50,
                    content: [{
                        type: 'component',
                        componentName: 'editor',
                        title: 'Java 编辑器',
                        height: 50
                    }, {
                        type: 'component',
                        componentName: 'console',
                        title: '实时日志',
                        height: 50
                    }]
                }, {
                    type: 'component',
                    componentName: 'output',
                    title: '编译输出',
                    width: 50
                }]
            }]
        };

        // 初始化布局
        layout = new GoldenLayout(config, '#layout-container');

        // 注册组件
        layout.registerComponent('editor', function(container, state) {
            container.getElement().html('<div class="monaco-editor-container" id="editor"></div>');
            require.config({ paths: { 'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs' } });
            require(['vs/editor/editor.main'], function() {
                editor = monaco.editor.create(container.getElement().find('.monaco-editor-container')[0], {
                    value: [
                        `
package com.example.demo;

import org.hao.core.print.PrintUtil;
import org.hao.spring.SpringRunUtil;
import org.hao.annotation.LogDefine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Greeter {
    public void run(){
        sayHello("123");
    }
    @LogDefine("123")
    public String  sayHello(String name) {
        System.out.println("Hello, " + name + "!");
        PrintUtil.BLUE.Println("name = " + name);
        SpringRunUtil.printRunInfo();
        log.info("name:{}",name);
        lombok.Lombok.preventNullAnalysis("123");
        return this.getClass().getName();
    }
}
                `
                    ].join('\n'),
                    language: 'java',
                    theme: 'vs-light',
                    fontSize: 16,
                    automaticLayout: true
                });
            });
        });

        layout.registerComponent('output', function(container, state) {
            container.getElement().html('<pre id="result" class="log-content"></pre>');
        });

        const term = new Terminal({
            cursorBlink: true,
            fontFamily: 'monospace',
            theme: {
                background: '#222',
                foreground: '#eee'
            }
        });
        const fitAddon = new FitAddon();
        term.loadAddon(fitAddon);

        layout.registerComponent('console', function(container, state) {
            container.getElement().html('<pre id="logWindow"  class="log-content" ></pre>');
            term.open(container.getElement().find('#logWindow')[0]);
            fitAddon.fit();
        });


        // 监听窗口大小变化，自动调整终端尺寸
        $(window).resize(function() {
            try {
                fitAddon.fit();
            } catch (e) {}
        });

        // 同时监听 Golden Layout 的更新事件
        layout.on('stateChanged', function () {
            setTimeout(() => fitAddon.fit(), 200); // 延迟确保 DOM 已更新
        });

        // 将 SSE 日志写入终端
        var logSource = new EventSource('/complier/log/stream');
        logSource.onmessage = function(e) {
            let message = e.data;
            message = base64ToUtf8( message);
            term.write(message);
        };

        function base64ToUtf8(base64) {
            return decodeURIComponent(Array.prototype.map.call(atob(base64), function(c) {
                return '%' + c.charCodeAt(0).toString(16).padStart(2, '0');
            }).join(''));
        }
        // 初始化布局
        layout.init();

        // 窗口大小改变时重新计算布局
        $(window).resize(function() {
            layout.updateSize();
        });

        function getCode() {
            return editor ? editor.getValue() : '';
        }

        // 编译按钮事件
        $('#compileSseBtn').click(function() {
            $('#result').text('');
            var eventSource = new EventSource('/complier/compile/sse?code=' + encodeURIComponent(getCode()), {
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });
            eventSource.onmessage = function(e) {
                $('#result').append(e.data + '\n');
                $('#result').scrollTop($('#result')[0].scrollHeight);
            };
            eventSource.onerror = function() {
                eventSource.close();
            };
        });

        // 清除日志按钮事件
        $('#clearLogsBtn').click(function() {
            $('#result').text('');
            term.clear();
        });



        // 实时日志SSE
       /* var logSource = new EventSource('/log/stream');
        logSource.onmessage = function(e) {
            var logWindow = $('#logWindow');
            var message =   e.data.replace("\\r","\r").replace("\\n","\n");
            logWindow.append(message);
            console.log("返回 ",e.data)

            logWindow.scrollTop(logWindow[0].scrollHeight);
        };*/
    </script>
</body>
</html> 