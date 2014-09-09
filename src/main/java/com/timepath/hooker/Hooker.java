package com.timepath.hooker;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    private static Method findMethod(Class c, String name) {
        for (Method m : c.getDeclaredMethods()) {
            if (name.equals(m.getName())) return m;
        }
        return null;
    }

    public static void hook(final HookFilter filter, final Hook hook, String main, String... args) throws Throwable {
        final String hooks = Hooks.class.getName();
        ClassPool cp = ClassPool.getDefault();
        final Loader cl = new Loader(Thread.currentThread().getContextClassLoader(), cp);
        final Object proxy = Proxy.newProxyInstance(cl, new Class[]{cl.loadClass(Hook.class.getName())}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return findMethod(Hook.class, method.getName()).invoke(hook, args);
            }
        });
        final Method put = findMethod(cl.loadClass(hooks), "put");
        cl.addTranslator(cp, new Translator() {

            @Override
            public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
            }

            @Override
            public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
                if (!filter.accept(classname)) return;
                try {
                    put.invoke(null, classname, proxy);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                // https://rawgit.com/jboss-javassist/javassist/master/tutorial/tutorial2.html#before
                String format = "{ " + hooks + ".%s(\"" + classname + "\", (Object) %s, \"%s\", \"%s\", $args); }";
                CtClass cc = pool.get(classname);
                for (CtMethod m : cc.getDeclaredMethods()) {
                    boolean isInstance = (m.getModifiers() & AccessFlag.STATIC) == 0;
                    String instance = isInstance ? "this" : "null";
                    try {
                        m.insertBefore(String.format(format, "before", instance, m.getDeclaringClass().getName(), m.getName()));
                        m.insertAfter(String.format(format, "after", instance, m.getDeclaringClass().getName(), m.getName()), true);
                    } catch (CannotCompileException e) {
                        LOG.log(Level.SEVERE, m.getLongName(), e);
                        throw e;
                    }
                }
            }
        });
        cl.run(main, args);
    }

    public static interface Hook {

        void before(Object inst, String owner, String method, Object[] args);

        void after(Object inst, String owner, String method, Object[] args);
    }

    public static interface HookFilter {

        boolean accept(String classname);

    }

    @SuppressWarnings("UnusedDeclaration")
    public static class Hooks {

        private static final Map<String, Hook> hooks = Collections.synchronizedMap(new HashMap<String, Hook>());

        public static void put(String classname, Hook hook) {
            hooks.put(classname, hook);
        }

        public static void before(String classname, Object inst, String owner, String method, Object[] args) {
            hooks.get(classname).before(inst, owner, method, args);
        }

        public static void after(String classname, Object inst, String owner, String method, Object[] args) {
            hooks.get(classname).after(inst, owner, method, args);
        }
    }
}
