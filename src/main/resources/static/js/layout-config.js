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
                                {
                                    type: 'column',
                                    width: 30,
                                    content: [{
                                        type: 'component',
                                        componentName: 'output',
                                        title: '编译输出',
                                        componentState: {},
                                        // 👇 关键配置
                                        isClosable: false,
                                    }]
                                }
                            ]
                        },
                        // 第二行：实时日志
                        {
                            type: 'row',
                            height: 40,
                            content: [{
                                type: 'component',
                                componentName: 'console',
                                title: '实时日志',
                                componentState: {},
                                // 👇 关键配置
                                isClosable: false,
                            }]
                        }
                    ]
                }
            ],
        },
    ]
};

const GoldenComponentMap = new Map();
GoldenComponentMap.set('editor', function (container, state) {
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

        fetch(`${window.baseUrl}/projects/5/file`)
            .then(response => response.text())
            .then(content => {
                let data = JSON.parse(content);
                console.log(data)
                editor.getModel().config = data;
                console.log(editor.getModel())
                editor.getModel().setValue(data.content);
            })
            .catch(error => console.error('Error fetching file:', error));
    });


});
GoldenComponentMap.set('output', function (container, state) {
    container.getElement().html('<div id="result" style="width: 100%; height: 100%;"></div>');
});
GoldenComponentMap.set('console', function (container, state) {
    container.getElement().html('<div id="logWindow" style="width: 100%; height: 100%;"></div>');
});
GoldenComponentMap.set('fileBrowser', function (container, state) {
    container.getElement().html('<div id="fileBrowser" style="width: 100%; height: 100%;"></div>');
    // 初始化 jsTree
    var fileBrowser = container.getElement().find('#fileBrowser');
    let projectId = 1;
    fetch(`${window.baseUrl}/projects/${projectId}/tree`).then(response => response.text())
        .then(content => {
            let jstreeData = convertData(JSON.parse(content));
            initJStree(jstreeData, fileBrowser);
        }).catch(error => console.error('Error fetching file:', error));

    function initJStree(jstreeData, fileBrowser) {
        console.log(jstreeData);
        var jstree;
        jstree = fileBrowser.jstree({
            'core': {
                'data': jstreeData.data,
                //"check_callback": true,
                "check_callback": function (operation, node, parent, position, more) {

                    if (operation === 'move_node') { // move_node
                        //console.log('operation', operation);
                        console.log('parent.type', parent.type);
                        // node 是要移动的节点，parent 是目标父节点
                        if (parent.type === "#"){ // 根目录
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
                items: function (node) {
                    console.log(' node', node);
                    let inst = $('#fileBrowser').jstree(true);
                    let items = {};
                    if (node.type === 'directory') {
                        items['新建文件夹'] = {
                            "label": "新建文件夹",
                            'icon': 'fas fa-folder',
                            "action": async function (data) {
                                // let inst = $.jstree.reference(data.reference)
                                let rdata = await fetch(`${window.baseUrl}/projects/1/dirs?name=${encodeURIComponent("temp")}&parentId=${node.id}`, {
                                    method: 'POST'
                                });
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
                                let rdata = await fetch(`${window.baseUrl}/projects/1/files?name=${encodeURIComponent("temp.java")}&parentId=${node.id}&content=''`, {
                                    method: 'POST'
                                });
                                rdata = await rdata.json();
                                console.log('childNode', rdata)
                                editor.getModel().config = rdata;
                                editor.getModel().setValue(rdata.content);
                                //let currentNode = inst.get_node(data.reference);
                                let childNode = {type: "file", id: rdata.id}
                                console.log('childNode', childNode)
                                inst.create_node(node, childNode, "last", function (new_node) {
                                    new_node.text = "temp.java";
                                    inst.edit(new_node);
                                });
                            }
                        }
                    }
                    if (!(node.children && node.children.length > 0)) {
                        items['删除节点'] = {
                            "label": "删除",
                            'icon': 'fas fa-del',
                            "action": async function (data) {
                                let parentNode = inst.get_node(data.reference);
                                //  console.log('parentNode', parentNode)
                                //自定义属性通过original 获取
                                //  console.log('parentNode.original.system', parentNode.original.system)
                                if (parentNode.original.system) {
                                    //alert("当前是系统文件不能删除！！！");
                                    // return;
                                }
                                if (!confirm('确定要删除吗？')) {
                                    return;
                                }
                                inst.delete_node(parentNode);
                                let rdata = await fetch(`${window.baseUrl}/projects/${node.id}/removeById`, {
                                    method: 'DELETE'
                                });

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
            },

        });
        // 展开节点
        jstree.on("loaded.jstree", function (event, data) {
            // 展开所有节点
            //$('#jstree').jstree('open_all');
            // 展开指定节点
            //data.instance.open_node(1);     // 单个节点 （1 是顶层的id）
            data.instance.open_node([1, 10]); // 多个节点 (展开多个几点只有在一次性装载后所有节点后才可行）
        });
        jstree.on('activate_node.jstree', function (e, data) {
            console.log('节点被激活:', data.node);
            // 在这里执行你想要的操作，比如打开一个新的页面、展示详细信息等
        });
        jstree.on('rename_node.jstree', function (e, data) {
            console.log('节点被激活:', data.node);
            // 在这里执行你想要的操作，比如打开一个新的页面、展示详细信息等
            fetch(`${window.baseUrl}/projects/${data.node.id}/reFileName?name=${data.node.text}`, {
                method: 'GET' // 指定请求方法为 POST
            }).then(response => response.text())
                .then(content => {
                    let data = JSON.parse(content);
                    console.log(data)
                    editor.getModel().config = data;
                    console.log(editor.getModel())
                    editor.getModel().setValue(data.content);
                })
                .catch(error => console.error('Error fetching file:', error));
        });
        // 监听点击事件
        jstree.on('select_node.jstree', function (e, data) {
            if (data.node.type === 'file') {
                const fileNode = data.node;
                const ProjectResourceId = data.node.id;
                console.log(fileNode)
                fetch(`${window.baseUrl}/projects/${ProjectResourceId}/file`)
                    .then(response => response.text())
                    .then(content => {
                        let data = JSON.parse(content);
                        console.log(data)
                        editor.getModel().config = data;
                        console.log(editor.getModel())
                        editor.getModel().setValue(data.content);
                    })
                    .catch(error => console.error('Error fetching file:', error));
            }
        });


        jstree.on('move_node.jstree', function (e, data) {
            console.log('节点移动:', data);

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

            // 示例：打印新旧位置信息
            console.log('新的父节点ID:', data.parent);
            console.log('新的位置:', data.position);
            console.log('原父节点ID:', data.old_parent);
            console.log('原位置:', data.old_position);
            let parentId = data.parent;
            if (data.parent === '#') {
                parentId = 0;
            }
            // 在这里执行你想要的操作，比如打开一个新的页面、展示详细信息等
            fetch(`${window.baseUrl}/projects/${data.node.id}/moveFileName?parentProjectResourceId=${parentId}`, {
                method: 'GET' // 指定请求方法为 POST
            }).then(response => response.text())
                .then(content => {
                    let data = JSON.parse(content);
                    editor.getModel().config = data;
                    editor.getModel().setValue(data.content);
                })
                .catch(error => console.error('Error fetching file:', error));
            // 在这里执行你的逻辑，例如发送请求到服务器更新数据库
        });
    }

});

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

