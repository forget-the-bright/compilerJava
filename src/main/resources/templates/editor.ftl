<!DOCTYPE html>
<html>
<head>
    <title>在线代码编辑器</title>
    <meta charset="UTF-8">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <!-- Golden Layout -->
    <link type="text/css" rel="stylesheet" href="https://golden-layout.com/files/latest/css/goldenlayout-base.css"/>
    <link type="text/css" rel="stylesheet"
          href="https://golden-layout.com/files/latest/css/goldenlayout-light-theme.css"/>
    <script type="text/javascript" src="https://golden-layout.com/files/latest/js/goldenlayout.min.js"></script>
    <!-- Monaco Editor loader -->
    <script src="https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs/loader.js"></script>
    <!-- xterm.js -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm/css/xterm.css">
    <#--    <script src="https://cdn.jsdelivr.net/npm/xterm/lib/xterm.min.js"></script>-->
    <link rel="stylesheet" href="${domainUrl}/css/style.css">
</head>
<body>
<div class="toolbar">
    <button id="compileSseBtn">编译运行</button>
    <button id="clearLogsBtn">清除日志</button>
</div>
<div id="layout-container"></div>

<script src="${domainUrl}/js/demo_code.js"></script>
<script src="${domainUrl}/js/layout-config.js"></script>

<script>
    var editor;
    var layout;
    // 配置布局
    var config = layoutConfig;
    // 初始化布局
    layout = new GoldenLayout(config, '#layout-container');
    // 注册组件
    layout.registerComponent('editor', function (container, state) {
        container.getElement().html('<div class="monaco-editor-container" id="editor"></div>');
        require.config({paths: {'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs'}});
        require(['vs/editor/editor.main'], function () {
            editor = monaco.editor.create(container.getElement().find('.monaco-editor-container')[0], {
                value: [getDemoCode()].join('\n'),
                language: 'java',
                theme: 'vs-light',
                fontSize: 16,
                automaticLayout: true
            });
        });
    });

    layout.registerComponent('output', function (container, state) {
        container.getElement().html('<div id="result" style="width: 100%; height: 100%;"></div>');
    });
    layout.registerComponent('console', function (container, state) {
        container.getElement().html('<div id="logWindow" style="width: 100%; height: 100%;"></div>');
    });

    // 监听窗口大小变化，自动调整终端尺寸
    $(window).resize(function () {
        try {
            layout.updateSize();
        } catch (e) {
        }
    });
</script>

<script type="module">
    // 初始化布局
    layout.init();
    import {Terminal} from 'https://esm.sh/xterm@5.3.0'
    import {FitAddon} from 'https://esm.sh/xterm-addon-fit@0.8.0';

    function getTermAndFitAddon(scrollback, document) {
        let term = new Terminal({
            cursorBlink: true,
            fontFamily: 'monospace',
            convertEol: true,     // 自动将 \n 转换为换行（可选）
            scrollback: scrollback,  // 可选：设置滚动历史缓冲区大小
            wrap: true,         // 关键：启用自动换行
            theme: {
                background: '#222',
                foreground: '#eee'
            }
        });
        let fitAddon = new FitAddon();
        term.loadAddon(fitAddon);
        term.open(document);
        fitAddon.fit();
        return {term, fitAddon};
    }

    const {term: logWindowTerm, fitAddon: logWindowFitAddon} = getTermAndFitAddon(10000, $('#logWindow')[0]);
    const {term: resultWindowTerm, fitAddon: resultWindowFitAddon} = getTermAndFitAddon(10000, $('#result')[0]);


    let resizeTimeout;
    // 同时监听 Golden Layout 的更新事件
    layout.on('stateChanged', function () {
        clearTimeout(resizeTimeout); // 清除之前的定时器
        resizeTimeout = setTimeout(() => {
            logWindowFitAddon.fit();
            resultWindowFitAddon.fit();
            // console.log(term.cols, term.rows);
            // console.log($('#logWindow')[0].offsetWidth,$('#logWindow')[0].offsetHeight);
        }, 200); // 延迟确保 DOM 已更新 // 只有最后一次会执行
    });

    // 将 SSE 日志写入终端
    var logSource = new EventSource('${domainUrl}/log/stream');
    logSource.onmessage = function (e) {
        let message = e.data;
        message = base64ToUtf8(message);
        logWindowTerm.write(message);
    };

    // 编译按钮事件
    $('#compileSseBtn').click(function () {

        var eventSource = new EventSource('${domainUrl}/compile/sse?code=' + encodeURIComponent(getCode()), {
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        });
        $('#compileSseBtn').prop("disabled", true);
        eventSource.onmessage = function (e) {
            let message = e.data;
            message = base64ToUtf8(message);
            resultWindowTerm.write(message);
        };
        eventSource.onerror = function () {
            eventSource.close();
            $('#compileSseBtn').prop("disabled", false);
        };
    });

    // 清除日志按钮事件
    $('#clearLogsBtn').click(function () {
        resultWindowTerm.clear();
        logWindowTerm.clear();
    });

    function getCode() {
        return editor ? editor.getValue() : '';
    }

    function base64ToUtf8(base64) {
        return decodeURIComponent(Array.prototype.map.call(atob(base64), function (c) {
            return '%' + c.charCodeAt(0).toString(16).padStart(2, '0');
        }).join(''));
    }

</script>
</body>
</html> 