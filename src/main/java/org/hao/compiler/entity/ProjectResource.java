package org.hao.compiler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:55
 */
//@Entity
@TableName(value = "PROJECT_RESOURCE", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResource extends Model<ProjectResource> {

    /*  @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)*/
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField(value = "PROJECT_ID")
    private Long projectId; // 所属项目ID

    @TableField(value = "PARENT_ID")
    private Long parentId; // 父节点ID（为null表示根节点）

    @TableField(value = "NAME")
    private String name; // 包路径或文件名

    //    @Enumerated(EnumType.STRING)
    @TableField(value = "TYPE")
    private ResourceType type; // DIRECTORY or FILE

    //    @Lob
//    @Column(name = "content", columnDefinition = "TEXT")
    @TableField(value = "CONTENT")
    private String content; // 只有FILE才有内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "CREATE_TIME")
    private Date createTime;

    //    @Transient
    @TableField(exist = false)
    @JsonIgnore
    private List<ProjectResource> children;

    // 构造方法辅助创建目录或文件
    public static ProjectResource ofDir(String name, Long projectId, Long parentId) {
        return new ProjectResource(null, projectId, parentId, name, ResourceType.DIRECTORY, null, new Date(), null);
    }

    public static ProjectResource ofFile(String name, String content, Long projectId, Long parentId) {
        return new ProjectResource(null, projectId, parentId, name, ResourceType.FILE, content, new Date(), null);
    }
}


