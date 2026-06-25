package com.paymentengine.config;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;

public class CachingResponseWrapper extends HttpServletResponseWrapper {
    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response The response to be wrapped
     * @throws IllegalArgumentException if the response is null
     */

    private final ByteArrayOutputStream buffer =  new ByteArrayOutputStream();
    private final PrintWriter writer;



    public CachingResponseWrapper(HttpServletResponse response, PrintWriter writer) throws IOException {
        super(response);
        this.writer = new PrintWriter(
                new OutputStreamWriter(buffer, response.getCharacterEncoding()), true
        );
    }

    @Override
    public PrintWriter getWriter(){
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream(){
        return new ServletOutputStream() {
            @Override
            public void write(int b) {
                buffer.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }
            @Override public void setWriteListener(WriteListener l) {

            }

        };
    }

    public byte[] getResponseBody(){
        writer.flush();
        return buffer.toByteArray();
    }
    public void copyBodyToResponse() throws IOException{
        byte[] body = getResponseBody();
        getResponse().getOutputStream().write(body);
        getResponse().getOutputStream().flush();
    }
}
