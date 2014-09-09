package com.timepath.hooker;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.logging.Logger;

public class Test {

    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public static void main(String[] args) throws Throwable {
        args = new String[]{"com.timepath.hooker.Test", Demo.class.getName()};
        if (args.length < 2) {
            System.err.println("Usage: <prefix> <Main>, [args]");
            System.exit(1);
        }
        final String prefix = args[0];
        final String main = args[1];
        String[] sub = new String[args.length - 2];
        System.arraycopy(args, 2, sub, 0, sub.length);
        Hooker.hook(new HookFilter() {
            @Override
            public boolean accept(String classname) {
                return classname.startsWith(prefix);
            }
        }, new Hook() {
            @Override
            public void before(Object inst, String owner, String method, Object[] args) {
                LOG.info("{ " + inst + " " + owner + " " + method + "\t" + Arrays.deepToString(args));
            }

            @Override
            public void after(Object inst, String owner, String method, Object[] args) {
                LOG.info("} " + inst + " " + owner + " " + method + "\t" + Arrays.deepToString(args));
            }
        }, main);
    }

    public static class Demo {

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
            new Demo().test();
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

}
