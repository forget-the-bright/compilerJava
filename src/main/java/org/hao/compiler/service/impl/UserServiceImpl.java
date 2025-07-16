package org.hao.compiler.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hao.compiler.entity.User;
import org.hao.compiler.mapper.UserMapper;
import org.hao.compiler.service.UserService;
import org.springframework.stereotype.Service;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:24
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
