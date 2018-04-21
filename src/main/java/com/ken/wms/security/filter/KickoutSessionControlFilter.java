package com.ken.wms.security.filter;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;

/**
 * 实现并发登陆人数控制
 *
 * @author Ken
 * @since 2017/4/27.
 */
public class KickoutSessionControlFilter extends AccessControlFilter {

    /**
     * 用户踢出后跳转地址
     */
    private String kickOutUrl;

    /**
     * 是否踢出之后登陆的用户
     */
    private boolean kickOutAfter;

    /**
     * 同一账号最大登陆数目
     */
    private int maxSessionNum;

    private SessionManager sessionManager;
    private Cache<String, Deque<Serializable>> cache;

    public void setKickOutUrl(String kickOutUrl) {
        this.kickOutUrl = kickOutUrl;
    }

    public void setKickOutAfter(boolean kickOutAfter) {
        this.kickOutAfter = kickOutAfter;
    }

    public void setMaxSessionNum(int maxSessionNum) {
        this.maxSessionNum = maxSessionNum;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cache = cacheManager.getCache("sessionCache");
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception {
        return false;
    }

    /**
     * 表示访问拒绝时是否自己处理，如果返回true表示自己不处理且继续拦截器链执行，
     * 返回false表示自己已经处理了（比如重定向到另一个页面）。
     * 根据 isAccessAllowed 方法的返回值
     *
     * @param servletRequest  request
     * @param servletResponse response
     * @return 返回是否已经处理访问拒绝
     * @throws Exception exception
     */
    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        // 如果用户还没有登陆则继续后续的流程
        Subject subject = getSubject(servletRequest, servletResponse);
        if (!subject.isAuthenticated() && !subject.isRemembered())
            return true;

        // 判断当前用户登陆数量是否超出
        Session session = subject.getSession();
        String userName = (String) subject.getPrincipal();
        Serializable sessionId = session.getId();

        // 初始化用户的登陆队列，将用户的队列放入到缓存中
        Deque<Serializable> deque = cache.get(userName);
        if (deque == null) {
            deque = new LinkedList<>();
            cache.put(userName, deque);
        }

        // 如果队列中没有此用户的 sessionId 且用户没有被踢出，则放入队列
        if (!deque.contains(sessionId) && session.getAttribute("kickOut") == null) {
            deque.push(sessionId);
        }

        // 若队列中的 sessionId 是否超出最大会话数目， 则踢出用户
        while (deque.size() > maxSessionNum) {
            Serializable kickOutSessionId;
            if (kickOutAfter) {
                kickOutSessionId = deque.removeFirst();
            } else {
                kickOutSessionId = deque.removeLast();
            }

            // 设置 sessionId 对应的 session 中的字段，表示该用户已经被踢出
            try {
                Session kickOutSession = sessionManager.getSession(new DefaultSessionKey(kickOutSessionId));
                if (kickOutSession != null) {
                    kickOutSession.setAttribute("kickOut", true);
                }
            } catch (Exception e) {
                // do logging
                e.printStackTrace();
            }
        }

        // 如果当前登陆用户被踢出，则退出并跳转
        if (session.getAttribute("kickOut") != null && Boolean.TRUE.equals(session.getAttribute("kickOut"))) {
            try {
                // 登出
                subject.logout();

                // 根据请求类型作出处理
                HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
                HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                if (!"XMLHttpRequest".equalsIgnoreCase(httpServletRequest.getHeader("X-Requested-with"))) {
                    // 普通请求
                    WebUtils.issueRedirect(httpServletRequest, httpServletResponse, kickOutUrl);
                } else {
                    // ajax 请求
                    httpServletResponse.setStatus(430);
                }

            } catch (Exception e) {
                // do nothing
            }
            return false;
        }

        return true;
    }
}
