package com.jc_lab.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1000;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1001;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_INTERNET = 1002;

    DrawerLayout drawerLayout;

    TextView textViewTemp;
    TextView textViewMaxTemp;
    TextView textViewMinTemp;
    TextView textViewFeelsLikeTemp;
    TextView textViewHumidity;
    TextView textViewWind;
    TextView textViewCity;
    TextView textViewDescription;

    ImageView gpsImageView;

    ImageButton addCityButton;

    public List<String> storedCityList;
    public List<String> changedCityList;

    private double currentLat;
    private double currentLng;
    private String currentCity;
    private String searchedCity;
    private RequestQueue volleyQueue;

    MutableLiveData<Boolean> isGPS;
    public static MutableLiveData<String> addedCity;
    public static String selectedCity;

    InputWithAlertDialog inputWithAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

        drawerLayout = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getTitle().toString().equals(getString(R.string.current_location)))
                {
                    isGPS.setValue(true);
                    getCurrentLocation();
                    getWeatherInfo(currentCity);
                    textViewCity.setText(currentCity);
                }
                else
                {
                    isGPS.setValue(false);
                    getCoordFromCity(item.getTitle().toString());
                    getWeatherInfo(searchedCity);
                    textViewCity.setText(searchedCity);
                    selectedCity = item.getTitle().toString();
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });

        storedCityList = new LinkedList<>();
        changedCityList = new LinkedList<>();
        SharedPreferences pref = getSharedPreferences("cityWeather", MODE_PRIVATE);
        try{
            String a = pref.getString("citysJSONArray","");
            if(!a.equals(""))
            {
                JSONArray jsonArray = new JSONArray(a);
                if(jsonArray != null)
                {
                    for(int i=0; i<jsonArray.length(); i++)
                    {
                        String b = jsonArray.getString(i);
                        if(b.equals("")) continue;
                        storedCityList.add(b);
                        changedCityList.add(b);
                    }
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        if(storedCityList != null)
        {
            final Menu menu = navigationView.getMenu();
            menu.clear();
            Iterator iter = storedCityList.iterator();
            menu.add(getString(R.string.current_location));

            while(iter.hasNext())
            {
                menu.add((String)iter.next());
            }
        }

        inputWithAlertDialog = new InputWithAlertDialog(this);

        textViewTemp = findViewById(R.id.temp_text_view);
        textViewMaxTemp = findViewById(R.id.max_temp_text_view);
        textViewMinTemp = findViewById(R.id.min_temp_text_view);
        textViewFeelsLikeTemp = findViewById(R.id.temp_feels_like_text_view);
        textViewHumidity = findViewById(R.id.humidity_text_view);
        textViewWind = findViewById(R.id.wind_text_view);
        textViewCity = findViewById(R.id.city_name);
        textViewDescription = findViewById(R.id.weather_description);
        gpsImageView = findViewById(R.id.gps_image);
        isGPS = new MutableLiveData<>();
        isGPS.setValue(true);
        isGPS.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(!aBoolean) gpsImageView.setVisibility(View.GONE);
                else gpsImageView.setVisibility(View.VISIBLE);
            }
        });
        addedCity = new MutableLiveData<>();

        addedCity.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                final Menu menu = navigationView.getMenu();
                if(changedCityList.contains(addedCity.getValue())) return;
                menu.clear();
                changedCityList.add(addedCity.getValue());
                menu.add(getString(R.string.current_location));
                Iterator iterator = changedCityList.iterator();
                while(iterator.hasNext())
                {
                    menu.add((String)iterator.next());
                }
            }
        });

        addCityButton = findViewById(R.id.add_city_button);
        addCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputWithAlertDialog.apply();
            }
        });

        volleyQueue = Volley.newRequestQueue(this);

        requestPermission();
        getCurrentLocation();
        getWeatherInfo(currentCity);
        textViewCity.setText(currentCity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.toolbar, menu);
        return true;
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }
            case R.id.toolbar_delete_city: { // 오른쪽 상단 버튼 눌렀을 때
//                Toast.makeText(this, "click", Toast.LENGTH_SHORT).show();
                if(changedCityList.contains(selectedCity))
                {
                    changedCityList.remove(selectedCity);
                    final Menu menu = ((NavigationView)findViewById(R.id.nav_view)).getMenu();
                    menu.clear();
                    menu.add(getString(R.string.current_location));
                    Iterator iterator = changedCityList.iterator();
                    while(iterator.hasNext())
                    {
                        menu.add((String)iterator.next());
                    }
                    isGPS.setValue(true);
                    getCurrentLocation();
                    getWeatherInfo(currentCity);
                    textViewCity.setText(currentCity);
                }

            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!changedCityList.equals(storedCityList))
        {
            SharedPreferences pref = getSharedPreferences("cityWeather", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            JSONArray jsonArray = new JSONArray();
            Iterator iter = changedCityList.iterator();
            while(iter.hasNext())
            {
                jsonArray.put((String)iter.next());
            }

            editor.putString("citysJSONArray", jsonArray.toString());
            editor.commit();
        }
    }

    public void requestPermission()
    {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
    }
    public void getWeatherInfo(String city)
    {
        String api = getString(R.string.openweather_api);
        Log.d("currentCity", city);
//        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f&lang=kr&units=metric&appid=%s",currentLat,currentLng,api);
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&lang=kr&units=metric&appid=%s",city,api);
        Log.d("url", url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try
                        {
                            JSONObject responseInJSONObject = new JSONObject(response);
                            Log.d("response",responseInJSONObject.toString());
                            JSONObject weather = responseInJSONObject.getJSONArray("weather").getJSONObject(0);
                            String mainWeather = weather.getString("main");
                            String descriptionWeather = weather.getString("description");
                            String icon = weather.getString("icon");
                            JSONObject main = responseInJSONObject.getJSONObject("main");
                            double temp = main.getDouble("temp");
                            double feelsLike = main.getDouble("feels_like");
                            String minTemp = main.getString("temp_min");
                            String maxTemp = main.getString("temp_max");
                            int pressure = main.getInt("pressure");
                            String humidity = main.getString("humidity");
                            int visibility = responseInJSONObject.getInt("visibility");
                            JSONObject wind = responseInJSONObject.getJSONObject("wind");
                            double windSpeed = wind.getDouble("speed");
                            int windDirection = wind.getInt("deg");
                            String name = responseInJSONObject.getString("name");
                            JSONObject time = responseInJSONObject.getJSONObject("sys");
                            int sunrise = time.getInt("sunrise");
                            int sunset = time.getInt("sunset");
                            setWeatherImage(icon);
                            textViewTemp.setText((String.format("%.1f",temp)) +'\u2103');
                            textViewMaxTemp.setText((maxTemp) +'\u2103');
                            textViewMinTemp.setText((minTemp) +'\u2103');
                            textViewFeelsLikeTemp.setText(String.format("%.1f",feelsLike)+'\u2103');
                            textViewHumidity.setText(humidity+'%');
                            textViewWind.setText((String.format("%.1f",windSpeed)));
                            textViewDescription.setText(descriptionWeather);

                        }
                        catch(JSONException e)
                        {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error", "That didn't work!");
            }
        });
        volleyQueue.add(stringRequest);
    }

    public Coordinate getCoordFromCity(String city)
    {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = new ArrayList<>();
        try{
            addresses = geocoder.getFromLocationName(city, 3);
        }catch(IOException ioException){
            Toast.makeText(this,"Geocoder Service Not Available", Toast.LENGTH_SHORT).show();
            return null;
        }catch(IllegalArgumentException illegalArgumentException){
            Toast.makeText(this,"Incorrect GPS Coordination", Toast.LENGTH_SHORT).show();
            return null;
        }
        if(addresses == null || addresses.size()==0)
        {
            Toast.makeText(this,"Coordinate Not Found", Toast.LENGTH_SHORT).show();
        }
        Address ad = addresses.get(0);
        searchedCity = ad.getAdminArea();
        Coordinate coord = new Coordinate(ad.getLatitude(), ad.getLongitude());
        return coord;
    }

    public String getCityFromCoord(double lat, double lon)
    {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = new ArrayList<>();
        try {
            addresses = geocoder.getFromLocation(lat, lon, 7);
        }catch(IOException ioException){
            Toast.makeText(this,"Geocoder Service Not Available", Toast.LENGTH_SHORT).show();
            return null;
        }catch(IllegalArgumentException illegalArgumentException){
            Toast.makeText(this,"Incorrect GPS Coordination", Toast.LENGTH_SHORT).show();
            return null;
        }
        if(addresses == null || addresses.size() == 0)
        {
            Toast.makeText(this,"City Not Found", Toast.LENGTH_SHORT).show();
        }
        Address ad = addresses.get(0);
        return ad.getAdminArea();
    }

    public void setWeatherImage(String icon)
    {
        ImageView weather = findViewById(R.id.weather_image);
        if(icon.equals("01d"))
        {
            weather.setBackground(getResources().getDrawable(R.drawable.sunny_icon_background));
            ViewCompat.setBackgroundTintList(weather, getResources().getColorStateList(R.color.holo_orange_light));
        }
        else if(icon.equals("01n"))
        {
            weather.setBackground(getResources().getDrawable(R.drawable.sunny_night_icon_background));
        }
        else if(icon.startsWith("02"))
        {
            weather.setBackground(getResources().getDrawable(R.drawable.sun_cloud_icon_background));
        }
        else if(icon.startsWith("03")) weather.setBackground(getResources().getDrawable(R.drawable.cloudy_icon_background));
        else if(icon.startsWith("04")) weather.setBackground(getResources().getDrawable(R.drawable.cloudy_icon_background));
        else if(icon.startsWith("09")) weather.setBackground(getResources().getDrawable(R.drawable.shower_icon_background));
        else if(icon.startsWith("10")) weather.setBackground(getResources().getDrawable(R.drawable.rainy_icon_background));
        else if(icon.startsWith("11"))
        {
            weather.setBackground(getResources().getDrawable(R.drawable.thunder_icon_background));
            ViewCompat.setBackgroundTintList(weather, getResources().getColorStateList(R.color.holo_orange_light));
        }
        else if(icon.startsWith("13")) weather.setBackground(getResources().getDrawable(R.drawable.snow_icon_background));
        else if(icon.startsWith("50")) weather.setBackground(getResources().getDrawable(R.drawable.mist_icon_background));
    }

    public void getCurrentLocation()
    {
        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        String locationProvider = LocationManager.GPS_PROVIDER;
        @SuppressLint("MissingPermission")
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation != null) {
            currentLat = lastKnownLocation.getLatitude();
            currentLng = lastKnownLocation.getLongitude();
            currentCity = getCityFromCoord(currentLat,currentLng);
            Toast.makeText(this, getCityFromCoord(currentLat,currentLng), Toast.LENGTH_LONG);
        }
    }


}

