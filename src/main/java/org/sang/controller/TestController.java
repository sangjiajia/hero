package org.sang.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/asyncServlet")
public class TestController {
	   @RequestMapping(value="/testAsyn",method=RequestMethod.GET)
	    public void testAsny(HttpServletRequest request, HttpServletResponse response) throws IOException{
	        System.out.println("start");
	        PrintWriter out = response.getWriter();
	        AsyncContext asyncContext = request.startAsync();
	        asyncContext.setTimeout(5000);
	        asyncContext.addListener(new MyAsyncListener());
	        new Thread(new Work(asyncContext,request)).start();
	        out.print("异步执行中");
	    }
}
class Work implements Runnable{

    private AsyncContext asyncContext;
    private HttpServletRequest request;

    public Work(AsyncContext asyncContext,HttpServletRequest request) {
        this.asyncContext = asyncContext;
        this.request = request;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(4000);
            //此处通过观察源码，得知需要从request来判断是否超时，否则会一直抛出异常,超时的话，超时参数会置为-1
            if(request.getAsyncContext() != null && asyncContext.getTimeout()>0){
                try {
                    ServletResponse response = asyncContext.getResponse();
                    PrintWriter out = response.getWriter();
                    out.println("后台线程执行完成");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                asyncContext.complete();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
class MyAsyncListener implements AsyncListener{

    @Override
    public void onComplete(AsyncEvent asyncEvent) throws IOException {
        try {
            AsyncContext asyncContext = asyncEvent.getAsyncContext();
            ServletResponse response = asyncContext.getResponse();
            ServletRequest request = asyncContext.getRequest();
            PrintWriter out= response.getWriter();
            if (request.getAttribute("timeout") != null && 
                    StringUtils.endsWithIgnoreCase("true",request.getAttribute("timeout").toString())) {//超时
                out.println("后台线程执行超时---【回调】");
                System.out.println("异步servlet【onComplete超时】");
            }else {//未超时
                out.println("后台线程执行完成---【回调】");
                System.out.println("异步servlet【onComplete完成】");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(AsyncEvent asyncEvent) throws IOException {
        System.out.println("异步servlet错误");
    }

    @Override
    public void onStartAsync(AsyncEvent arg0) throws IOException {
        System.out.println("开始异步servlet");
    }

    @Override
    public void onTimeout(AsyncEvent asyncEvent) throws IOException {
        ServletRequest request = asyncEvent.getAsyncContext().getRequest();
        request.setAttribute("timeout", "true");
        System.out.println("异步servlet【onTimeout超时】");
    }

} 