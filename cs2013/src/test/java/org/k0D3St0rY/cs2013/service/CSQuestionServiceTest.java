package org.k0D3St0rY.cs2013.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.k0D3St0rY.cs2013.server.CSApplication;

public class CSQuestionServiceTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CSApplication.main(new String[] { "-blocalhost", "-p8080" });        
    }

    
    @Test
    public void testQuestion1() throws IOException {
        assertEquals("jeremie.codestory@gmail.com", get("/?q=Quelle+est+ton+adresse+email"));
    }

    private String get(String uri) throws IOException {
        return get(uri, 200);
    }

    private String get(String uri, int expectedStatusCode) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod("http://localhost:8080" + uri);

        assertEquals(expectedStatusCode, client.executeMethod(method));
        return new String(method.getResponseBody());
    }

}
