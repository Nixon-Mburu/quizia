package com.example.quizia_backend.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.WriteListener;

@Component
public class QuestionsApiDebugFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (req.getRequestURI().startsWith("/api/questions")) {

            BufferedResponseWrapper wrapper = new BufferedResponseWrapper(res);
            chain.doFilter(request, wrapper);
            byte[] bodyBytes = wrapper.getBody();

            try (FileOutputStream fos = new FileOutputStream("/tmp/quizia_backend_questions.json", false)) {
                fos.write(bodyBytes);
            } catch (Exception ex) {

            }

            res.setContentType(wrapper.getContentType());
            res.setCharacterEncoding(wrapper.getCharacterEncoding());
            res.getOutputStream().write(bodyBytes);
            res.getOutputStream().flush();
        } else {
            chain.doFilter(request, response);
        }
    }


    private static class BufferedResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final ServletOutputStream outStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                buffer.write(b);
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setWriteListener(WriteListener writeListener) {

            }
        };
        private PrintWriter writer;
        private String contentType;
        private String characterEncoding = "UTF-8";

        public BufferedResponseWrapper(HttpServletResponse response) {
            super(response);
        }
        @Override
        public ServletOutputStream getOutputStream() {
            return outStream;
        }
        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
            }
            return writer;
        }
        @Override
        public void setContentType(String type) {
            this.contentType = type;
            super.setContentType(type);
        }
        @Override
        public void setCharacterEncoding(String charset) {
            this.characterEncoding = charset;
            super.setCharacterEncoding(charset);
        }
        public String getContentType() {
            return contentType != null ? contentType : super.getContentType();
        }
        public String getCharacterEncoding() {
            return characterEncoding != null ? characterEncoding : super.getCharacterEncoding();
        }
        public byte[] getBody() {
            if (writer != null) {
                writer.flush();
            }
            return buffer.toByteArray();
        }
    }
}