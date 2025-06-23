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
var layoutConfig = {
    settings: otherLayoutConfig.settings,
    dimensions: otherLayoutConfig.dimensions,
    labels: otherLayoutConfig.labels,
    content: [
        {
            type: 'row',
            content: [
                {
                    type: 'column',
                    width: 10,
                    content: [
                        {
                            type: 'component',
                            componentName: 'fileBrowser',
                            componentState: {title: '文件预览'},
                            // 👇 关键配置
                            isClosable: false,
                            collapsible: true, // 启用折叠功能
                            collapsed: false   // 初始是否折叠
                        }
                    ]
                },
                {
                    type: 'column',
                    width: 90,
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
                                        componentState: {title: 'Java 编辑器'},
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
                                        componentState: {title: '编译输出'},
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
                                componentState: {title: '实时日志'},
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