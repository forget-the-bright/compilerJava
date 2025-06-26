package org.hao.compiler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*import javax.persistence.Id;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;*/
import java.util.Date;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 14:53
 */
//@Entity
@TableName(value = "PROJECT", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project extends Model<Project> {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField(value = "NAME")
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "CREATE_TIME")
    private Date createTime;

    @TableField(value = "MAIN_CLASS")
    private String mainClass;

    @TableField(value = "CREATOR")
    private String creator;
}
