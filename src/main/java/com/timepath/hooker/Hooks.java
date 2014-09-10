package com.timepath.hooker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User code should not use this class. Maintains hook state.
 *
 * @author TimePath
 */
public class Hooks {

    private static final Map<String, Hook> hooks = Collections.synchronizedMap(new HashMap<String, Hook>());

    public static void put(String classname, Hook hook) {
        hooks.put(classname, hook);
    }

    public static boolean override(String classname, String owner, String method) {
        return hooks.get(classname).override(owner, method);
    }

    public static void before(String classname, Object inst, String owner, String method, Object[] args, Object[] out) {
        hooks.get(classname).before(inst, owner, method, args, out);
    }

    public static void after(String classname, Object inst, String owner, String method, Object[] args) {
        hooks.get(classname).after(inst, owner, method, args);
    }

}
