package org.k0D3St0rY.cs2013.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSQuestionService extends AbstractCSService {

    private final static Map<String, String> questions = new HashMap<String, String>();
    private final static String error = "Sorry, I don't understand ...";

    public CSQuestionService() {
        questions.put("Quelle est ton adresse email", "jeremie.codestory@gmail.com");
        questions.put("Es tu heureux de participer(OUI/NON)", "OUI");
        questions.put("Es tu abonne a la mailing list(OUI/NON)", "OUI");
        questions.put("Es tu pret a recevoir une enonce au format markdown par http post(OUI/NON)", "OUI");
        questions.put("Est ce que tu reponds toujours oui(OUI/NON)", "NON");
        questions.put("As tu bien recu le premier enonce(OUI/NON)", "OUI");
        questions.put("As tu passe une bonne nuit malgre les bugs de l etape precedente(PAS_TOP/BOF/QUELS_BUGS)","QUELS_BUGS");
        questions.put("As tu bien recu le second enonce(OUI/NON)","OUI");
        questions.put("As tu copie le code de ndeloof(OUI/NON/JE_SUIS_NICOLAS)","NON");
    }

    @Override
    public CharSequence execute(Map<String, List<String>> params) {
        if (!params.isEmpty() && params.containsKey("q")) {
            String key = params.get("q").get(0);
            if (questions.containsKey(key))
                return questions.get(key);
            else {
                String result = calcul(key.replace(',', '.'));
                // pas glop!
                result = result.replace('.', ',');
                questions.put(key, result);
                return result;
            }
        } else {
            return error;
        }

    }

    private String calcul(String eq) {
        while (eq.contains("(")) {
            int end = eq.indexOf(')');
            int start = eq.substring(0, end).lastIndexOf('(');
            String result = calcul(eq.substring(start + 1, end));
            eq = eq.substring(0, start) + result + eq.substring(end + 1);
        }
        while (eq.matches(".*[*/].*")) {
            Matcher m = Pattern.compile("^(.*[^/\\*\\.0-9])*([-\\.0-9]+)(/|\\*)([-\\.0-9]+).*").matcher(eq);
            m.matches();
            String value1 = m.group(2);
            String value2 = m.group(4);
            if (m.group(3).equals("*"))
                eq = eq.replace(value1 + "*" + value2, (new BigDecimal(value1).multiply(new BigDecimal(value2)).toString()));
            else
                eq = eq.replace(value1 + "/" + value2, (new BigDecimal(value1).divide(new BigDecimal(value2)).toString()));
        }
        while (eq.matches(".*\\d[ -]\\d*.*")) {
            Matcher m = Pattern.compile("^([^-/\\*\\.0-9])*((-|[0-9])[\\.0-9]*)( |-)([\\.0-9]+).*").matcher(eq);
            m.matches();
            String value1 = m.group(2);
            String value2 = m.group(5);
            if (m.group(4).equals(" "))
                eq = eq.replace(value1 + " " + value2, (new BigDecimal(value1).add(new BigDecimal(value2)).toString()));
            else
                eq = eq.replace(value1 + "-" + value2, (new BigDecimal(value1).subtract(new BigDecimal(value2)).toString()));
        }
        if (eq.contains(".")) {
            while (eq.endsWith("0")) {
                eq = eq.substring(0, eq.length() - 1);
            }
        }
        if (eq.endsWith("."))
            eq = eq.substring(0, eq.length() - 1);
        return eq;
    }
}
