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
    <link rel="stylesheet" href="https://esm.sh/xterm/css/xterm.css">
    <!-- jstree -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css"/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <link rel="stylesheet" href="${domainUrl}/css/editor.css">
    <script defer>
        window.baseUrl = "${domainUrl}";
        window.wsUrl = "${wsUrl}";
        window.projectId = "${projectId}";
        window.mainClassId = "${project.mainClassId}";
        window.SessionId = "${SessionId}";
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
<#--<script src="${domainUrl}/js/xterm-addon-clipboard.js"></script>-->
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.3.12/jstree.min.js"></script>
<script src="https://golden-layout.com/files/latest/js/goldenlayout.min.js"></script>
<!-- Monaco Editor loader -->
<script src="https://cdn.jsdelivr.net/npm/monaco-editor@latest/min/vs/loader.js"></script>
<script src="${domainUrl}/js/demo_code.js"></script>
<script src="${domainUrl}/js/layout-config.js"></script>

<#noparse>
    <script type="module">
        import {Terminal} from 'https://esm.sh/xterm@latest'
        import {FitAddon} from 'https://esm.sh/xterm-addon-fit@latest';
        import {WebLinksAddon} from 'https://esm.sh/xterm-addon-web-links@latest';
        import {AttachAddon} from 'https://esm.sh/xterm-addon-attach@latest';
        import {SerializeAddon} from 'https://esm.sh/xterm-addon-serialize@latest';

        window.xterm = {Terminal, FitAddon, WebLinksAddon, AttachAddon, SerializeAddon};


        var editor, layout;
        // 初始化布局
        layout = new GoldenLayout(GoldenConfig, '#layout-container');
        // 注册组件
        for (let [key, value] of GoldenComponentMap) {
            layout.registerComponent(key, value);
        }
        // 初始化布局
        layout.init();
        // 监听窗口大小变化，自动调整终端尺寸
        $(window).resize(function () {
            try {
                layout.updateSize();
            } catch (e) {
            }
        });

        const {term: logWindowTerm, fitAddon: logWindowFitAddon} = getTermAndFitAddon(10000, $('#logWindow')[0]);
        const {term: resultWindowTerm, fitAddon: resultWindowFitAddon} = getTermAndFitAddon(10000, $('#result')[0]);
        const {term: terminalTerm, fitAddon: terminalTermFitAddon} = getTermAndFitAddon(10000, $('#terminal')[0]);
        // console.log("terminal.options.theme:", resultWindowTerm.options.theme)

        let resizeTimeout;
        // 同时监听 Golden Layout 的更新事件
        layout.on('stateChanged', function () {
            clearTimeout(resizeTimeout); // 清除之前的定时器
            resizeTimeout = setTimeout(() => {
                logWindowFitAddon.fit();
                resultWindowFitAddon.fit();
                resizeWsTerminal();
            }, 100); // 延迟确保 DOM 已更新 // 只有最后一次会执行
        });

        // 开启控制台交互
        // 建立 WebSocket 连接
        const socket = new WebSocket(`${window.wsUrl}/terminalWS/${window.SessionId}`);
        const attachAddon = new xterm.AttachAddon(socket);
        const serializeAddon = new xterm.SerializeAddon();
        terminalTerm.loadAddon(attachAddon);
        terminalTerm.loadAddon(serializeAddon);

        function resizeWsTerminal() {
            const val= terminalTermFitAddon.proposeDimensions(); // 获取推荐的尺寸
            if (!val) return;

            const cols = val.cols;
            const rows = val.rows;
            console.log("proposeDimensions:", cols, rows)
            if (cols>30){
                terminalTermFitAddon.fit();
            }
            if (cols<50){
                return;
            }
            socket.send(JSON.stringify({
                type: "terminalTerm-resize",
                cols: cols,
                rows: rows
            }));
        }

        // 将 SSE 日志写入终端
        var logSource = new EventSource(`${window.baseUrl}/log/stream?sessionId=${window.SessionId}`);
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
</#noparse>
</body>
</html>
</body>
</html>