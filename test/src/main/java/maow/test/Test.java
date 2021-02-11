package maow.test;

import maow.optionals.annotations.Optional;

public class Test {
    public static void main(String[] args) {
        final Test test = new Test();
        test.test();
    }

    public void test(@Optional String s) {
        System.out.println(s);
    }
}
