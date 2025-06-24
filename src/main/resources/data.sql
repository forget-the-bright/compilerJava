-- 1 +/- SELECT COUNT(*) FROM PUBLIC.PROJECT;
INSERT INTO "PUBLIC"."PROJECT" VALUES
    (1, TIMESTAMP '2025-06-24 15:13:36.682', 'wanghao', 'demoProject');

-- 3 +/- SELECT COUNT(*) FROM PUBLIC.PROJECT_RESOURCE;
INSERT INTO "PUBLIC"."PROJECT_RESOURCE" VALUES
                                            (1, NULL, TIMESTAMP '2025-06-24 15:14:39.588', 'org', NULL, 1, 'DIRECTORY'),
                                            (2, NULL, TIMESTAMP '2025-06-24 15:23:34.813', 'hao', 1, 1, 'DIRECTORY'),
                                            (5, U&'package com.example.demo;\000d\000a\000d\000aimport org.hao.core.print.PrintUtil;\000d\000aimport org.hao.spring.SpringRunUtil;\000d\000aimport org.hao.annotation.LogDefine;\000d\000aimport lombok.extern.slf4j.Slf4j;\000d\000a\000d\000a@Slf4j\000d\000apublic class Greeter {\000d\000a    public void run(){\000d\000a        sayHello("123-"+123);\000d\000a    }\000d\000a    @LogDefine("123")\000d\000a    public String  sayHello(String name) {\000d\000a        System.out.println("Hello, " + name + "!");\000d\000a        PrintUtil.BLUE.Println("name = " + name);\000d\000a        SpringRunUtil.printRunInfo();\000d\000a        log.info("name:{}",name);\000d\000a        lombok.Lombok.preventNullAnalysis("123");\000d\000a        return this.getClass().getName();\000d\000a    }\000d\000a}\000d\000a', TIMESTAMP '2025-06-24 15:29:21', 'demo.java', 2, 1, 'FILE');
