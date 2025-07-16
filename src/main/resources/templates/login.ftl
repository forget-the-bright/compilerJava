<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>登录</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">



<div class="container d-flex align-items-center justify-content-center min-vh-100">
    <div class="card shadow-sm w-100" style="max-width: 400px;">
        <!-- 卡片头部：标题 -->
        <div class="card-header d-flex align-items-center justify-content-center">
            <h1 class="h3 fw-normal text-primary m-0 text-center">在线 Java 编译器</h1>
        </div>
        <div class="card-body">
            <h4 class="card-title text-center mb-4">用户登录</h4>

            <!-- 错误提示 -->
            <#if error??>
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <strong>登录失败：</strong> ${error}
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            </#if>
            <div class="mt-3 text-center">
                <a href="${domainUrl}/register">没有账号？立即注册</a>
            </div>
            <!-- 登录表单 -->
            <form action="${domainUrl}/login" method="post">
                <div class="mb-3">
                    <label for="username" class="form-label">用户名</label>
                    <input type="text" class="form-control" id="username" name="username" value="wanghao" required>
                </div>
                <div class="mb-3">
                    <label for="password" class="form-label">密码</label>
                    <input type="password" class="form-control" id="password" name="password" value="" required>
                </div>
                <button type="submit" class="btn btn-primary w-100">登录</button>
            </form>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
</body>
</html>