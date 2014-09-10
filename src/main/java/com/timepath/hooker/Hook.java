package com.timepath.hooker;

/**
 * @author TimePath
 */
public interface Hook {

    /**
     * Called once per class
     *
     * @param owner
     * @param method
     * @return true if the method body should be discarded
     */
    boolean override(String owner, String method);

    /**
     * @param inst
     * @param owner
     * @param method
     * @param args   the method arguments, can be modified
     * @param out    Object[1] if overridden, else null
     */
    void before(Object inst, String owner, String method, Object[] args, Object[] out);

    void after(Object inst, String owner, String method, Object[] out);

}
