package bat.ke.qq.com.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;

@Component
public class A {

    private String name;
    private int age;


    @Autowired
    private B b;

    public A() {
    }


    public A(B b) {

    }

    public void setB(B b) {
        this.b = b;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
