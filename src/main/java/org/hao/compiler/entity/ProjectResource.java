package org.hao.compiler.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:55
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId; // 所属项目ID

    private Long parentId; // 父节点ID（为null表示根节点）

    private String name; // 包路径或文件名

    @Enumerated(EnumType.STRING)
    private ResourceType type; // DIRECTORY or FILE

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 只有FILE才有内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // 构造方法辅助创建目录或文件
    public static ProjectResource ofDir(String name, Long projectId, Long parentId) {
        return new ProjectResource(null, projectId, parentId, name, ResourceType.DIRECTORY, null, new Date());
    }

    public static ProjectResource ofFile(String name, String content, Long projectId, Long parentId) {
        return new ProjectResource(null, projectId, parentId, name, ResourceType.FILE, content, new Date());
    }
}


