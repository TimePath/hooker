package com.timepath.hooker;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Hooker {

    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static Method findMethod(Class c, String name) {
        for (Method m : c.getDeclaredMethods()) {
            if (name.equals(m.getName())) return m;
        }
        return null;
    }

    /**
     * Run an entry point with targeted hooks.
     *
     * @param filter a filter targeting specific classes
     * @param hook   the hook to attach to every method in every class matching the filter
     * @param main   the entry point to start
     * @param args   the command line arguments
     * @throws Throwable
     */
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
                String format = "{ " + hooks + ".%s(\"" + classname + "\", (Object) %s, \"%s\", \"%s\", %s); }";
                CtClass cc = pool.get(classname);
                for (CtMethod m : cc.getDeclaredMethods()) {
                    boolean isInstance = (m.getModifiers() & AccessFlag.STATIC) == 0;
                    String instance = isInstance ? "this" : "null";
                    try {
                        m.insertBefore(String.format(format, "before", instance, m.getDeclaringClass().getName(), m.getMethodInfo2(), "$args"));
                        m.insertAfter(String.format(format, "after", instance, m.getDeclaringClass().getName(), m.getMethodInfo2(), "new Object[] {($w) $_}"), true);
                    } catch (CannotCompileException e) {
                        LOG.log(Level.SEVERE, m.getLongName(), e);
                        throw e;
                    }
                }
            }
        });
        cl.run(main, args);
    }

}
