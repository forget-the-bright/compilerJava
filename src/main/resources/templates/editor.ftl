<!DOCTYPE html>
<html>
<head>
    <title>在线代码编辑器</title>
    <meta charset="UTF-8">

    <#--    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs/editor/editor.main.min.css">-->
    <!-- Golden Layout -->
    <link type="text/css" rel="stylesheet" href="https://golden-layout.com/files/latest/css/goldenlayout-base.css"/>
    <link type="text/css" rel="stylesheet"
          href="https://golden-layout.com/files/latest/css/goldenlayout-light-theme.css"/>
    <!-- xterm.js -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm/css/xterm.css">
    <!-- jstree -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css"/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <link rel="stylesheet" href="${domainUrl}/css/editor.css">

    <script>
        window.baseUrl = "${domainUrl}";
        window.wsUrl = "${wsUrl}";
        window.projectId = "${projectId}";
        window.mainClassId = "${project.mainClassId}";
    </script>
</head>
<body>
<div class="toolbar">
    <button id="compileSseBtn">编译当前文件运行</button>
    <button id="compileProjectSseBtn">编译项目运行</button>
    <button id="clearLogsBtn">清除日志</button>
    <button id="saveFile">保存文件</button>
</div>
<div id="layout-container"></div>


<!-- jsTree 脚本 -->
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.3.12/jstree.min.js"></script>
<script src="https://golden-layout.com/files/latest/js/goldenlayout.min.js"></script>
<!-- Monaco Editor loader -->
<script src="https://cdn.jsdelivr.net/npm/monaco-editor@latest/min/vs/loader.js"></script>

<script src="${domainUrl}/js/demo_code.js"></script>
<script src="${domainUrl}/js/layout-config.js"></script>

<script>
    var editor;
    var layout;
    // 初始化布局
    layout = new GoldenLayout(GoldenConfig, '#layout-container');
    // 注册组件
    for (let [key, value] of GoldenComponentMap) {
        layout.registerComponent(key, value);
    }
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

    import {Terminal} from 'https://esm.sh/xterm@latest'
    import {FitAddon} from 'https://esm.sh/xterm-addon-fit@latest';
    import {WebLinksAddon} from 'https://esm.sh/xterm-addon-web-links@latest';


    function getTermAndFitAddon(scrollback, document) {
        let term = new Terminal({
            cursorBlink: true,
            fontFamily: 'monospace',
            convertEol: true,     // 自动将 \n 转换为换行（可选）
            scrollback: scrollback,  // 可选：设置滚动历史缓冲区大小
            wrap: true,         // 关键：启用自动换行
            theme: {
                background: '#ffffff',
                foreground: '#000000'
            }
        });
        let fitAddon = new FitAddon();
        let webLinksAddon =  new WebLinksAddon();
        console.log(webLinksAddon)
        term.loadAddon(fitAddon);
        // 加载并启用 WebLinksAddon，这允许识别和点击网页链接
        term.loadAddon(webLinksAddon);
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
        }, 100); // 延迟确保 DOM 已更新 // 只有最后一次会执行
    });

    // 将 SSE 日志写入终端
    var logSource = new EventSource('${domainUrl}/log/stream');
    logSource.onmessage = function (e) {
        let message = e.data;
        message = base64ToUtf8(message);
        logWindowTerm.write(message);
    };

    //保存文件按钮事件
    $('#saveFile').click(() => saveEditorFile(true));
    // 编译按钮事件
    $('#compileSseBtn').click(() => compileCurrentCode(resultWindowTerm));
    // 编译项目按钮事件
    $('#compileProjectSseBtn').click(() => compileProjectCode(resultWindowTerm));
    // 清除日志按钮事件
    $('#clearLogsBtn').click(() => {
        resultWindowTerm.clear();
        logWindowTerm.clear();
    });

</script>
</body>
</html>