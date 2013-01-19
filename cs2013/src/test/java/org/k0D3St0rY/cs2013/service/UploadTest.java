package org.k0D3St0rY.cs2013.service;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.junit.BeforeClass;
import org.junit.Test;
import org.k0D3St0rY.cs2013.server.CSApplication;

public class UploadTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CSApplication.main(new String[] { "-blocalhost", "-p8181" });        
    }

    
    @Test
    public void testUpload() throws IOException {
        assertEquals("", post("/enonce/1", 201));
    }

    private String post(String uri, int expectedStatusCode) throws IOException {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("http://localhost:8181" + uri);

        File f = File.createTempFile("enonce.md", null);
        f.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write("Upload d'un fichier");
        out.close();

        Part[] parts = { new FilePart(f.getName(), f) };

        method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

        int status = client.executeMethod(method);

        if (status == HttpStatus.SC_OK) {
            System.out.println("Upload complete, response=" + method.getResponseBodyAsString());
        } else {
            System.out.println("Upload failed, response=" + HttpStatus.getStatusText(status));
        }
        assertEquals(expectedStatusCode, client.executeMethod(method));
        return new String(method.getResponseBody());
    }

}
