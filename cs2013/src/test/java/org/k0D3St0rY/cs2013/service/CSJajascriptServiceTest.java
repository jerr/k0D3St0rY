package org.k0D3St0rY.cs2013.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.k0D3St0rY.cs2013.server.CSApplication;
import org.k0D3St0rY.cs2013.service.CSJajascriptService.Vol;

public class CSJajascriptServiceTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CSApplication.main(new String[] { "-blocalhost", "-p8181" });
    }

    @Test
    public void testSmall() throws IOException {
        List<Vol> vols = new ArrayList<Vol>();
        vols.add(new Vol("MONAD42", 0, 5, 10));
        vols.add(new Vol("META18", 3, 7, 14));
        vols.add(new Vol("LEGACY01", 5, 9, 8));
        vols.add(new Vol("YAGNI17", 5, 9, 7));
        assertEquals("", post(vols, 201));
    }

    @Test
    public void testBig() throws IOException {
        List<Vol> vols = new ArrayList<Vol>();
        for (int i = 0; i < 10000; i++) {
            int start = RandomUtils.nextInt(50);
            int durre = 1 + RandomUtils.nextInt(50 - start);
            vols.add(new Vol("vol" + i, start, durre, RandomUtils.nextInt(30)));
        }
        assertEquals("", post(vols, 201));
    }

    private String post(List<Vol> vols, int expectedStatusCode) throws IOException {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("http://localhost:8181/jajascript/optimize");

        StringBuilder body = new StringBuilder();
        body.append("[ ");
        Vol last = vols.remove(vols.size() - 1);
        for (Vol vol : vols) {
            body.append("\t").append(vol.toJSON()).append(", \n");
        }
        body.append("\t").append(last.toJSON()).append("\n").append(" ]\n");
        method.setRequestEntity(new StringRequestEntity(body.toString(), "application/json", "UTF-8"));

        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

        int status = client.executeMethod(method);

        if (status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK) {
            System.out.println("Upload complete, response=" + method.getResponseBodyAsString());
        } else {
            System.out.println("Upload failed, response=" + HttpStatus.getStatusText(status));
        }
        assertEquals(expectedStatusCode, status);
        return new String(method.getResponseBody());
    }

}
