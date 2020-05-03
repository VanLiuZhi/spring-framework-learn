package bat.ke.qq.com.scope;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description
 * @Author VanLiuZhi
 * @Date 2020-04-14 16:26
 */
public class Single {
    public Single(){
        System.out.println("Single构造方法");
    }
    @Autowired
    private Prototype prototype;
    // public Prototype getPrototype() {
    //     return prototype;
    // }
    // public void setPrototype(Prototype prototype) {
    //     this.prototype = prototype;
    // }
    public void say() {
        System.out.println(this);
        prototype.say();
    }
}
