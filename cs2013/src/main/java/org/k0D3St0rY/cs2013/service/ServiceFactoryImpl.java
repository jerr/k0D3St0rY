package org.k0D3St0rY.cs2013.service;


public class ServiceFactoryImpl implements ServiceFactory {
    CSQuestionService questionService = new CSQuestionService(); 

    public AbstractCSService create(String path) {
        if ("/".equals(path))
            return questionService;

        return null;
    }

}
