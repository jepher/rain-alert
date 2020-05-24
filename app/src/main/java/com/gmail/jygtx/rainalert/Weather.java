package com.gmail.jygtx.rainalert;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

// weather object that stores weather data for a given hour
public class Weather implements Parcelable{
    public String time;

    public int iconId;
    public String iconDescription;

    public int temperature;
    public int realFeelTemperature;
    public String temperatureUnits;

    public double windSpeed;
    public String windSpeedUnits;

    public int uvIndex;
    public String uvIndexDescription;

    public boolean hasPrecipitation;
    public int precipitationProbability;

    public int rainProbability;
    public double rainAmount;
    public String rainUnits;

    public int snowProbability;
    public double snowAmount;
    public String snowUnits;

    public int iceProbability;
    public double iceAmount;
    public String iceUnits;

    public Weather(JSONObject data) throws JSONException {
        time = formatTime(data.get("DateTime").toString());

        iconId = (int)data.get("WeatherIcon");
        iconDescription = data.get("IconPhrase").toString();

        temperature = (int)Math.round((double)data.getJSONObject("Temperature").get("Value"));
        realFeelTemperature = (int)Math.round((double)data.getJSONObject("RealFeelTemperature").get("Value"));
        temperatureUnits = data.getJSONObject("Temperature").get("Unit").toString();

        windSpeed = (double)data.getJSONObject("Wind").getJSONObject("Speed").get("Value");
        windSpeedUnits = data.getJSONObject("Wind").getJSONObject("Speed").get("Unit").toString();

        uvIndex = (int)data.get("UVIndex");
        uvIndexDescription = data.get("UVIndexText").toString();

        hasPrecipitation = (boolean)data.get("HasPrecipitation");
        precipitationProbability = (int)data.get("PrecipitationProbability");

        rainProbability = (int)data.get("RainProbability");
        rainAmount = (double)data.getJSONObject("Rain").get("Value");
        rainUnits = data.getJSONObject("Rain").get("Unit").toString();

        snowProbability = (int)data.get("SnowProbability");
        snowAmount = (double)data.getJSONObject("Snow").get("Value");
        snowUnits = data.getJSONObject("Snow").get("Unit").toString();

        iceProbability = (int)data.get("IceProbability");
        iceAmount = (double)data.getJSONObject("Ice").get("Value");
        iceUnits = data.getJSONObject("Ice").get("Unit").toString();
    }

    protected Weather(Parcel in) {
        time = in.readString();

        iconId = in.readInt();
        iconDescription = in.readString();

        temperature = in.readInt();
        realFeelTemperature = in.readInt();
        temperatureUnits = in.readString();

        windSpeed = in.readDouble();
        windSpeedUnits = in.readString();

        uvIndex = in.readInt();
        uvIndexDescription = in.readString();

        hasPrecipitation = in.readByte() != 0; // icon1 = true
        precipitationProbability = in.readInt();

        rainProbability = in.readInt();
        rainAmount = in.readDouble();
        rainUnits = in.readString();

        snowProbability = in.readInt();
        snowAmount = in.readDouble();
        snowUnits = in.readString();

        iceProbability = in.readInt();
        iceAmount = in.readDouble();
        iceUnits = in.readString();
    }

    private String formatTime(String dateTime) {
        String hourString = dateTime.substring(dateTime.indexOf('T') + 1);
        hourString = hourString.substring(0, 5);

        // extract value for hour
        int hour = Integer.parseInt(hourString.substring(0, hourString.indexOf(":")));

        // determine if AM or PM
        String amOrPm;
        if(hour > 12) {
            hour %= 12;
            amOrPm = "PM";
        }
        else {
            if(hour == 0)
                hour = 12;
            amOrPm = "AM";
        }

//        hourString = String.format("%02d:00", hour);
//        return hourString + " " + amOrPm;
        return hour + " " + amOrPm;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(time);
        parcel.writeInt(iconId);
        parcel.writeString(iconDescription);
        parcel.writeInt(temperature);
        parcel.writeInt(realFeelTemperature);
        parcel.writeString(temperatureUnits);
        parcel.writeDouble(windSpeed);
        parcel.writeString(windSpeedUnits);
        parcel.writeInt(uvIndex);
        parcel.writeString(uvIndexDescription);
        parcel.writeByte((byte) (hasPrecipitation ? 1 : 0)); // convert boolean to byte
        parcel.writeInt(precipitationProbability);
        parcel.writeInt(rainProbability);
        parcel.writeDouble(rainAmount);
        parcel.writeString(rainUnits);
        parcel.writeInt(snowProbability);
        parcel.writeDouble(snowAmount);
        parcel.writeString(snowUnits);
        parcel.writeInt(iceProbability);
        parcel.writeDouble(iceAmount);
        parcel.writeString(iceUnits);
    }


    public static final Creator<Weather> CREATOR = new Creator<Weather>() {
        @Override
        public Weather createFromParcel(Parcel in) {
            return new Weather(in);
        }

        @Override
        public Weather[] newArray(int size) {
            return new Weather[size];
        }
    };
}
