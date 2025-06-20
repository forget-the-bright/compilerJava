// layout-config.js
var layoutConfig = {
    content: [{
        type: 'column',
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
                            componentState: { title: 'Java 编辑器' },
                        }]
                    },
                    {
                        type: 'column',
                        width: 30,
                        content: [{
                            type: 'component',
                            componentName: 'output',
                            componentState: { title: '编译输出' },
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
                    componentState: { title: '实时日志' },
                }]
            }
        ]
    }]
};