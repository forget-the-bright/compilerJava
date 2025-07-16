package org.hao.compiler.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hao.compiler.entity.User;
/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:22
 */



public interface UserService extends IService<User> {
    void register(String userName, String nickName, String email, String mobileNumber, String password);

    User getUserByName(String username);
    String hashPassword(String password, String salt);
}
