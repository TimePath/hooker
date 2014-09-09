package com.timepath.hooker;

public class Test {

    public static void main(String[] args) {
        test(args);
        test(null);
        test(new Object[]{null});
        test("a");
        test("a", "b");
        test(new String[]{"c", "d"});
        test(new String[]{"c", "d"}, "e");
        new StaticInner().test();
        StaticInner.test(7, 8);
        new Test().test();
    }

    static void test(Object... a) {
    }

    void test() {
        new EnclosedInner().test();
    }

    static class StaticInner {
        static void test(int o, int j) {
        }

        void test() {
        }
    }

    class EnclosedInner {
        void test() {
        }
    }

}
