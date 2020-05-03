import bat.ke.qq.com.bean.Cat;
import bat.ke.qq.com.common.MyApplicationContextAware;
import bat.ke.qq.com.scope.AppConfig;
import bat.ke.qq.com.scope.Prototype;
import bat.ke.qq.com.test.User;
import bat.ke.qq.com.test.UserCommon;
import bat.ke.qq.com.test.UserComponent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.jupiter.api.Test;

public class MyIocTest {
    @Test
    public void test() {
        // xml方式

        // ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");

        // 为什么加&符号返回的是原生的
        // 通过Spring容器的getBean()方法返回的不是FactoryBean本身，
        // 而是FactoryBean#getObject()方法所返回的对象，相当于FactoryBean#getObject()代理了getBean()方法

        // Cat myApplicationContextAware = (Cat) context.getBean("myApplicationContextAware");
        // Cat myApplicationContextAware1 = (Cat) context.getBean("myApplicationContextAware");

        // System.out.println(context.getBean("myApplicationContextAware"));// getObject()
        // System.out.println(context.getBean("&myApplicationContextAware"));// myApplicationContextAware

        // annotation

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyApplicationContextAware.class);
        context.refresh();
        System.out.println(context.getBean("myApplicationContextAware"));
        System.out.println(context.getBean("&myApplicationContextAware"));

    }

    @Test
    public void scopeTest() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class, bat.ke.qq.com.scope.Test.class);
        bat.ke.qq.com.scope.Test test = (bat.ke.qq.com.scope.Test) context.getBean("test");
        test.run();
    }

    @Test
    public void Test2() {
        AnnotationConfigApplicationContext annotationConfigApplicationContext =
                new AnnotationConfigApplicationContext(UserCommon.class, User.class, UserComponent.class);
        Object a = annotationConfigApplicationContext.getBean("a");
        System.out.println(a.getClass());
    }
}
