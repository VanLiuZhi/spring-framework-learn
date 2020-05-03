package bat.ke.qq.com.scope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author VanLiuZhi
 * @Date 2020-04-14 16:26
 */
@Component
public class Test {
    @Autowired
    Single single;
    public void run() {
        for (int i = 0; i < 5; i++) {
            // 这里打印的一直是同一个Prototype，因为Prototype注入给Single只有容器启动的那次
            // 可以把Prototype改为每次从context取
            // 使用@Lookup注解
            single.say();
        }
    }
}
