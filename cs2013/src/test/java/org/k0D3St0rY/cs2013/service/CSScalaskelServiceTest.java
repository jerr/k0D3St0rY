package org.k0D3St0rY.cs2013.service;


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.k0D3St0rY.cs2013.server.CSApplication;

public class CSScalaskelServiceTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CSApplication.main(new String[] { "-blocalhost", "-p8282" });        
    }

    
    @Test
    public void test1() throws IOException {
        assertEquals("[{\"foo\": 1}]", get("/scalaskel/change/1"));
    }

    @Test
    public void test7() throws IOException {
        assertEquals("[{\"foo\": 7},{\"bar\": 1}]]", get("/scalaskel/change/7"));
    }

    @Test
    public void test28() throws IOException {
        assertEquals("[{\"foo\": 28},{\"foo\": 21,\"bar\": 1},{\"foo\": 14,\"bar\": 2},{\"foo\": 7,\"bar\": 3},{\"bar\": 4},{\"foo\": 17,\"qix\": 1},{\"foo\": 10,\"bar\": 1,\"qix\": 1},{\"foo\": 3,\"bar\": 2,\"qix\": 1},{\"foo\": 6,\"qix\": 2},{\"foo\": 7,\"baz\": 1},{\"bar\": 1,\"baz\": 1}]", get("/scalaskel/change/28"));
    }

    private String get(String uri) throws IOException {
        return get(uri, 200);
    }

    private String get(String uri, int expectedStatusCode) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod("http://localhost:8282" + uri);
        long start = System.currentTimeMillis();
        assertEquals(expectedStatusCode, client.executeMethod(method));
        long time = System.currentTimeMillis() - start;
        System.out.println(" ## " + method.getPath() + " " + time + "ms");
        return new String(method.getResponseBody());
    }
}
