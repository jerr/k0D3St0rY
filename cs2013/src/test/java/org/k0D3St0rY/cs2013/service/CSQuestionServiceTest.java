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

    
    @Test
    public void testQuestionCalcul1() throws IOException {
        assertEquals("2", get("/?q=1+1"));
    }
    
    @Test
    public void testQuestionCalcul2() throws IOException {
        assertEquals("6", get("/?q=(1+2)*2"));
    }
    
    @Test
    public void testQuestionCalcul3() throws IOException {
        assertEquals("1,5", get("/?q=(1+2)/2"));
    }
    
    @Test
    public void testQuestionCalcul4() throws IOException {
        assertEquals("110", get("/?q=(1+2+3+4+5+6+7+8+9+10)*2"));
    }
    
    @Test
    public void testQuestionCalcul5() throws IOException {
        assertEquals("272,5", get("/?q=((1+2)+3+4+(5+6+7)+(8+9+10)*3)/2*5"));
    }

    @Test
    public void testQuestionCalcul6() throws IOException {
        assertEquals("6", get("/?q=1,5*4"));
    }
    
    @Test
    public void testQuestionCalcul7() throws IOException {
        assertEquals("31878018903828899277492024491376690701584023926880", get("/?q=((1,1+2)+3,14+4+(5+6+7)+(8+9+10)*4267387833344334647677634)/2*553344300034334349999000"));
    }
    
    @Test
    public void testQuestionCalcul8() throws IOException {
        assertEquals("0", get("/?q=(-1)+(1)"));
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
