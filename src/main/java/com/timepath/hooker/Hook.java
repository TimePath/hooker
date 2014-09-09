package com.timepath.hooker;

/**
 * @author TimePath
 */
public interface Hook {

    void before(Object inst, String owner, String method, Object[] args);

    void after(Object inst, String owner, String method, Object[] args);

}
