package com.example.compiler.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.example.compiler.util.SseEmitterWriter;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.hao.core.compiler.CompilerUtil;
import org.hao.core.compiler.InMemoryClassLoader;
import org.hao.core.compiler.InMemoryJavaFileManager;
import org.hao.core.compiler.JavaSourceFromString;
import org.hao.core.exception.HaoException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.tools.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class JavaCompilerService {
    public static Class<?> compile(String code, SseEmitter emitter) throws ClassNotFoundException {
        String classNameByCode = getClassNameByCode(code);
        TreeSet<String> classpath = CompilerUtil.loadClassPath();
        JavaCompiler SYSTEM_COMPILER = CompilerUtil.SYSTEM_COMPILER;
        // 获取系统自带的 Java 编译器
        if (SYSTEM_COMPILER == null) {
            throw new RuntimeException("无法获取 Java 编译器，请确保使用的是 JDK 而不是 JRE");
        }

        // 构建内存文件管理器：使用 InMemoryJavaFileManager 管理源码与字节码的内存存储
        InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(SYSTEM_COMPILER.getStandardFileManager(null, null, null));
        List<JavaFileObject> compilationUnits = Arrays.asList(new JavaSourceFromString(classNameByCode, code));
        // 执行编译任务
        // 构造编译任务：将输入的 Java 源码字符串封装为 JavaFileObject 并设置编译参数
        // 创建一个诊断收集器，用于收集编译过程中的信息
        DiagnosticCollector<? super JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        // 创建一个选项列表，用于配置编译任务的参数
        List<String> options = new ArrayList<>();
        if (classpath.isEmpty()) {
            classpath.clear();
            classpath.addAll(CompilerUtil.loadClassPath());
        }
        if (CollUtil.isNotEmpty(classpath)) {
            options.add("-cp");
            options.add(StrUtil.join(File.pathSeparator, classpath));
            List<String> lombokJar = classpath.stream().filter(q -> q.contains("lombok")).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(lombokJar)) {
                options.add("-processorpath");
                options.add(StrUtil.join(File.pathSeparator, lombokJar));
            }
        }
        SseEmitterWriter sseEmitterWriter = null;
        if (emitter != null) {
            sseEmitterWriter = new SseEmitterWriter(emitter);
        }
        // 获取一个编译任务实例
        // 此处省略了SYSTEM_COMPILER和fileManager的初始化过程
        JavaCompiler.CompilationTask task = SYSTEM_COMPILER.getTask(
                sseEmitterWriter, // 不使用Writer对象
                fileManager, // 文件管理器，负责管理编译过程中的文件
                diagnosticCollector, // 诊断收集器，收集编译信息
                options, // 编译选项
                (Iterable) null, // 不使用类路径入口
                compilationUnits); // 编译单元集合，包含需要编译的Java源文件
        // 尝试执行编译任务
        try {
            // 如果编译失败
            if (!task.call()) {
                // 抛出异常，包含编译失败的详细信息
                // todo 这里可以做一些处理,比如编译失败，如果是jar环境，可以清理掉temp-classpath 重新解压加载，
                //  但是问题是，如果代码就是引用了找不到的库，这里重复加载，就会消耗系统性能， 磁盘io
                throw new HaoException("编译失败: " + CompilerUtil.getDiagnosticMessages(diagnosticCollector));
            }
        } finally {
            // 确保文件管理器被正确关闭，释放资源
            IoUtil.close(fileManager);
        }
        // 创建类加载器并加载编译后的类
        // 如果未指定父类加载器，则使用当前线程的上下文类加载器
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();

        // 创建自定义的类加载器实例，用于加载内存中的字节码
        InMemoryClassLoader classLoader = new InMemoryClassLoader(parentClassLoader);

        // 遍历已编译的类集合，将每个类的字节码添加到类加载器中
        for (Map.Entry<String, InMemoryJavaFileManager.ByteCodeJavaFileObject> entry : fileManager.getCompiledClasses().entrySet()) {
            classLoader.addClassBytes(entry.getKey(), entry.getValue().getByteCode());
        }

        // 使用自定义类加载器加载指定名称的类
        Class<?> aClass = classLoader.loadClass(classNameByCode);

        // 将加载的类缓存起来，以便后续使用

        // 返回加载的类
        return aClass;
    }


    public static String getClassNameByCode(String code) {
        CompilationUnit parse = StaticJavaParser.parse(code);
        String packageName = parse.getPackageDeclaration().orElse(new PackageDeclaration().setName("empty")).getName().toString();
        String className = "";
        TypeDeclaration<?> type = parse.getType(0);
        className = type.getName().toString();

        return packageName.equals("empty") ? className : packageName + "." + className;
    }


} 