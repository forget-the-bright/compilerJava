<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <title>在线代码编辑器</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>项目管理</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/css/bootstrap.min.css" rel="stylesheet"
          crossorigin="anonymous">
    <link rel="stylesheet" href="${domainUrl}/css/index.css">
    <script>
        window.baseUrl = "${domainUrl}";
        window.wsUrl = "${wsUrl}";
    </script>
</head>

<body>
<div class="toolbar">
    <div>
        <button type="button" class="btn btn-primary" onclick="createProject()">创建项目</button>
        <button type="button" class="btn btn-primary" onclick="deleteProject()">删除项目</button>
        <button type="button" class="btn btn-primary" onclick="enterProject()">进入项目</button>
        <button type="button" class="btn btn-primary" onclick="editProject()">修改项目</button>
        <!-- 触发按钮 -->
        <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#exampleModal">
            弹窗模态框
        </button>
    </div>
    <div>
        <span>项目总数: <span id="projectCount">${projectSize}</span></span>
    </div>
</div>
<div class="content-container">
    <div class="content" id="content">
        <#list projects as project>
           <#-- <div class="card">
                <h3>${project.name}</h3>
                <p>这是一个简单的项目描述。项目入口类: ${project.mainClass}</p>
            </div>-->

            <div class="card">
<#--                <img src="..." class="card-img-top" alt="...">-->
                <div class="card-body">
                    <h5 class="card-title"> ${project.name} </h5>
                    <p class="card-text">这是一个简单的项目描述。项目入口类: ${project.mainClass}</p>
                    <a href="${domainUrl}/editor?projectId=${project.id}" class="btn btn-primary">进入项目</a>
                </div>
            </div>
        </#list>
    </div>
</div>
<!-- 模态框HTML -->
<!-- Modal -->
<div class="modal fade" id="exampleModal" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="exampleModalLabel">Modal title</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <form id="dataForm">
                    <!-- 隐藏ID -->
                    <input type="hidden" id="id" name="id" value="">

                    <!-- 主类 -->
                    <div class="mb-3">
                        <label for="category" class="form-label">主类（分类）</label>
                        <input type="text" class="form-control" id="category" name="category" required>
                    </div>

                    <!-- 标题 -->
                    <div class="mb-3">
                        <label for="title" class="form-label">标题</label>
                        <input type="text" class="form-control" id="title" name="title" required>
                    </div>

                    <!-- 描述 -->
                    <div class="mb-3">
                        <label for="description" class="form-label">描述</label>
                        <textarea class="form-control" id="description" name="description" rows="3"></textarea>
                    </div>

                    <!-- 提交按钮 -->
                    <button type="submit" class="btn btn-primary">提交</button>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary">Save changes</button>
            </div>
        </div>
    </div>
</div>
<#noparse>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/js/bootstrap.bundle.min.js"
            crossorigin="anonymous"></script>
    <script>
        let projects = [];
        const projectContainer = document.getElementById('content');
        const projectCountSpan = document.getElementById('projectCount');

        function createProject() {
            const projectName = `项目 ${projects.length + 1}`;
            projects.push(projectName);
            renderProjects();
        }

        function deleteProject() {
            if (projects.length > 0) {
                projects.pop();
                renderProjects();
            }
        }

        function enterProject() {
            alert('进入项目功能待实现');
            let projectId = "1";
            window.location.href = `${window.baseUrl}/editor?projectId=${projectId}`;
        }

        function editProject() {
            alert('修改项目功能待实现');
        }

        function renderProjects() {
            projectContainer.innerHTML = '';
            projects.forEach((project, index) => {
                const card = document.createElement('div');
                card.className = 'card';
                card.innerHTML = `
                    <h3>${project}</h3>
                    <p>这是一个简单的项目描述。</p>
                `;
                projectContainer.appendChild(card);
            });
            projectCountSpan.textContent = projects.length;
        }

        window.addEventListener('resize', renderProjects);
    </script>
</#noparse>
</body>

</html>