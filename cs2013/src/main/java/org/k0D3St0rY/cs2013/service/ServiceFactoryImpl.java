package org.k0D3St0rY.cs2013.service;

import java.util.HashMap;
import java.util.Map;

public class ServiceFactoryImpl implements ServiceFactory {
    private Map<String, AbstractCSService> services = new HashMap<String, AbstractCSService>();

    public ServiceFactoryImpl() {
     services.put("/", new CSQuestionService());
     services.put("/enonce/1", new CSUplaodService());
    }
    
    public AbstractCSService create(String path) {
        if(services.containsKey(path))
            return services.get(path);
        return null;
    }

}
