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
    <script src="https://cdn.jsdelivr.net/npm/xterm/lib/xterm.min.js"></script>

    <style>
        body {
            margin: 0;
            padding: 0;
            overflow: hidden;
        }
        .toolbar {
            height: 40px;
            background: #f3f3f3;
            border-bottom: 1px solid #ddd;
            padding: 5px 15px;
            display: flex;
            align-items: center;
        }
        #layout-container {
            position: absolute;
            top: 40px;
            left: 0;
            right: 0;
            bottom: 0;
        }
        .log-content {
            background: #222;
            color: #eee;
            padding: 10px;
            font-family: monospace;
            height: 100%;
            overflow: auto;
            margin: 0;
        }
        .toolbar button {
            padding: 6px 12px;
            margin-right: 10px;
            background: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        .toolbar button:hover {
            background: #0056b3;
        }
        .monaco-editor-container {
            width: 100%;
            height: 100%;
            overflow: hidden;
        }
    </style>
</head>
<body>
    <div class="toolbar">
        <button id="compileSseBtn">编译运行</button>
        <button id="clearLogsBtn">清除日志</button>
    </div>
    <div id="layout-container"></div>

    <script>
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

        layout.registerComponent('console', function(container, state) {
            container.getElement().html('<div id="logWindow"  class="log-content" style="width:100%;height:100%;"></div>');

            const term = new Terminal({
                cursorBlink: true,
                fontFamily: 'monospace',
                theme: {
                    background: '#222',
                    foreground: '#eee'
                }
            });
            term.open(container.getElement().find('#logWindow')[0]);

            // 将 SSE 日志写入终端
            var logSource = new EventSource('/log/stream');
            logSource.onmessage = function(e) {
                let message = e.data;
                if (message.endsWith('\n')) {
                    message = message.slice(0, -1); // 去掉末尾换行
                }
                term.write(message);
            };
        });


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
            var eventSource = new EventSource('/compile/sse?code=' + encodeURIComponent(getCode()), {
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
            $('#logWindow').text('');
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