package org.hao.compiler.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hao.compiler.entity.User;
import org.hao.compiler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:26
 */


@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户增删改查接口")
public class UserController {

    @Autowired
    private UserService userService;

    // 创建用户
    @PostMapping
    @Operation(summary = "创建用户")
    public Boolean createUser(@RequestBody User user) {
        return userService.save(user);
    }

    // 查询所有用户
    @GetMapping
    @Operation(summary = "查询所有用户")
    public List<User> getAllUsers() {
        return userService.list();
    }

    // 分页查询用户
    @GetMapping("/page")
    @Operation(summary = "分页查询用户")
    public Page<User> getUsersByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return userService.page(new Page<>(pageNum, pageSize));
    }

    // 查询单个用户
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    public User getUserById(@PathVariable String id) {
        return userService.getById(id);
    }

    // 更新用户
    @PutMapping("/{id}")
    @Operation(summary = "根据ID更新用户")
    public Boolean updateUser(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        return userService.updateById(user);
    }

    // 删除用户
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除用户")
    public Boolean deleteUser(@PathVariable String id) {
        return userService.removeById(id);
    }
}
