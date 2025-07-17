<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>认证失败</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<div class="container d-flex align-items-center justify-content-center min-vh-100 text-center">
    <div>
        <h1 class="display-4 text-danger">401 - Unauthorized</h1>
        <p class="lead">${errorMsg}</p>
        <a href="${domainUrl}/login" class="btn btn-primary mt-3">去登录</a>
    </div>
</div>

</body>
</html>