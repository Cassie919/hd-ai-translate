package com.hd.ai.common.interceptor;

/**
 * 用户上下文工具类，基于 ThreadLocal 存储当前请求的用户标识。
 * <p>
 * 在请求处理完成后必须调用 {@link #clear()} 避免内存泄漏。
 * 典型使用方式：
 * <pre>{@code
 *   String userId = UserContext.getUserId();
 *   userService.doSomething(userId);
 * }</pre>
 */
public class UserContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContext() {
        // 工具类，禁止实例化
    }

    /**
     * 设置当前请求的用户标识
     */
    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前请求的用户标识
     *
     * @return 用户标识，可能为 null（在非 HTTP 请求线程中调用时）
     */
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清理 ThreadLocal，必须在请求结束后调用以防止内存泄漏
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
