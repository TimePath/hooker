package com.timepath.hooker;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Hooker {

    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public static void main(String[] args) throws Throwable {
        args = new String[]{"com.timepath.hooker.Test", "com.timepath.hooker.Test"};
        if (args.length < 2) {
            System.err.println("Usage: <prefix> <Main>, [args]");
            System.exit(1);
        }
        final String prefix = args[0];
        final String main = args[1];
        String[] sub = new String[args.length - 2];
        System.arraycopy(args, 2, sub, 0, sub.length);
        hook(new HookFilter() {
            @Override
            public boolean accept(String classname) {
                return classname.startsWith(prefix);
            }
        }, main);
    }

    public static void hook(final HookFilter filter, String main, String... args) throws Throwable {
        ClassPool cp = ClassPool.getDefault();
        final Loader cl = new Loader(cp);
        cl.addTranslator(cp, new Translator() {

            @Override
            public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
            }

            @Override
            public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
                if (!filter.accept(classname)) return;
                // https://rawgit.com/jboss-javassist/javassist/master/tutorial/tutorial2.html#before
                String format = "{ %s.%s((Object)%s, \"%s\", \"%s\", $args); }";
                CtClass cc = pool.get(classname);
                for (CtMethod m : cc.getDeclaredMethods()) {
                    boolean isInstance = (m.getModifiers() & AccessFlag.STATIC) == 0;
                    String instance = isInstance ? "this" : "null";
                    try {
                        m.insertBefore(String.format(format, Hooker.class.getName(), "before", instance, m.getDeclaringClass().getName(), m.getName()));
                        m.insertAfter(String.format(format, Hooker.class.getName(), "after", instance, m.getDeclaringClass().getName(), m.getName()), true);
                    } catch (CannotCompileException e) {
                        LOG.log(Level.SEVERE, m.getLongName(), e);
                        throw e;
                    }
                }
            }
        });
        cl.run(main, args);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void before(Object inst, String owner, String method, Object[] args) {
        LOG.info("{ " + inst + " " + owner + " " + method + "\t" + Arrays.deepToString(args));
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void after(Object inst, String owner, String method, Object[] args) {
        LOG.info("} " + inst + " " + owner + " " + method + "\t" + Arrays.deepToString(args));
    }

    public static interface HookFilter {

        boolean accept(String classname);

    }
}
