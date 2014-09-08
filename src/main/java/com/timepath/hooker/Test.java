package com.timepath.hooker;

public class Test {

    public static void main(String[] args) {
        test(args);
        test("a");
        test("a", "b");
        new Test().test();
    }

    static void test(Object... a) {
    }

    void test() {
    }

}
