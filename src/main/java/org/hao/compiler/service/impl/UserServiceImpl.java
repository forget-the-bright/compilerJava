package org.hao.compiler.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hao.compiler.entity.User;
import org.hao.compiler.mapper.UserMapper;
import org.hao.compiler.service.UserService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:24
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public User getUserByName(String username) {
        return this.getOne(Wrappers.lambdaQuery(User.class).eq(User::getUserName, username).last("limit 1"));
    }

    // 模拟注册逻辑（密码加密）
    public void register(String userName, String nickName, String email, String mobileNumber, String rawPassword) {
        // 生成盐值
        String salt = generateSalt(16);
        // 使用 PBKDF2 加密密码
        String passwordHash = hashPassword(rawPassword, salt);

        User one = getUserByName(userName);
        if (one != null) {
            throw new RuntimeException("用户名已存在");
        }
        // TODO: 插入数据库
        new User()
                .setUserName(userName)
                .setPasswordHash(passwordHash)
                .setPasswordSalt(salt)
                .setNickName(nickName)
                .setEmail(email)
                .setMobileNumber(mobileNumber)
                .setRegisterTime(new Date())
                .setLastLoginTime(new Date())
                .setStatus(1)
                .setIsDeleted(0)
                .insert();
    }

    // 生成盐值
    private String generateSalt(int length) {
        byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 使用 PBKDF2 对密码进行哈希加密
    public String hashPassword(String password, String salt) {
        try {
            int iterations = 65536;
            int keyLength = 128;
            char[] chars = password.toCharArray();
            byte[] saltBytes = Base64.getDecoder().decode(salt);

            PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, iterations, keyLength * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
