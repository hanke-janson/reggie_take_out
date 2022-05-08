package com.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.reggie.common.BaseContext;
import com.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    /**
     * 路径匹配器 支持通配符
     */
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 获取本次请求的URI
        // /backend/index.html
        String requestURI = request.getRequestURI();
        log.info("拦截到请求:{}", requestURI);
        // 定义不需要处理的请求路径
        String[] urls = {
                "/employee/login",
                "/employee/logout",
                //window.top.location.href = '/backend/page/login/login.html' 因为这里需要跳转到登录页面，所以需要后端拦截器放行
                "/backend/**",
                "/front/**",
                "/common/**",
                // 移动端发送短信
                "/user/sendMsg",
                //移动端登录
                "/user/login"
        };
        // 判断本次请求是否需要处理
        boolean check = this.check(urls, requestURI);
        // 若不需要处理, 则直接放行
        if (check) {
            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        // 判断管理端登录状态, 若已登录, 放行
        Long empId = (Long) request.getSession().getAttribute("emp_id");
        if (empId != null) {
            log.info("用户已登录, 用户id为:{}, 放行", empId);
            // 将当前登录用户ID存入ThreadLocal
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request, response);
            return;
        }

        // 判断移动端登录状态, 若已登录, 放行
        Long userId = (Long) request.getSession().getAttribute("user");
        if (userId != null) {
            log.info("用户已登录, 用户id为:{}, 放行", userId);
            // 将当前登录用户ID存入ThreadLocal
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request, response);
            return;
        }

        //前端向后端发送请求员工数据页面的请求,后端拦截到请求:/employee/page, 发现未登录然后发送响应数据,前端判断响应数据后跳转到登录页面
        log.info("用户未登录");
        // 若未登录则返回未登录结果 通过输出流方式向客户端页面响应数据(相应的数据要和前端request.js中的响应拦截器判断的数据相同)
        response.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
    }

    /**
     * 路径匹配,检查本次请求路径是否需要处理
     *
     * @param urls       不需要处理的请求路径数组
     * @param requestURI 本次请求的URI
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
