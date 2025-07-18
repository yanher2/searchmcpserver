package com.searchserver.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.stream.Collectors;

@Component
@Order(1)
public class RequestLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        logger.info("Request received - Method: {}, Path: {}", method, path);

        Enumeration<String> headers = httpRequest.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = headers.nextElement();
            logger.info("Header - {}: {}", headerName, httpRequest.getHeader(headerName));
        }

        Enumeration<String> params = httpRequest.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();
            logger.info("Parameter - {}: {}", paramName, httpRequest.getParameter(paramName));
        }

        if (httpRequest.getContentLength() > 0) {
            try {
               // String body = httpRequest.getReader().lines().collect(Collectors.joining());
               String body = "";
                logger.info("Request Body: {}", body);
            } catch (Exception e) {
                logger.error("Error reading request body", e);
            }
        }
        chain.doFilter(request, response);
        /*// 检查是否为SSE响应
        if (response.getContentType() != null && response.getContentType().contains("text/event-stream")) {
            // 对于SSE响应，不包装和记录响应体
            chain.doFilter(request, response);
            logger.info("SSE response detected, skipping response body logging");
        } else {
            // 对于普通响应，包装并记录响应内容
            CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) response);
            chain.doFilter(request, responseWrapper);

            String responseBody = responseWrapper.toString();
            logger.info("Response Data: {}", responseBody);

            // 将内容写回原始响应
            response.getWriter().write(responseBody);
        }*/
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RequestLoggingFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("RequestLoggingFilter destroyed");
    }

    // 新增响应包装类
    private static class CharResponseWrapper extends HttpServletResponseWrapper {
        private CharArrayWriter charWriter = new CharArrayWriter();
        private PrintWriter writer = new PrintWriter(charWriter);

        public CharResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public String toString() {
            writer.flush();
            return charWriter.toString();
        }
    }
}
