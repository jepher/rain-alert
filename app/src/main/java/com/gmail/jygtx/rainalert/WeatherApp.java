package com.gmail.jygtx.rainalert;

// made using AccuWeather API

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherApp {
    private static String apiKey = "ExW1ayjnVikxRuFTU2HY5xHsYTUpeEbO";
    private static String locationKey;
    private static String location;

    private static Weather[] weatherData;

    // return response code
    public int getLocationKey(String zipCode) throws IOException, JSONException {
        URL url = new URL("http://dataservice.accuweather.com/locations/v1/postalcodes/search?apikey=" + apiKey + "&q="
                + zipCode);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        // check if valid zip code
        if (con.getResponseCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            // parse through json file
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            con.disconnect();

            // convert parsed json string into json object
            String jsonString = content.toString();
            jsonString = jsonString.substring(1, jsonString.length() - 1); // get rid of opening and closing brackets
            JSONObject json = new JSONObject(jsonString);

            // extract location data from json
            locationKey = json.get("Key").toString();
            location = json.get("LocalizedName") + ", " + json.getJSONObject("AdministrativeArea").getString("ID");
            return 200;
        }
        else {
            locationKey = "";
            location = "Set location";
            return con.getResponseCode();
        }
    }

    // populates weather data array with weather objects
    public int getWeather(String locationKey) throws IOException, JSONException {
        URL url = new URL("http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/" + locationKey + "?apikey=" + apiKey + "&details=true");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        // check if connection successful
        if (con.getResponseCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            // parse through json file
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            con.disconnect();

            // convert parsed json string into json array object
            String jsonString = content.toString();
            JSONArray data = new JSONArray(jsonString);

            weatherData = new Weather[data.length()];

            // populate weather data array
            for (int i = 0; i < data.length(); i++) {
                Weather weatherObject = new Weather((JSONObject) data.get(i));
                weatherData[i] = weatherObject;
            }

            return 200;
        }
        else{
            weatherData = null;
            return con.getResponseCode();
        }
    }

    public String getLocation() {
        return location;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public Weather[] getData() {
        return weatherData;
    }
}
