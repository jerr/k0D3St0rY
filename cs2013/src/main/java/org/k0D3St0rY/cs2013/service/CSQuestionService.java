package org.k0D3St0rY.cs2013.service;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CSQuestionService extends AbstractCSService {

    // I like simple code!
    private final static Properties questions = new Properties();
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
            return questions.getProperty(params.get("q").get(0), error);
        } else {
            return error;
        }

    }

}
