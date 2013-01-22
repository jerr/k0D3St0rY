package org.k0D3St0rY.cs2013.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.netty.util.internal.StringUtil;

public class CSQuestionService extends AbstractCSService {

    private final static Map<String,String> questions = new HashMap<String, String>();
    private final static String error = "Sorry, I don't understand ...";

    public CSQuestionService() {
        questions.put("Quelle est ton adresse email", "jeremie.codestory@gmail.com");
        questions.put("Es tu heureux de participer(OUI/NON)", "OUI");
        questions.put("Es tu abonne a la mailing list(OUI/NON)", "OUI");
        questions.put("Es tu pret a recevoir une enonce au format markdown par http post(OUI/NON)", "OUI");
        questions.put("Est ce que tu reponds toujours oui(OUI/NON)", "NON");
        questions.put("As tu bien recu le premier enonce(OUI/NON)", "OUI");
    }

    @Override
    public CharSequence execute(Map<String, List<String>> params) {
        if (!params.isEmpty() && params.containsKey("q")) {
            String key = params.get("q").get(0);
            if(questions.containsKey(key))
                return questions.get(key);
            else {
                key = key.replace(',', '.');
                if(key.contains(" ")) {
                    String[] values = StringUtil.split(key, ' ');
                    return (new BigDecimal(values[0])).add(new BigDecimal(values[1])).toString();
                } if(key.contains("-")) {
                    String[] values = StringUtil.split(key, '-');
                    return (new BigDecimal(values[0])).min(new BigDecimal(values[1])).toString();
                } if(key.contains("*")) {
                    String[] values = StringUtil.split(key, '*');
                    return (new BigDecimal(values[0])).multiply(new BigDecimal(values[1])).toString();
                } if(key.contains("/")) {
                    String[] values = StringUtil.split(key, '/');
                    return (new BigDecimal(values[0])).divide(new BigDecimal(values[1])).toString();
                } return error;
            }
        } else {
            return error;
        }

    }
}
