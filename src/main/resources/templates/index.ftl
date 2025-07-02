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
        <button type="button" class="btn btn-primary" onclick="editOrCreateProject()">创建项目</button>
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
                    <p class="card-text">这是一个简单的项目描述。</p>
                    <p class="card-text">项目入口类: ${project.mainClass}</p>
                    <input type="button" class="btn btn-primary d-inline"
                           onclick="enterProject(${project.id})" value="进入项目"/>
                    <input type="button" class="btn btn-primary d-inline" data-bs-toggle="modal"
                           onclick="editOrCreateProject(${project.id})" value="修改项目"/>
                    <input type="button" class="btn btn-danger d-inline" value="删除项目"/>
                </div>
            </div>
        </#list>
    </div>
</div>
<!-- 模态框HTML -->
<!-- Modal -->
<div class="modal fade" id="exampleModal" tabindex="-1" data-bs-backdrop="static" aria-labelledby="exampleModalLabel"
     aria-hidden="true">
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

                    <!-- 名称 -->
                    <div class="mb-3">
                        <label for="name" class="form-label">名称 </label>
                        <input type="text" class="form-control" id="name" name="name" required>
                    </div>
                    <!-- 主类 -->
                    <div class="mb-3">
                        <label for="mainClass" class="form-label">主类（分类）</label>
                        <input type="text" class="form-control" id="mainClass" name="mainClass" required>
                    </div>

                    <!-- 标题 -->
                    <div class="mb-3">
                        <label for="creator" class="form-label">创建者</label>
                        <input type="text" class="form-control" id="creator" name="creator" required>
                    </div>

                    <!-- 描述 -->
                    <div class="mb-3">
                        <label for="description" class="form-label">描述</label>
                        <textarea class="form-control" id="description" name="description" rows="3"></textarea>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                <button type="submit" form="dataForm" class="btn btn-primary">提交</button>
            </div>
        </div>
    </div>
</div>
<#noparse>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/js/bootstrap.bundle.min.js"
            crossorigin="anonymous"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <script>
        let projects = [];
        let options = {
            backdrop: 'static',
            keyboard: true,
            focus: true
        }
        const projectContainer = document.getElementById('content');
        const projectCountSpan = document.getElementById('projectCount');
        var myModal = new bootstrap.Modal(document.getElementById('exampleModal'), options)

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

        function enterProject(projectId) {
            if (!projectId) return;
            window.location.href = `${window.baseUrl}/editor?projectId=${projectId}`;
        }

        function editOrCreateProject(projectId) {
            let title = "";
            var modal = $('#exampleModal')
            var modalTitle = modal.find('.modal-title');

            var dataForm = $('#dataForm')
            let id = dataForm.find('input[name="id"]').first();
            let name = dataForm.find('input[name="name"]').first();
            let mainClass = dataForm.find('input[name="mainClass"]').first();
            let creator = dataForm.find('input[name="creator"]').first();
            if (projectId) {
                title = "修改项目"
                $.ajax({
                    url: `${window.baseUrl}/projects/getProjectInfo?projectId=${projectId}`,
                    method: 'GET',
                    success: function (response) {
                        console.log(response)
                        if (!response) {
                            alert("项目不存在")
                            throw new Error("项目不存在");
                        }

                        id.val(response.id);
                        name.val(response.name);
                        mainClass.val(response.mainClass);
                        creator.val(response.creator);
                        // 更新模态框内容
                        modalTitle.text(title);
                        dataForm.attr('action', `${window.baseUrl}/projects/${projectId}/updateProject`)
                        dataForm.attr('method', 'POST')
                        myModal.show()
                    },
                    error: function (error) {
                        console.error('错误:', error);
                    }
                });
            } else {
                title = "新增项目"

                id.val('');
                name.val('');
                mainClass.val('');
                creator.val('');
                dataForm.attr('action', `${window.baseUrl}/projects`)
                dataForm.attr('method', 'POST')

                // 更新模态框内容
                modalTitle.text(title);
                myModal.show()
            }


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


        $('#dataForm').on('submit', function (event) {
            event.preventDefault(); // 阻止默认提交行为
            // 使用$.ajax()进行AJAX提交
            $.ajax({
                url: $(this).attr('action'),
                method: $(this).attr('method'),
                contentType: 'application/json', // 设置请求头 Content-Type
                dataType: 'json', // 预期服务器返回的数据类型
                data: JSON.stringify(serializeToObject(this)), // 将 JavaScript 对象转换为 JSON 字符串作为 body 数据
                success: function (response) {
                    console.log('成功:', response);
                },
                error: function (error) {
                    console.error('错误:', error);
                }
            });
        });

        function serializeToObject(form) {
            const obj = {};
            const formData = new FormData(form);
            for (const [key, value] of formData.entries()) {
                obj[key] = value;
            }
            return obj;
        }
        //window.addEventListener('resize', renderProjects);
    </script>
</#noparse>
</body>

</html>