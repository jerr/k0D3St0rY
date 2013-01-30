package org.k0D3St0rY.cs2013.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;

import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

import com.google.common.collect.ArrayListMultimap;

public class CSJajascriptService extends AbstractCSService {

    static final InternalLogger logger = InternalLoggerFactory.getInstance(CSJajascriptService.class);

    @Override
    public CharSequence execute(Map<String, List<String>> params) {
        ArrayListMultimap<Integer, Vol> volStartMap = ArrayListMultimap.create();
        //ArrayListMultimap<Integer, Vol> volEndMap = ArrayListMultimap.create();

        int maxStart = 0;
        StringBuilder result = new StringBuilder();
        if (!params.isEmpty() && params.containsKey("content")) {
            for (String element : params.get("content")) {
                try {
                    JSONArray jsonArray = JSONArray.fromObject(element);
                    List<Vol> vols = new ArrayList<Vol>();
                    for (Object json : jsonArray) {
                        Vol vol = new Vol();
                        vol.setVol(((JSONObject) json).getString("VOL"));
                        vol.setDepart(((JSONObject) json).getInt("DEPART"));
                        vol.setDurre(((JSONObject) json).getInt("DUREE"));
                        vol.setPrix(((JSONObject) json).getInt("PRIX"));
                        vols.add(vol);
                        volStartMap.put(vol.getDepart(), vol);
              //          volEndMap.put(vol.depart + vol.durre, vol);
                        logger.info(vol.toJSON());
                        if(vol.getDepart()>maxStart)
                            maxStart = vol.getDepart();
                    }
                }catch (Exception e) {
                   logger.error(" size : " + volStartMap.size(), e);
                }
            }

            Vol startVol = null;
            int max = 0;
            for (int i = 0; i < maxStart; i++) {
                for (Vol vol : volStartMap.get(i)) {
                    int prix = prixMax(vol, volStartMap,maxStart);
                    if (prix > max) {
                        max = prix;
                        startVol = vol;
                    }
                }
            }
            result.append("{ \"gain\" : " + max + ", \"path\" : [");
            while (startVol != null) {
                result.append("\"" + startVol.vol + "\"");
                if (startVol.nextVol != null)
                    result.append(", ");
                startVol = startVol.nextVol;
            }
            result.append("] }");
        }

        logger.info(result.toString());
        return result.toString();
    }

    private int prixMax(Vol vol, ArrayListMultimap<Integer, Vol> volStartMap, int maxStart) {
        if (vol.gain == -1) {
            int max = 0;
            for (int i = (vol.depart + vol.durre); i <= maxStart; i++) {
                for (Vol nextVol : volStartMap.get(i)) {
                    int prix = prixMax(nextVol, volStartMap, maxStart);
                    if (prix > max) {
                        max = prix;
                        vol.nextVol = nextVol;
                    }
                }

            }
            vol.gain = max + vol.prix;
        }
        return vol.gain;
    }

    public static final class Vol {

        private Vol nextVol;
        private int gain = -1;

        private String vol;
        private int depart;
        private int durre;
        private int prix;

        public Vol(String vol, int depart, int durre, int prix) {
            this.vol = vol;
            this.depart = depart;
            this.durre = durre;
            this.prix = prix;
        }

        public Vol() {
            // TODO Auto-generated constructor stub
        }

        public String toJSON() {
            return "{ \"VOL\": \"" + vol + "\", \"DEPART\": " + depart + ", \"DUREE\": " + durre + ", \"PRIX\": " + prix + " }";
        }

        public String getVol() {
            return vol;
        }

        public void setVol(String vol) {
            this.vol = vol;
        }

        public int getDepart() {
            return depart;
        }

        public void setDepart(int depart) {
            this.depart = depart;
        }

        public int getDurre() {
            return durre;
        }

        public void setDurre(int durre) {
            this.durre = durre;
        }

        public int getPrix() {
            return prix;
        }

        public void setPrix(int prix) {
            this.prix = prix;
        }

    }
}
