package com.ingot.framework.core.context;

import com.ingot.framework.core.constants.ContextConstants;
import com.ingot.framework.core.wrapper.ThreadLocalMap;

/**
 * <p>Description  : Context.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 6:25 下午.</p>
 */
public class ContextHolder {

    public static String token() {
        return ThreadLocalMap.get(ContextConstants.KEY_TOKEN);
    }

    public static void setToken(String token){
        ThreadLocalMap.put(ContextConstants.KEY_TOKEN, token);
    }

    public static void removeUserAuth(){
        ThreadLocalMap.remove(ContextConstants.KEY_TOKEN);
    }

    public static void clear() {
        ThreadLocalMap.clear();
    }

}
