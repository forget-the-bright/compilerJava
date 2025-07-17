//region 黄金布局配置信息
var otherLayoutConfig = {
    settings: {
        hasHeaders: true, // 开启或关闭标题栏。如果设置为 false，布局将仅显示分隔条。
        constrainDragToContainer: true, // 限制项目可拖动到布局容器的区域。layout.createDragSource()调用时将自动设置为 false。
        reorderEnabled: true, // 如果为真，用户可以通过将项目按标签拖动到所需位置来重新排列布局。
        selectionEnabled: false, // 如果为 true，用户可以通过点击标题来选择项目。这会将 layout.selectedItem 的值设置为被点击的项目，高亮显示其标题，并且布局会触发 'selectionChanged' 事件。
        popoutWholeStack: false, //决定用户点击弹出图标时将在新窗口中打开的内容。如果为 true，则整个堆栈将转移到新窗口；如果为 false，则仅打开活动组件。
        blockedPopoutsThrowError: true, // 指定当浏览器阻止弹出窗口（例如通过编程打开弹出窗口）时是否抛出错误。如果为 false，则弹出窗口调用将静默失败。
        closePopoutsOnUnload: true, // 指定在创建弹出窗口的页面关闭时是否关闭所有弹出窗口。弹出窗口与其父窗口的依赖关系不强，可以独立存在，但手动关闭会比较麻烦。此外，父窗口关闭后，对弹出窗口所做的任何更改都不会被保存。
        showPopoutIcon: true, // 指定是否应在标题栏中显示弹出图标。
        showMaximiseIcon: true, // 	指定是否应在标题栏中显示最大化图标。
        showCloseIcon: true // 指定是否应在标题栏中显示关闭图标。

    },
    dimensions: {
        borderWidth: 5, // 布局项之间边框的宽度（以像素为单位）。请注意：实际可拖动区域比可见区域宽，因此可以安全地将其设置为较小的值，而不会影响可用性。
        minItemHeight: 10, // 项目可以调整到的最小高度（以像素为单位）。
        minItemWidth: 10, // 项目可以调整到的最小宽度（以像素为单位）。
        headerHeight: 20, // 标题元素的高度（以像素为单位）。此高度可以更改，但主题的标题 CSS 需要相应调整。
        dragProxyWidth: 300, // 拖动项目时出现的元素的宽度（以像素为单位）。
        dragProxyHeight: 200 // 拖动项目时出现的元素的高度（以像素为单位）。
    },
    labels: {
        close: '关闭窗口', // 将鼠标悬停在关闭图标上时出现的工具提示文本。
        maximise: '最大化', // 	将鼠标悬停在最大化图标上时出现的工具提示文本。
        minimise: '最小化', // 将鼠标悬停在最小化图标上时出现的工具提示文本。
        popout: '在新窗口打开' // 将鼠标悬停在弹出图标上时出现的工具提示文本。
    },
}
// layout-config.js
var GoldenConfig = {
    settings: otherLayoutConfig.settings,
    dimensions: otherLayoutConfig.dimensions,
    labels: otherLayoutConfig.labels,
    content: [
        {
            type: 'row',
            content: [
                {
                    type: 'column',
                    width: 20,
                    content: [
                        {
                            type: 'component',
                            componentName: 'fileBrowser',
                            title: '文件预览',
                            componentState: {},
                            // 👇 关键配置
                            isClosable: false,
                        }
                    ]
                },
                {
                    type: 'column',
                    width: 80,
                    content: [
                        // 第一行：Java编辑器 + 编译输出
                        {
                            type: 'row',
                            height: 60,
                            content: [
                                {
                                    type: 'column',
                                    width: 70,
                                    content: [{
                                        type: 'component',
                                        componentName: 'editor',
                                        title: 'Java 编辑器',
                                        componentState: {},
                                        // 👇 关键配置
                                        isClosable: false,

                                    }]
                                },
                                /*  {
                                      type: 'column',
                                      width: 30,
                                      content: [
                                          {
                                          type: 'component',
                                          componentName: 'output',
                                          title: '编译输出',
                                          componentState: {},
                                          // 👇 关键配置
                                          isClosable: false,
                                          }
                                      ]
                                  }*/
                            ]
                        },
                        // 第二行：实时日志
                        {
                            type: 'stack',
                            height: 40,
                            content: [
                                {
                                    type: 'component',
                                    componentName: 'terminal',
                                    title: '<span class="fas fa-link" id="termianl-title"> 控制台</span>',
                                    componentState: {},
                                    // 👇 关键配置
                                    isClosable: false,
                                }, {
                                    type: 'component',
                                    componentName: 'console',
                                    title: '实时日志',
                                    componentState: {},
                                    // 👇 关键配置
                                    isClosable: false,
                                }, {
                                    type: 'component',
                                    componentName: 'output',
                                    title: '编译输出',
                                    componentState: {},
                                    // 👇 关键配置
                                    isClosable: false,
                                },
                            ]
                        }
                    ]
                }
            ],
        },
    ]
};
//endregion

// region 黄金布局组件注册
const GoldenComponentMap = new Map();
GoldenComponentMap.set('editor', function (container, state) {
    container.getElement().html('<div class="monaco-editor-container" id="editor"></div>');
    let editorContainer = container.getElement().find('.monaco-editor-container')[0];
    require.config({
        paths: {
            'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@latest/min/vs',
        }
    });
    // 创建 WebSocket 实例
    //const webSocket = new WebSocket(`${window.wsUrl}/lsp`); // 替换为你的WebSocket地址
    require([
        'vs/editor/editor.main'
    ], function (monaco) {

        editor = monaco.editor.create(editorContainer, {
            value: [getDemoCode()].join('\n'),
            language: 'java',
            theme: 'vs-light',
            fontSize: 16,
            automaticLayout: true
        });
        let fileId = window.mainClassId;
        // 填充文件内容
        fillEditorFileContent(fileId);
        // 填充命令
        fillEditorCommand();
    });

    //这里require是异步,所以这个方法要在回调中执行。
    function fillEditorCommand() {
        // 自定义 Ctrl+S 的行为
        editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, function () {
            console.log('用户按下了 Ctrl + S，正在执行保存操作');
            // 在这里执行你的保存逻辑，例如：
            saveEditorFile(true);
        });
    }

});
GoldenComponentMap.set('output', function (container, state) {
    container.getElement().html('<div id="result" style="width: 100%; height: 100%;"></div>');
});
GoldenComponentMap.set('console', function (container, state) {
    container.getElement().html('<div id="logWindow" style="width: 100%; height: 100%;"></div>');
});
GoldenComponentMap.set('terminal', function (container, state) {
    container.getElement().html('<div id="terminal" style="width: 100%; height: 100%;"></div>');
});
GoldenComponentMap.set('fileBrowser', function (container, state) {
    container.getElement().html('<div id="fileBrowser" style="width: 100%; height: 100%;"></div>');
    // 初始化 jsTree
    var fileBrowser = container.getElement().find('#fileBrowser');
    let projectId = window.projectId;
        fetch(`${window.baseUrl}/projects/${projectId}/tree`).then(response => {
            if (!response.ok) {
                isStatus401Redirect(response)
                throw new Error('Network response was not ok ' + response.statusText);
            }
            return response.json(); // 解析 JSON 格式的响应
        }).then(content => {
            let jstreeData = convertData(content);
            initJStree(jstreeData, fileBrowser);

        }).catch(error => console.error('Error fetching file:', error));

    function initJStree(jstreeData, fileBrowser) {
        let jstree;
        jstree = fileBrowser.jstree({
            'core': {
                'data': jstreeData.data,
                //"check_callback": true,
                "check_callback": JSTreeCheckCallback
            },
            'types': {
                'directory': {
                    'icon': 'fas fa-folder' //folder
                },
                'file': {
                    'icon': 'fas fa-code' //file
                }
            },
            'plugins': ['types', 'contextmenu', 'dnd'], //类型支持,菜单支持,拖拽支持
            "contextmenu": {
                // 自定义菜单项
                "items": fillJSTreeMenuItems
            },
        });
        //绑定事件
        fillJSTreeEvent(jstree);
    }

    //jstree 回调检测函数，返回true, 放过对应事件,返回false, 拦截对应事件
    function JSTreeCheckCallback(operation, node, parent, position, more) {
        if (operation === 'move_node') { // move_node
            //console.log('operation', operation);
            console.log('parent.type', parent.type);
            // node 是要移动的节点，parent 是目标父节点
            if (parent.type === "#") { // 根目录
                return true;
            }
            if (parent.type === "directory") { // 拖动目标是目录合理
                return true; // item → folder 合法
            } else {
                return false; // 其他组合不允许
            }
        }
        return true;// 允许创建、删除、重命名等操作
    }

    //jstree 绑定右键菜单
    function fillJSTreeMenuItems(node) {
        let inst = $('#fileBrowser').jstree(true);
        let items = {};
        // 只有文件夹目录才能有的事件， 新建文件夹 和 新建文件
        if (node.type === 'directory') {
            items['新建文件夹'] = {
                "label": "新建文件夹",
                'icon': 'fas fa-folder',
                "action": async function (data) {
                    // let inst = $.jstree.reference(data.reference)
                    let rdata =
                        await fetch(`${window.baseUrl}/projects/${window.projectId}/dirs?name=${encodeURIComponent("temp")}&parentId=${node.id}`,
                            {
                                method: 'POST'
                            });
                    if (!rdata.ok) {
                        isStatus401Redirect(rdata)
                        throw new Error('Network response was not ok ' + rdata.statusText);
                    }
                    rdata = await rdata.json();
                    let childNode = {type: "directory", id: rdata.id}
                    inst.create_node(node, childNode, "last", function (new_node) {
                        new_node.text = "temp";
                        inst.edit(new_node);
                    });
                }
            }
            items['新建文件'] = {
                "label": "新建文件",
                'icon': 'fas fa-code',
                "action": async function (data) {
                    let rdata =
                        await fetch(`${window.baseUrl}/projects/${window.projectId}/files?name=${encodeURIComponent("temp.java")}&parentId=${node.id}&content=''`,
                            {
                                method: 'POST'
                            });
                    if (!rdata.ok) {
                        isStatus401Redirect(rdata)
                        throw new Error('Network response was not ok ' + rdata.statusText);
                    }
                    rdata = await rdata.json();
                    editor.getModel().config = rdata;
                    editor.getModel().setValue(rdata.content);
                    //let currentNode = inst.get_node(data.reference);
                    let childNode = {type: "file", id: rdata.id}
                    //console.log('childNode', childNode)
                    inst.create_node(node, childNode, "last", function (new_node) {
                        new_node.text = "temp.java";
                        inst.edit(new_node);
                    });
                }
            }
        }
        //只有当前节点下没有子节点才可以删除
        if (!(node.children && node.children.length > 0)) {
            items['删除节点'] = {
                "label": "删除",
                'icon': 'fas fa-del',
                "action": async function (data) {
                    let parentNode = inst.get_node(data.reference);
                    //  自定义属性通过original 获取
                    //  console.log('parentNode.original.system', parentNode.original.system)
                    if (parentNode.original.system) {
                        // 通过判断节点的自定义属性来确定是否是系统文件,节点初始化赋值的属性
                        // alert("当前是系统文件不能删除！！！");
                        // return;
                    }
                    if (!confirm('确定要删除吗？')) {
                        return;
                    }
                    inst.delete_node(parentNode);
                    let rdata =
                        await fetch(`${window.baseUrl}/projects/${node.id}/removeById`,
                            {
                                method: 'DELETE'
                            });
                    if (!rdata.ok) {
                        isStatus401Redirect(rdata)
                        throw new Error('Network response was not ok ' + rdata.statusText);
                    }

                }
            }
        }
        items['重命名'] = {
            "label": "重命名",
            'icon': 'fas fa-code',
            "action": async function (data) {
                inst.edit(node);
            }
        }
        return items;
    }

    //jstree 绑定事件
    function fillJSTreeEvent(jstree) {
        // 展开节点
        jstree.on("loaded.jstree", function (event, data) {
            // 展开所有节点
            //$('#jstree').jstree('open_all');
            // 展开指定节点
            //  console.log('data', data)
            //  console.log('data', data.instance._model.data['#'].children_d)
            //data.instance.open_node('#');     // 单个节点 （1 是顶层的id）
            //data.instance.open_node([1, 10]); // 多个节点 (展开多个几点只有在一次性装载后所有节点后才可行）
            data.instance.open_node(data.instance._model.data['#'].children_d); // 展开所有节点
        });
        // 节点激活事件
        jstree.on('activate_node.jstree', function (e, data) {
            // 在这里执行你想要的操作，比如打开一个新的页面、展示详细信息等
        });
        // 节点重命名
        jstree.on('rename_node.jstree', function (e, data) {
            // 在这里执行你想要的操作，比如打开一个新的页面、展示详细信息等
            fetch(`${window.baseUrl}/projects/${data.node.id}/reFileName?name=${data.node.text}`, {
                method: 'GET' // 指定请求方法为 POST
            }).then(response => {
                if (!response.ok) {
                    isStatus401Redirect(response)
                    throw new Error('Network response was not ok ' + response.statusText);
                }
                return response.json(); // 解析 JSON 格式的响应
            })
                .then(content => {
                    let data = content;
                    editor.getModel().config = data;
                    editor.getModel().setValue(data.content);
                })
                .catch(error => console.error('Error fetching file:', error));
        });
        // 监听点击事件
        jstree.on('select_node.jstree', function (e, data) {
            if (data.node.type === 'file') {
                const fileNode = data.node;
                console.log(fileNode)
                const ProjectResourceId = data.node.id;
                // 保存编辑器当前文件内容
                saveEditorFile(false);
                // 填充编辑器文件内容,根据选择的文件id
                fillEditorFileContent(ProjectResourceId);

            }
        });
        // 节点移动事件
        jstree.on('move_node.jstree', function (e, data) {
            /*
             * data 包含以下属性：
             * - node: 被移动的节点对象
             * - parent: 新父节点的 ID
             * - position: 新的位置（索引）
             * - old_parent: 原父节点的 ID
             * - old_position: 原位置（索引）
             * - is_multi: 是否跨实例拖拽
             * - is_copy: 是否是复制而非移动
             */

            let parentId = data.parent;
            if (data.parent === '#') { // 根节点
                parentId = 0;
            }
            // 在这里执行你想要的操作，比如打开一个新的页面、展示详细信息等
            fetch(`${window.baseUrl}/projects/${data.node.id}/moveFileName?parentProjectResourceId=${parentId}`, {
                method: 'GET' // 指定请求方法为 POST
            }).then(response => {
                if (!response.ok) {
                    isStatus401Redirect(response)
                    throw new Error('Network response was not ok ' + response.statusText);
                }
                return response.json(); // 解析 JSON 格式的响应
            })
                .then(content => {
                    let data = content;
                    editor.getModel().config = data;
                    editor.getModel().setValue(data.content);
                })
                .catch(error => console.error('Error fetching file:', error));
            // 在这里执行你的逻辑，例如发送请求到服务器更新数据库
        });
    }

});
// endregion

//region 工具类方法
function transformNode(node) {
    const isFile = node.type === 'FILE';
    const transformed = {
        text: node.name,
        type: node.type.toLowerCase(),
        system: true,
        id: node.id,
    };

    if (isFile) {
        return transformed;
    }

    const children = (node.children || [])
        .map(child => transformNode(child))
        .filter(Boolean); // 过滤掉无效节点

    return {
        ...transformed,
        children
    };

    return null; // 没有子文件的目录也忽略
}

function convertData(input) {
    const result = input.reduce((acc, node) => {
        const transformed = transformNode(node);
        if (transformed) acc.push(transformed);
        return acc;
    }, []);

    return {data: result};
}

function getCode() {
    return editor ? editor.getValue() : '';
}

function base64ToUtf8(base64) {
    return decodeURIComponent(Array.prototype.map.call(atob(base64), function (c) {
        return '%' + c.charCodeAt(0).toString(16).padStart(2, '0');
    }).join(''));
}

function activateTabByTitle(titleToFind, layout) {
    // 假设你有一个组件的 reference
    const componentItem = layout.root.getItemsByFilter(function (item) {
        return item.type === 'component' && item.componentName === titleToFind;
    })[0];
    console.log('componentItem', componentItem)
    if (componentItem && componentItem.parent && componentItem.parent.setActiveContentItem) {
        componentItem.parent.setActiveContentItem(componentItem);
    }
}

//endregion

//region 后端交互方法

//保存文件函数
function saveEditorFile(flushEditorContent, func) {
    let config = editor.getModel().config;
    if (!config) {
        // alert('请选择文件');
        return;
    }
    config.content = getCode();
    // 使用 fetch 发送 POST 请求
    fetch(`${window.baseUrl}/projects/updateFile`, {
        method: 'POST', // 指定请求方法为 POST
        headers: {
            'Content-Type': 'application/json', // 设置请求头，表明请求体是 JSON 格式
            // 如果需要身份验证或其他类型的头信息，可以在这里添加
            // 'Authorization': 'Bearer your-token'
        },
        body: JSON.stringify(config), // 将 JavaScript 对象转换为 JSON 字符串
    }).then(response => {
        if (!response.ok) {
            isStatus401Redirect(response)
            throw new Error('Network response was not ok ' + response.statusText);
        }
        return response.json(); // 解析 JSON 格式的响应
    }).then(data => {
        console.log(data)
        if (func) func();
        if (flushEditorContent) {
            editor.getModel().config = data;
            editor.getModel().setValue(data.content);
        }
    }) // 成功处理响应数据
        .catch(error => console.error('There was a problem with the fetch operation:', error));
}

//编译当前文件函数
function compileCurrentCode(resultWindowTerm) {
    saveEditorFile(true, () => {
        let config = editor.getModel().config;
        if (!config) {
            // alert('请选择文件');
            return;
        }
        var eventSource = new EventSource(`${window.baseUrl}/compile/sse?ProjectResourceId=${encodeURIComponent(config.id)}`, {
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
}

// 编译项目代码
function compileProjectCode(resultWindowTerm, docmentId, interfaceAddress) {
    resultWindowTerm.clear();
    saveEditorFile(true, () => {
        let projectId = window.projectId;
        var eventSource = new EventSource(
            `${window.baseUrl}/${interfaceAddress}/sse?projectId=${projectId}&SessionId=${window.SessionId}`, {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                }
            });
        if (interfaceAddress.indexOf("Local") !== -1) {
            $('#terminationBtn').attr("style", '');
        }
        $(`${docmentId}`).prop("disabled", true);
        eventSource.onmessage = function (e) {
            let message = e.data;
            message = base64ToUtf8(message);
            resultWindowTerm.write(message);
        };
        eventSource.onerror = function () {
            eventSource.close();
            console.log('compileProject eventSource is error close ');
            $(`${docmentId}`).prop("disabled", false);
            if (interfaceAddress.indexOf("Local") !== -1) {
                $('#terminationBtn').attr("style", 'display: none;');
            }
        };
    });
}

// 填充编辑器文件内容,根据选择的文件id
function fillEditorFileContent(ProjectResourceId) {
    fetch(`${window.baseUrl}/projects/${ProjectResourceId}/file`)
        .then(response => {
            if (!response.ok) {
                isStatus401Redirect(response)
                throw new Error('Network response was not ok ' + response.statusText);
            }
            return response.json(); // 解析 JSON 格式的响应
        })
        .then(data => {
            if (!data) {
                return;
            }
            editor.getModel().config = data;
            editor.getModel().setValue(data.content);
        })
        .catch(error => console.error('Error fetching file:', error));
}

// 建立 WebSocket 连接
function connectionTerminalWS(terminalTerm) {
    // 清空终端
    terminalTerm.clear();
    const socket = new WebSocket(`${window.wsUrl}/terminalWS/user_project/${window.projectId}`);
    window.socket = socket;
    socket.onclose = function () {
        $("#termianl-title").attr('class', 'fas fa-unlink');
        $("#termianl-title").attr('data-context', 'true');
        $("#terminal").attr('data-context', 'true');
        terminalTerm.write('\r\n \r\n');
        terminalTerm.write("\u001b[31;2m  连接已断开,请呼出右键菜单点击重连 ! ! ! \u001b[0;39m \r\n \r\n");
        console.log("WebSocket connection closed.");
    };
    socket.onopen = function () {
        $("#termianl-title").attr('class', 'fas fa-link');
        console.log("WebSocket connection opened.");
        terminalTerm.clear();
    };
    const attachAddon = new xterm.AttachAddon(socket);
    terminalTerm.loadAddon(attachAddon);
}

// 销毁编译项目
function compileProjectDestory() {
    // 使用 fetch 发送 POST 请求
    fetch(`${window.baseUrl}/compileProjectLocal/stop?SessionId=${window.SessionId}`, {
        method: 'GET', // 指定请求方法为 POST
        headers: {
            'Content-Type': 'application/json', // 设置请求头，表明请求体是 JSON 格式
            // 如果需要身份验证或其他类型的头信息，可以在这里添加
            // 'Authorization': 'Bearer your-token'
        },
    }).then(response => {
        if (!response.ok) {
            isStatus401Redirect(response)
            throw new Error('Network response was not ok ' + response.statusText);
        }
        return response.text(); // 解析 JSON 格式的响应
    }).then(data => {
        console.log("销毁编译项目执行成功", data)
    }) // 成功处理响应数据
        .catch(error => console.error('There was a problem with the fetch operation:', error));
}

function isStatus401Redirect(response) {
    if (response.status === 401) {
         window.location.reload(true);
        //window.location.href = `${window.baseUrl}/error/401`;
    }
}

//endregion

//region mjs相关逻辑

function getTermAndFitAddon(scrollback, document) {
    let term = new xterm.Terminal({
        cursorBlink: true,
        fontFamily: 'monospace',
        convertEol: true,     // 自动将 \n 转换为换行（可选）
        scrollback: scrollback,  // 可选：设置滚动历史缓冲区大小
        wrap: false,         // 关键：启用自动换行
        wordWrap: false, // 开启自动换行
        theme: { //https://xtermjs.org/docs/api/terminal/interfaces/itheme/ 主题配置文档
            background: '#ffffff',
            foreground: '#000000',
            selectionBackground: 'rgba(0,120,255, 0.4)', // 蓝色 + 30% 透明度',
            cursor: 'black',
        },
        macOptionIsMeta: true, // macOS 上 Option 键作为 Meta 键使用
    });
    let fitAddon = new xterm.FitAddon();
    let webLinksAddon = new xterm.WebLinksAddon();
    // let clipboardAddon = new ClipboardAddon.ClipboardAddon();
    term.loadAddon(fitAddon);
    // 加载并启用 WebLinksAddon，这允许识别和点击网页链接
    term.loadAddon(webLinksAddon);
    // term.loadAddon(clipboardAddon);
    term.open(document);
    fitAddon.fit();
    return {term, fitAddon};
}

//endregion