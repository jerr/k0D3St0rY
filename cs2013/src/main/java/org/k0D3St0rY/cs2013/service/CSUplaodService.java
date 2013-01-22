package org.k0D3St0rY.cs2013.service;

import java.util.List;
import java.util.Map;

import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.k0D3St0rY.cs2013.server.HttpCSServer;

public class CSUplaodService extends AbstractCSService {

    static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpCSServer.class);

    @Override
    public CharSequence execute(Map<String, List<String>> params) {
        if (!params.isEmpty() && params.containsKey("content")) {
            for (String element : params.get("content")) {
                logger.info(element);
            }
        }
        return "";
    }

}
