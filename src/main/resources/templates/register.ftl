<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>注册</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<div class="container d-flex align-items-center justify-content-center min-vh-100">
    <div class="card shadow-sm w-100" style="max-width: 450px;">
        <!-- 卡片头部：标题 -->
        <div class="card-header d-flex align-items-center justify-content-center">
            <h1 class="h3 fw-normal text-primary m-0 text-center">在线 Java 编译器</h1>
        </div>
        <div class="card-body">
            <h4 class="card-title text-center mb-4">用户注册</h4>

            <!-- 错误提示 -->
            <#if error??>
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <strong>注册失败：</strong> ${error}
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            </#if>

            <!-- 注册表单 -->
            <form action="${domainUrl}/register" method="post">
                <div class="mb-3">
                    <label for="user_name" class="form-label">用户名</label>
                    <input type="text" class="form-control" id="user_name" name="user_name" required>
                </div>
                <div class="mb-3">
                    <label for="nick_name" class="form-label">昵称</label>
                    <input type="text" class="form-control" id="nick_name" name="nick_name">
                </div>
                <div class="mb-3">
                    <label for="email" class="form-label">邮箱</label>
                    <input type="email" class="form-control" id="email" name="email">
                </div>
                <div class="mb-3">
                    <label for="mobile_number" class="form-label">手机号</label>
                    <input type="text" class="form-control" id="mobile_number" name="mobile_number">
                </div>
                <div class="mb-3">
                    <label for="password" class="form-label">密码</label>
                    <input type="password" class="form-control" id="password" name="password" required>
                </div>
                <div class="mb-3">
                    <label for="confirm_password" class="form-label">确认密码</label>
                    <input type="password" class="form-control" id="confirm_password" name="confirm_password" required>
                </div>
                <button type="submit" class="btn btn-success w-100">注册</button>
            </form>

            <div class="mt-3 text-center">
                <a href="${domainUrl}/login">已有账号？去登录</a>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
</body>
</html>