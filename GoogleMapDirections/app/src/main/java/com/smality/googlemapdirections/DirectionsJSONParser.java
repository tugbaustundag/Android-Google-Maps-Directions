package com.smality.googlemapdirections;

import android.content.*;
import android.preference.PreferenceManager;
import com.google.android.gms.maps.model.LatLng;
import org.json.*;
import java.util.*;
public class DirectionsJSONParser {
    /** Google Directions URL kullanarak web servisden elde edilen JSONObject çözümlenerek,
     rotayı ne kadar sürede bitirebileceğiniz, rotanın kaç km olduğu, yol tarifleri hakkında detaylı bilgiler elde edilir   */
    public List<List<HashMap<String,String>>> parse(JSONObject jObject, Context context){

        List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;

        try {
            jRoutes = jObject.getJSONArray("routes");

            for(int i=0;i<jRoutes.length();i++){
                jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");

                JSONObject c = jLegs.getJSONObject(i);
                JSONObject vo = c.getJSONObject("duration");
                //Rotaya varış süresini, SharedPreferences nesnesine atadım. Haritada gösterimi, MainActivity sınıfında.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("duration", vo.getString("text"));
                editor.commit();

                List path = new ArrayList<HashMap<String, String>>();
                for(int j=0;j<jLegs.length();j++){
                    jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");
                    for(int k=0;k<jSteps.length();k++){
                     //Polyline çözümleyerek, rota noktalarının enlem, boylam bilgisi alma
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        for(int l=0;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                            hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
        }
        return routes;
    }

    /**
     * Polyline (rota yolunun şifrelenmiş çizgileri) çözümleyerek, rota noktalarının enlem, boylam bilgisini döner
     */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}
