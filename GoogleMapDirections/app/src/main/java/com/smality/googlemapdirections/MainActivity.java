package com.smality.googlemapdirections;

import androidx.fragment.app.FragmentActivity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.*;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng mOrigin;
    private LatLng mDestination;
    private Polyline mPolyline;
    ArrayList<LatLng> mMarkerPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SupportMapFragment ile haritayı yükleyeceğimiz arayüz elemanını tanımlıyoruz
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        mMarkerPoints = new ArrayList<>();
    }
    //Harita üzerinde yolun başlangıç ve bitiş noktasını imleçler ile işaretleyerek seçme
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // Her lokasyon yolunu çizme işleminden sonra yeni yol çizimi için harita refresh yapılıyor
                if(mMarkerPoints.size()>1){
                    mMarkerPoints.clear();
                    mMap.clear();
                }
                mMarkerPoints.add(point);

                //İmleçleri oluşturan sınıfı tanımladık
                MarkerOptions options = new MarkerOptions();
                //İmleci, haritada belirtilen noktalara eklenmesi için ilgili yerin değerini atadık
                options.position(point);
                //DirectionsJSONParser sınıfında, yol süresi değerini SharedPreferences ile taşıyıp
                // imleçde gösterdik
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                options.title("Duration: " +preferences.getString("duration", null));

                /**
                 * İmleçlerin görsellerini belirleme, Başlangıç imlecini yeşil
                 * lokasyonu bitişini belirten imleç ise kırmızı olarak belirledim
                 */
                if(mMarkerPoints.size()==1){
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }else if(mMarkerPoints.size()==2){
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                // Haritada marker(imleç) ekleniyor
                mMap.addMarker(options);

                // Google Directions web servis urlsini hazırlayan ve web servisi kullanan metodu çağırdık
                if(mMarkerPoints.size() >= 2){
                    mOrigin = mMarkerPoints.get(0);
                    mDestination = mMarkerPoints.get(1);
                    drawRoute();
                }

            }
        });
    }
    // Google Directions API den konum datalarını kullanabilmek için web servis ile
    //bağlantı kuracağımız url hazırlayan metodu çağırdık ve web servisi çalıştırdık
    private void drawRoute(){
        String url = getDirectionsUrl(mOrigin, mDestination);

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    //Haritada belirttiğiniz başlangeç ve bitiş imleçlerinin koordinatlarını API Key ile kullanarak
    //çizilecek düzergah ile ilgili bilgileri elde etmek adına url olusturan metod
    private String getDirectionsUrl(LatLng origin,LatLng dest){

        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // API Key parametre olarak hazırlama
        String key = "key=" +"AIzaSyBconZdqpSflHNcs6t-btfk0Rlo1Lex7wo";

        String parameters = str_origin+"&"+str_dest+"&"+key;
        String output = "json";

        // Oluşturduğumuz parametreleri kullanarak url yi oluşturuyoruz
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            // Hazırladığımız url yi kullanarak web servise Http bağlantısı ile sağlıyoruz
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            // webservisden gelen json datayı okuyup, data değişkenine atadık
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }
            //json veriyi data değişkenine atadıkve metodda return yaptık
            data = sb.toString();
            br.close();

        }catch(Exception e){
            //Log.d("Exception on download", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /** Bu sınıf da, Google Directions URL kullanarak, rotanın kaç km. olduğu, ne kadar sürede gidilebildiği
     * gibi bilgileri barındıran json datayı çektik */
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try{
                data = downloadUrl(url[0]);

            }catch(Exception e){
                //Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Google Directions bilgisini, JSON formatını parse ederek almayı sağlayan sınıf
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    /** Bu sınıf Google Directions bilgisini, JSON formatını parse ederek almayı sağlar  */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                //DirectionsJSONParser sınıfında tüm json çözümlenir.
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject,MainActivity.this);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Json çözümlendikten sonra, harita üzerinde rotayı(path) çizmeyi sağlayan metod
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                //PolylineOptions ile rota üzerinde noktalar ekleyerek, çizginin rengini ve genişliğini belirleme
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.RED);
            }

            if(lineOptions != null) {
                if(mPolyline != null){
                    mPolyline.remove();
                }
                mPolyline = mMap.addPolyline(lineOptions);

            }else
                Toast.makeText(getApplicationContext(),"Rota bulanamadı", Toast.LENGTH_LONG).show();
        }
    }

}
