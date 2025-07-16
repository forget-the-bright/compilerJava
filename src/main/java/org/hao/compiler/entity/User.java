package org.hao.compiler.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:18
 */

@Data
@TableName("user")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户信息表")
public class User extends Model<User> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    /**
     * 用户名，唯一
     */
    @Schema(description = "用户名，唯一")
    private String userName;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickName;

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱地址")
    private String email;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码")
    private String mobileNumber;

    /**
     * 密码哈希值
     */
    @Schema(description = "密码哈希值")
    private String passwordHash;

    /**
     * 密码加密盐值
     */
    @Schema(description = "密码加密盐值")
    private String passwordSalt;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL")
    private String avatarUrl;

    /**
     * 性别：0未知，1男，2女
     */
    @Schema(description = "性别：0未知，1男，2女")
    private Byte gender;

    /**
     * 出生日期
     */
    @Schema(description = "出生日期")
    private Date birthDate;

    /**
     * 注册时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "注册时间")
    private Date registerTime;

    /**
     * 最后登录时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间")
    private Date lastLoginTime;

    /**
     * 账号状态：0禁用，1启用
     */
    @Schema(description = "账号状态：0禁用，1启用")
    private Integer status;

    /**
     * 是否删除：0未删，1已删
     */
    @Schema(description = "是否删除：0未删，1已删")
    private Integer isDeleted;

    /**
     * 创建人ID或用户名
     */
    @Schema(description = "创建人ID或用户名")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新人ID或用户名
     */
    @Schema(description = "更新人ID或用户名")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
