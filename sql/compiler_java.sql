/*
 Navicat Premium Data Transfer

 Source Server         : debain-app_3306
 Source Server Type    : MySQL
 Source Server Version : 80200
 Source Host           : 192.168.3.199:3306
 Source Schema         : compiler_java

 Target Server Type    : MySQL
 Target Server Version : 80200
 File Encoding         : 65001

 Date: 18/07/2025 15:43:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for project
-- ----------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project`  (
  `ID` bigint(0) NOT NULL AUTO_INCREMENT,
  `CREATE_TIME` datetime(3) NULL DEFAULT NULL,
  `CREATOR` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `MAIN_CLASS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `MAIN_CLASS_ID` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `DESCRIPTION` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `UPDATOR` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project
-- ----------------------------
INSERT INTO `project` VALUES (1, '2025-06-24 15:13:36.682', 'wanghao', 'demoProject', 'Main', '30', NULL, NULL);
INSERT INTO `project` VALUES (3, '2025-07-07 15:02:31.850', 'wanghao', 'demoMath', 'org.math.Main', '36', NULL, NULL);
INSERT INTO `project` VALUES (4, '2025-07-10 17:18:16.290', 'wanghao', 'demoRunLocal', 'org.hao.Main', '42', NULL, NULL);
INSERT INTO `project` VALUES (5, '2025-07-16 17:13:40.931', 'zhangsan', 'newUserProject', 'org.zhangsan.Main', '45', NULL, NULL);

-- ----------------------------
-- Table structure for project_resource
-- ----------------------------
DROP TABLE IF EXISTS `project_resource`;
CREATE TABLE `project_resource`  (
  `ID` bigint(0) NOT NULL AUTO_INCREMENT,
  `CONTENT` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `CREATE_TIME` datetime(3) NULL DEFAULT NULL,
  `NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `PARENT_ID` bigint(0) NULL DEFAULT NULL,
  `PROJECT_ID` bigint(0) NULL DEFAULT NULL,
  `TYPE` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_resource
-- ----------------------------
INSERT INTO `project_resource` VALUES (1, NULL, '2025-06-24 15:14:39.588', 'org', NULL, 1, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (2, NULL, '2025-06-24 15:23:34.813', 'hao', 1, 1, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (5, 'package org.hao;\r\n\r\nimport org.hao.core.print.PrintUtil;\r\nimport org.hao.spring.SpringRunUtil;\r\nimport org.hao.annotation.LogDefine;\r\nimport lombok.extern.slf4j.Slf4j;\r\n\r\n@Slf4j\r\npublic class Greeter {\r\n    public void run() {\r\n        String javaHome = System.getProperty(\"java.home\");\r\n        PrintUtil.BLUE.Println(\"javaHome: {}\", javaHome);\r\n        PrintUtil.BLUE.Println(\r\n                \"CompilerUtil.getTempDirName(): {}\",\r\n                org.hao.core.compiler.CompilerUtil.getTempDirName());\r\n        sayHello(\"123-\" + 123);\r\n    }\r\n\r\n    @LogDefine(\"123\")\r\n    public String sayHello(String name) {\r\n        System.out.println(\"Hello, \" + name + \"!\");\r\n        PrintUtil.BLUE.Println(\"name = \" + name);\r\n        SpringRunUtil.printRunInfo();\r\n        log.info(\"name:{}\", name);\r\n        lombok.Lombok.preventNullAnalysis(\"123\");\r\n        return this.getClass().getName();\r\n    }\r\n}\r\n', '2025-06-24 15:29:21.000', 'Greeter.java', 2, 1, 'FILE');
INSERT INTO `project_resource` VALUES (30, 'import org.hao.core.print.PrintUtil;\r\nimport lombok.extern.slf4j.Slf4j;\r\nimport org.hao.Greeter;\r\n\r\n@Slf4j\r\npublic class Main {\r\n\r\n    public void run() {\r\n\r\n        PrintUtil.BLUE.Println(\"Hello World!\" + 123);\r\n        log.info(\"name:{}\", this.getClass().getName());\r\n        new Greeter().run();\r\n    }\r\n\r\n    public static void main(String[] args) {\r\n        new Main().run();\r\n    }\r\n}\r\n', '2025-06-26 00:38:54.000', 'Main.java', 0, 1, 'FILE');
INSERT INTO `project_resource` VALUES (31, 'package org.hao;\r\n\r\nimport org.hao.core.print.PrintUtil;\r\nimport lombok.extern.slf4j.Slf4j;\r\n\r\n@Slf4j\r\npublic class temp {\r\n\r\n  public void run() {\r\n    PrintUtil.BLUE.Println(\"Hello World!\");\r\n    log.info(\"name:{}\", this.getClass().getName());\r\n  }\r\n}\r\n', '2025-06-27 00:54:09.000', 'temp.java', 2, 1, 'FILE');
INSERT INTO `project_resource` VALUES (34, NULL, '2025-07-07 15:02:32.281', 'org', 0, 3, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (35, NULL, '2025-07-07 15:02:32.436', 'math', 34, 3, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (36, 'package org.math;\r\n\r\nimport org.hao.core.print.PrintUtil;\r\nimport lombok.extern.slf4j.Slf4j;\r\nimport org.math.util.*;\r\n\r\n@Slf4j\r\npublic class Main {\r\n\r\n    public static void main(String[] args) {\r\n        // testArchFunction();\r\n        // testCopperMassCalculator();\r\n        getDischargeByCopperMassCalculator();\r\n    }\r\n\r\n    public static void getDischargeByCopperMassCalculator() {\r\n        // 示例输入参数\r\n        double h1 = 0.510; // 初始铜面高度（米）= 510 mm\r\n        double h2 = 0.622; // 结束铜面高度（米）= 622 mm\r\n        double feedRate_t_per_hour = 100; // 投料量（吨/小时）\r\n        double time_hours = 3; // 时间差（小时）\r\n\r\n        // 直接计算粗铜排放量\r\n        double discharge =\r\n                CopperMassCalculator.calculateDischarge(h1, h2, feedRate_t_per_hour, time_hours);\r\n        double dischargeTon = discharge / 1000;\r\n        double dischargeRat = dischargeTon / time_hours;\r\n\r\n        // 输出结果\r\n        System.out.printf(\"粗铜排放量kg: %.2f kg\\n\", discharge);\r\n        System.out.printf(\"粗铜排放量t: %.2f t\\n\", dischargeTon);\r\n        System.out.printf(\"粗铜排放速度t/h: %.2f t/h\\n\", dischargeRat);\r\n    }\r\n\r\n    public static void testCopperMassCalculator() {\r\n        // 参数配置（单位统一为国际单位制）\r\n        double r = 16.0; // 拱底半径（米）= 16000 mm\r\n        double L = 22.7; // 炉体长度（米）= 22700 mm\r\n        double density = 8920; // 铜密度（kg/m³）\r\n        double yield_percent = 64; // 冰铜产率（%）\r\n\r\n        double h1 = 0.510; // 初始铜面高度（米）= 510 mm\r\n        double h2 = 0.622; // 结束铜面高度（米）= 622 mm\r\n\r\n        double feedRate_t_per_hour = 100; // 投料量（吨/小时）\r\n        double time_hours = 3; // 时间差（小时）\r\n\r\n        // 单位转换 投料量（kg/小时）\r\n        double feedRate_kg_per_hour = feedRate_t_per_hour * 1000;\r\n\r\n        // 计算 m1 和 m2\r\n        double m1 = CopperMassCalculator.getCopperMass(h1);\r\n        double m2 = CopperMassCalculator.getCopperMass(h2);\r\n\r\n        // 计算 m3\r\n        double m3 = CopperMassCalculator.calculateM3(feedRate_kg_per_hour, time_hours);\r\n\r\n        // 输出结果\r\n        System.out.printf(\"初始铜质量 m1: %.2f kg\\n\", m1);\r\n        System.out.printf(\"结束铜质量 m2: %.2f kg\\n\", m2);\r\n        System.out.printf(\"投入铜量 m3: %.2f kg\\n\", m3);\r\n\r\n        // 粗铜排放量 = m1 - m2 + m3\r\n        double discharge = m1 - m2 + m3;\r\n        double dischargeTon = discharge / 1000;\r\n        double dischargeRat = dischargeTon / time_hours;\r\n        System.out.printf(\"粗铜排放量kg: %.2f kg\\n\", discharge);\r\n        System.out.printf(\"粗铜排放量t: %.2f t\\n\", dischargeTon);\r\n        System.out.printf(\"粗铜排放速度t/h: %.2f t/h\\n\", dischargeRat);\r\n    }\r\n\r\n    public static void testArchFunction() {\r\n        double radius = 16000; // mm → 如果需要以米为单位，请除以 1000\r\n        double h1 = 510; // mm\r\n        double h2 = 622; // mm\r\n\r\n        // 转换为米（可选）\r\n        double r_meters = radius / 1000.0;\r\n        double h1_meters = h1 / 1000.0;\r\n        double h2_meters = h2 / 1000.0;\r\n\r\n        ArchFunction archFunction = new ArchFunction(r_meters);\r\n\r\n        double A1 = archFunction.value(h1_meters); // 初始高度下的截面积（m²）\r\n        double A2 = archFunction.value(h2_meters); // 结束高度下的截面积（m²）\r\n\r\n        System.out.println(\"初始截面积 A1: \" + A1 + \" m²\");\r\n        System.out.println(\"结束截面积 A2: \" + A2 + \" m²\");\r\n    }\r\n}\r\n', '2025-07-07 15:02:32.000', 'Main.java', 35, 3, 'FILE');
INSERT INTO `project_resource` VALUES (37, NULL, '2025-07-07 15:09:37.747', 'util', 35, 3, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (38, 'package org.math.util;\r\n\r\nimport org.hao.core.print.PrintUtil;\r\nimport lombok.extern.slf4j.Slf4j;\r\nimport org.apache.commons.math3.analysis.UnivariateFunction;\r\nimport org.apache.commons.math3.analysis.integration.SimpsonIntegrator;\r\nimport org.apache.commons.math3.analysis.integration.UnivariateIntegrator;\r\n\r\n@Slf4j\r\npublic class ArchFunction implements UnivariateFunction {\r\n    private final double r; // 半径\r\n\r\n    public ArchFunction(double r) {\r\n        this.r = r;\r\n    }\r\n\r\n    @Override\r\n    public double value(double h) {\r\n        // 计算扇形面积部分\r\n        double sectorArea = r * r * Math.acos((r - h) / r);\r\n        // 计算三角形面积部分\r\n        double triangleHeight = r - h;\r\n        double baseLength = Math.sqrt(2 * r * h - h * h);\r\n        double triangleArea = triangleHeight * baseLength;\r\n        // 返回扇形面积减去三角形面积\r\n        return sectorArea - triangleArea;\r\n    }\r\n}\r\n', '2025-07-07 15:09:51.000', 'ArchFunction.java', 37, 3, 'FILE');
INSERT INTO `project_resource` VALUES (39, 'package org.math.util;\r\n\r\nimport org.hao.core.print.PrintUtil;\r\nimport lombok.extern.slf4j.Slf4j;\r\nimport cn.hutool.core.util.StrUtil;\r\n/**\r\n * 熔炼炉中铜质量计算工具类。\r\n *\r\n * <p>提供基于拱底截面积模型计算熔池内铜质量的方法， 并支持粗铜排放量相关计算。\r\n *\r\n * @author wanghao(helloworlwh @ 163.com)\r\n * @since 2025/7/7 下午2:44\r\n */\r\n@Slf4j\r\npublic class CopperMassCalculator {\r\n\r\n    // -----------------------------\r\n    // 常量定义（系统固定参数）\r\n    // -----------------------------\r\n\r\n    /** 拱底半径（单位：米） 对应图纸尺寸 16000 mm */\r\n    public static final double RADIUS = 16.0;\r\n\r\n    /** 炉体有效长度（单位：米） 对应图纸尺寸 22700 mm */\r\n    public static final double LENGTH = 22.7;\r\n\r\n    /** 铜密度（单位：kg/m³） 熔融态下约为 8920 kg/m³ */\r\n    public static final double COPPER_DENSITY = 8920;\r\n\r\n    /** 冰铜产率（单位：%） 表示原料转化为冰铜的比例 */\r\n    public static final double YIELD_PERCENT = 64;\r\n\r\n    // -----------------------------\r\n    // 核心方法定义\r\n    // -----------------------------\r\n\r\n    /**\r\n     * 根据给定高度 h 计算拱底截面积 S(h)。\r\n     *\r\n     * <p>公式： S = r² * arccos((r - h)/r) - (r - h) * √(2rh - h²)\r\n     *\r\n     * @param r 拱底半径（单位：米）\r\n     * @param h 铜液面高度（单位：米，0 ≤ h ≤ r）\r\n     * @return 截面积 S（单位：平方米 m²）\r\n     * @throws IllegalArgumentException 如果 h 超出范围\r\n     */\r\n    public static double getCrossSectionArea(double r, double h) {\r\n        if (h > r || h < 0) {\r\n            throw new IllegalArgumentException(\"高度 h 必须在 [0, r] 范围内\");\r\n        }\r\n\r\n        double sector = r * r * Math.acos((r - h) / r);\r\n        double triangle = (r - h) * Math.sqrt(2 * r * h - h * h);\r\n        return sector - triangle;\r\n    }\r\n\r\n    /**\r\n     * 根据铜液面高度计算熔池中的铜质量 m。 使用系统常量 RADIUS、LENGTH、COPPER_DENSITY。\r\n     *\r\n     * <p>公式： m = ρ * S * L\r\n     *\r\n     * @param h 铜液面高度（单位：米，0 ≤ h ≤ RADIUS）\r\n     * @return 铜质量 m（单位：千克 kg）\r\n     */\r\n    public static double getCopperMass(double h) {\r\n        double area = getCrossSectionArea(RADIUS, h);\r\n        PrintUtil.RED.Println(\"getCrossSectionArea:\" + area + \" h: \" + h);\r\n        return COPPER_DENSITY * area * LENGTH;\r\n    }\r\n\r\n    /**\r\n     * 计算排放期间投入的铜量 m3。 使用系统常量 YIELD_PERCENT。\r\n     *\r\n     * <p>公式： m3 = 投料量 × 时间 × 冰铜产率\r\n     *\r\n     * @param feedRate_kg_per_hour 投料量（单位：kg/h）\r\n     * @param time_hours 排放时间（单位：小时）\r\n     * @return m3（单位：kg）\r\n     */\r\n    public static double calculateM3(double feedRate_kg_per_hour, double time_hours) {\r\n        return feedRate_kg_per_hour * time_hours * (YIELD_PERCENT / 100.0);\r\n    }\r\n\r\n    /**\r\n     * 直接根据输入参数计算粗铜排放量。\r\n     *\r\n     * <p>公式： 排放量 = m1 - m2 + m3\r\n     *\r\n     * @param h1 初始铜面高度（单位：米）\r\n     * @param h2 结束铜面高度（单位：米）\r\n     * @param feedRate_t_per_hour 投料量（单位：吨/小时）\r\n     * @param time_hours 排放持续时间（单位：小时）\r\n     * @return 粗铜排放量（单位：kg）\r\n     */\r\n    public static double calculateDischarge(\r\n            double h1, double h2, double feedRate_t_per_hour, double time_hours) {\r\n        double m1 = getCopperMass(h1);\r\n        PrintUtil.RED.Println(\"getCopperMass m1:\" + m1);\r\n        double m2 = getCopperMass(h2);\r\n        PrintUtil.RED.Println(\"getCopperMass m2:\" + m2);\r\n        double m3 = calculateM3(feedRate_t_per_hour * 1000, time_hours);\r\n        PrintUtil.RED.Println(\"calculateM3 m3:\" + m3);\r\n        return m1 - m2 + m3;\r\n    }\r\n}\r\n', '2025-07-07 15:24:51.000', 'CopperMassCalculator.java', 37, 3, 'FILE');
INSERT INTO `project_resource` VALUES (40, NULL, '2025-07-10 17:18:16.434', 'org', 0, 4, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (41, NULL, '2025-07-10 17:18:16.550', 'hao', 40, 4, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (42, 'package org.hao;\r\n\r\nimport org.hao.core.print.*;\r\nimport org.hao.core.compiler.CompilerUtil;\r\nimport lombok.extern.slf4j.Slf4j;\r\nimport lombok.SneakyThrows;\r\nimport java.io.File;\r\nimport java.util.Optional;\r\nimport java.util.concurrent.TimeUnit;\r\n\r\n@Slf4j\r\npublic class Main {\r\n\r\n    @SneakyThrows\r\n    public static void main(String[] args) throws Exception {\r\n        // 获取 java.home 系统属性值\r\n        String javaHome = System.getProperty(\"java.home\");\r\n        CompilerUtil.classpath.stream().forEach(log::info);\r\n        for (int i = 0; i < 30; i++) {\r\n            TimeUnit.SECONDS.sleep(1);\r\n            log.info(\r\n                    \"{}: {}\",\r\n                    ColorText.Builder().BgBrightCyan().FgBlue().FontBold().build(\"javaHome\"),\r\n                    ColorText.Builder().BgBrightYellow().FgRed().FontBold().build(javaHome));\r\n        }\r\n    }\r\n}\r\n', '2025-07-10 17:18:16.000', 'Main.java', 41, 4, 'FILE');
INSERT INTO `project_resource` VALUES (43, NULL, '2025-07-16 17:13:41.074', 'org', 0, 5, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (44, NULL, '2025-07-16 17:13:41.183', 'zhangsan', 43, 5, 'DIRECTORY');
INSERT INTO `project_resource` VALUES (45, 'package org.zhangsan;\r\n\r\nimport org.hao.core.print.*;\r\nimport lombok.extern.slf4j.Slf4j;\r\nimport lombok.SneakyThrows;\r\nimport java.io.File;\r\nimport java.util.Optional;\r\nimport java.util.concurrent.TimeUnit;\r\n\r\n@Slf4j\r\npublic class Main {\r\n    @SneakyThrows\r\n    public static void main(String[] args) {\r\n        // 获取 java.home 系统属性值\r\n        String javaHome = System.getProperty(\"java.home\");\r\n        for (int i = 0; i < 20; i++) {\r\n            TimeUnit.SECONDS.sleep(1);\r\n            log.info(\r\n                    \"{}-{}: {}\",\r\n                    i + 1,\r\n                    ColorText.Builder().BgBrightCyan().FgBlue().FontBold().build(\"javaHome\"),\r\n                    ColorText.Builder().BgBrightYellow().FgRed().FontBold().build(javaHome));\r\n        }\r\n    }\r\n}\r\n', '2025-07-16 17:13:41.000', 'Main.java', 44, 5, 'FILE');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名，唯一',
  `nick_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱地址',
  `mobile_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号码',
  `password_hash` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希值',
  `password_salt` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码加密盐值',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint(0) NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
  `birth_date` date NULL DEFAULT NULL COMMENT '出生日期',
  `register_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '注册时间',
  `last_login_time` datetime(0) NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '账号状态：0禁用，1启用',
  `is_deleted` tinyint(0) NULL DEFAULT 0 COMMENT '是否删除：0未删，1已删',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人ID或用户名',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人ID或用户名',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_name`(`user_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1945411138023968771 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1945388163417513985, 'wanghao', '忘皓', 'kshelloworldwh@gmail.com', '15128022404', 'eW5RivH5cROCiK0p5qMTl9AGfbutwEf19AXK0i+kdRzhtvM1At9G65SAsowxZjXJRg48VGEvEJh1BEV8Swl5jOEqGUGIMQx5xl0sexrsARVrw6UMCVzqgmTe2T5XnSQFdOqAhnXGGpngKhPUv4SwMfd8IuKwcg/6g3WKrjfrbGk=', 'agQzA1y9y7jGOT9zQhV/Xg==', NULL, 0, NULL, '2025-07-16 15:40:56', '2025-07-18 15:20:35', 1, 0, NULL, '2025-07-16 15:40:55', NULL, '2025-07-16 15:40:55');
INSERT INTO `user` VALUES (1945411138023968770, 'zhangsan', '张三', 'kshelloworldwh@gmail.com', '15128022404', 'GpxpN9K9zLdCR0gT+pPwZnBhv4mJrQ5UmLo42AdsPJFcI354VuG4xR7G+puPk6Elypkhhm8xw0AUZVNAFTCupwUXjg6fGmd93xpWYm6RUPeW3u0PzqX3adDSizLmLzDz/BH2AkRSIQvaYV6+fN/mFc+rHRPZi/KP1NhsJMONwsI=', 'QElhWk8PV1fzjI3lSqNjzA==', NULL, 0, NULL, '2025-07-16 17:12:14', '2025-07-17 17:42:26', 1, 0, NULL, '2025-07-16 17:12:13', NULL, '2025-07-16 17:12:13');

SET FOREIGN_KEY_CHECKS = 1;
