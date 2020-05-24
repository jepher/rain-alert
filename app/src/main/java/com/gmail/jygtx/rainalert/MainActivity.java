package com.gmail.jygtx.rainalert;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.location.Address;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class MainActivity extends AppCompatActivity {
    private NotificationManagerCompat notificationManager;

    private AlarmManager alarmManager;
    private CustomCountDownTimer countDownTimer1;
    private CustomCountDownTimer countDownTimer2;
    private Calendar clock;
    private long timeOfAlarm1;
    private long timeOfAlarm2;
    private long timeLeftInMillis1;
    private long timeLeftInMillis2;

    private FusedLocationProviderClient locationClient;
    private Geocoder geocoder;
    private List<Address> addresses;

    private String zipCode;
    private String location;
    private String locationKey;
    private String summary;
    private String detailedReport;
    private boolean alertOn = false;
    private boolean useLocation = false;
    private Weather[] data;
    private long timeOfLastReport; // time in milliseconds since 01/01/1970
    private int dataToDisplay; // index in data array to display
    private CustomCountDownTimer updateTimer; // updates the weather display every hour

    TextView locationTextView;
    TextView countDownTextView;
    TextView temperatureTextView;
    TextView weatherDescriptionTextView;
    TextView reportTextView;
    Button changeLocationBtn;
    Button setAlarmBtn;
    ToggleButton toggleBtn;
    Switch locationSwitch;
    ImageView weatherIconImageView;

    public Toast toast;

    // shared prefs keys
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String ZIP_CODE = "zip_code";
    public static final String LOCATION = "location";
    public static final String LOCATION_KEY = "location_key";
    public static final String ALERT_ON = "alert_on";
    public static final String USE_LOCATION = "use_location";
    public static final String ALARM_1_TIME = "alarm_1_time";
    public static final String ALARM_2_TIME = "alarm_2_time";
    public static final String DATA = "data";
    public static final String TIME_OF_LAST_REPORT = "time_of_last_report";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestLocationPermission();

        notificationManager = NotificationManagerCompat.from(this);
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        locationTextView = findViewById(R.id.locationTextView);
        countDownTextView = findViewById(R.id.countDownTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        weatherDescriptionTextView = findViewById(R.id.weatherDescriptionTextView);
        reportTextView = findViewById(R.id.reportTextView);
        changeLocationBtn = findViewById(R.id.changeLocationBtn);
        locationSwitch = findViewById(R.id.locationSwitch);
        toggleBtn = findViewById(R.id.toggleBtn);
        setAlarmBtn = findViewById(R.id.setAlarmBtn);
        weatherIconImageView = findViewById(R.id.weatherIconImageView);


        changeLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showZipCodeDialog(MainActivity.this);
            }
        });

        // click to use last reported location
        locationSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                useLocation = !useLocation;

                if(useLocation) {
                    getCurrentLocation();
//                    Log.d("zip code", zipCode);
                    getLocationKey(zipCode);
                }
            }
        });

        // toggle rain alerts
        toggleBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(alertOn) {
                    toast = Toast.makeText(getApplicationContext(), "Automatic rain reports disabled", Toast.LENGTH_LONG);

                    // cancel alarms
                    cancelAlarms();
                }
                else {
                    toast = Toast.makeText(getApplicationContext(), "Automatic rain reports enabled", Toast.LENGTH_LONG);

                    // set time
                    showTimePickerDialog(MainActivity.this);
                }
                toast.show();
                alertOn = !alertOn;

                // update set alarm button
                updateViews();
            }
        });

        // set time of first report
        setAlarmBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showTimePickerDialog(MainActivity.this);
            }
        });

        // load settings saved from previous sessions
        try {
            loadData();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // update views
        updateViews();

        // update weather display
        updateWeatherDisplay();

        // start count down displays
        if(alertOn)
            startCountDown();
    }

    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_COARSE_LOCATION}, 1);
    }

    public void sendNotification(){
        Notification notification = new NotificationCompat.Builder(this, NotificationService.ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_black)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentTitle("Rain Report")
                .setContentText(summary)
                .build();
        notificationManager.notify(1, notification);
    }

    private void showZipCodeDialog(Context c) {
        final EditText zipCodeEditText = new EditText(c);
        zipCodeEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle("Change Location")
                .setMessage("Enter your zip code:")
                .setView(zipCodeEditText)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         String zipCode = String.valueOf(zipCodeEditText.getText());
                         Log.d("zip code", zipCode);
                         // disable automatic location getter
                         if(useLocation)
                             useLocation = false;

                         getLocationKey(zipCode);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void showTimePickerDialog(Context c){
        // change default timepicker mode to spinner
        ContextThemeWrapper spinnerContext = new ContextThemeWrapper(c,android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        final TimePicker timePicker = new TimePicker(spinnerContext);

        AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle("Set Alarm Time")
                .setMessage("Set the time for the first rain report (a second report will trigger 6 hours later):")
                .setView(timePicker)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // cancel current alarms
                        cancelAlarms();

                        int hour = timePicker.getCurrentHour();
                        int min = timePicker.getCurrentMinute();

                        setAlarm(hour, min);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    public void getLocationKey(String zipCode){
        if(zipCode == null) {
            toast = Toast.makeText(getApplicationContext(), "Location not found", Toast.LENGTH_LONG);
            toast.show();
        }
        else {
            Intent getLocationIntent = new Intent(this, WebService.class);
            Bundle extras = new Bundle();
            extras.putString("Request", "Location");
            extras.putString("Zip Code", zipCode);
            getLocationIntent.putExtras(extras);

            ContextCompat.startForegroundService(this, getLocationIntent);
        }
    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // get extras from intent
            Bundle extras = intent.getExtras();

            int responseCode = extras.getInt("Response Code");

            // update location text
            location = extras.getString("Location");

            // update location key
            String newLocationKey = extras.getString("Location Key");
            locationKey = newLocationKey;

            if (responseCode == 200){
                // update data
                getWeatherData();
            }
            else {
                data = null;

                String toastMessage;
                if(responseCode == 503)
                    toastMessage = "Maximum daily requests exceeded";
                else
                    toastMessage = "Invalid zip code";

                toast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
                toast.show();
            }

            Log.d("location key", locationKey);
            updateWeatherDisplay();
            updateViews();
        }
    };

    public void getCurrentLocation(){
        // check if app has permission to use gps
        if (ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    // get coordinates from location object
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    try {
                        // get address object from coordinates
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);

                        Log.d("address", addresses.toString());
                        // get zip code from address object
                        zipCode = addresses.get(0).getPostalCode();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void getWeatherData(){
        // create intent
        Intent getDataIntent = new Intent(this, WebService.class);

        // add extras
        Bundle extras = new Bundle();
        extras.putString("Request", "Data");
        extras.putString("Location Key", locationKey);
        getDataIntent.putExtras(extras);

        // send intent to web service
        ContextCompat.startForegroundService(this, getDataIntent);

        // save time
        clock.getInstance();
        timeOfLastReport = clock.getTimeInMillis();
        dataToDisplay = 0;
    }

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // get extras from intent
            Bundle extras = intent.getExtras();

            int responseCode = extras.getInt("Response Code");

            if(responseCode == 200)
                data = (Weather[]) (extras.getParcelableArray("Data"));
            else {
                String toastMessage;
                if(responseCode == 503)
                    toastMessage = "Maximum daily requests exceeded";
                else
                    toastMessage = "Could not get data";

                toast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    };

    //	checks if it is likely to rain in the next 12 hours
    public void getReport() {
        if(data == null){
            summary = "No data to report";
            detailedReport = summary;
        }
        else {
            String startTime = "";
            String endTime = "";
            boolean counting = false; // check if raining for consecutive hours
            double maxRainAmount = 0; // keep track of maximum rainfall(inches)
            double minRainAmount = Double.MAX_VALUE;
            Pair<String, String>[] rainIntervals = new Pair[data.length]; // keep track of all the periods of time with rain
            int currentPosition = 0; // counter for rainIntervals

            // iterate through data to see if there is rain
            for (int i = dataToDisplay; i < data.length; i++) {
                double rainAmount = data[i].rainAmount;
                maxRainAmount = Math.max(maxRainAmount, rainAmount);
                minRainAmount = Math.min(minRainAmount, rainAmount);

                int rainProbability = data[i].rainProbability;

                if (rainProbability > 40) { // start counting if probability for rain is over 40 percent
                    if (!counting) {
                        counting = true;

                        startTime = data[i].time;
                    }
                }
                // time interval ends if probability for rain drops under 10 percent or all the data has bee iterated through
                if (counting && (rainProbability < 10 || i == (data.length - 1))) {
                    if (i < data.length - 1)
                        counting = false;

                    endTime = data[i].time;
                    rainIntervals[currentPosition] = Pair.create(startTime, endTime);
                    currentPosition++;
                }
            }

            // determine if it will rain
            if (rainIntervals[0] == null) {
                summary = "It is not likely to rain in the next 12 hours!";
                detailedReport = summary;
            } else {
                // determine rain descriptions
                String maxRainDescription = getRainDescription(maxRainAmount);
                String minRainDescription = getRainDescription(minRainAmount);

                summary = "Bring your umbrella! Expect " + maxRainDescription + " today.";

                detailedReport = "Bring your umbrella! \nIt is likely to rain ";
                // report rain intervals
                for (int i = 0; i < currentPosition; i++) {
                    if (i == currentPosition - 1) {
                        detailedReport += "and from " + rainIntervals[i].first + " to " + rainIntervals[i].second;
                        if (counting)
                            detailedReport += " onwards.";
                        else
                            detailedReport += ".";
                    } else {
                        detailedReport += "from " + rainIntervals[i].first + " to " + rainIntervals[i].second + ", ";
                    }
                }

                // report rain range
                if (maxRainDescription.equals(minRainDescription))
                    detailedReport += "\nExpect " + maxRainDescription + " today.";
                else
                    detailedReport += "\nRain ranges from " + minRainDescription + " to " + maxRainDescription + ".";
            }
        }
    }

    private String getRainDescription(double rainAmount) {
        if (rainAmount >= 0.3) // heavy rain
            return "heavy rain";
        else if (rainAmount >= 0.1) // moderate rain
            return "moderate rain";
        else // drizzle
            return "a drizzle";
    }

    // if data is more than 6 hours old, get new data
    public void validateData(){
        clock.getInstance();
        long timeDifference = clock.getTimeInMillis() - timeOfLastReport;
        if(timeDifference >= 6 * AlarmManager.INTERVAL_HOUR) {
            getWeatherData();
        }
        else {
            // update weather display pointer
            dataToDisplay = (int) (timeDifference / AlarmManager.INTERVAL_HOUR);
        }

        getReport();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // create intent filters
        IntentFilter locationFilter = new IntentFilter("com.gmail.jygtx.rainalert.UPDATELOCATION");
        IntentFilter reportFilter = new IntentFilter("com.gmail.jygtx.rainalert.GETDATA");
        IntentFilter alarmFilter = new IntentFilter("com.gmail.jygtx.rainalert.ALARMRECEIVED");

        // register receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, locationFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, reportFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(alarmReceiver, alarmFilter);

        // start update timer
        clock.getInstance();
        long timeToNextHour = AlarmManager.INTERVAL_HOUR - (clock.getTimeInMillis() % AlarmManager.INTERVAL_HOUR);
        updateTimer = new CustomCountDownTimer(timeToNextHour, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                // update weather display
                updateWeatherDisplay();

                // restart count down
                cancel();
                setMillisInFuture(AlarmManager.INTERVAL_HOUR);
                start();
            }
        }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // unregister receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmReceiver);

        clock = Calendar.getInstance();

        // save user settings
        saveData();

        // cancel update timer
        updateTimer.cancel();
    }

    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ZIP_CODE, zipCode);
        editor.putString(LOCATION, location);
        editor.putString(LOCATION_KEY, locationKey);
        editor.putBoolean(ALERT_ON, toggleBtn.isChecked());
        editor.putBoolean(USE_LOCATION, locationSwitch.isChecked());
        editor.putLong(ALARM_1_TIME, timeOfAlarm1);
        editor.putLong(ALARM_2_TIME, timeOfAlarm2);
        editor.putLong(TIME_OF_LAST_REPORT, timeOfLastReport);

        // convert weather data into json string
        Gson gson = new Gson();
        String json = gson.toJson(data);
        editor.putString(DATA, json);

        editor.apply();
    }

    public void loadData() throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        useLocation = sharedPreferences.getBoolean(USE_LOCATION, false);
        alertOn = sharedPreferences.getBoolean(ALERT_ON, false);
        // update current location instead of using saved data if location changed
        if(useLocation){
            getCurrentLocation();
            if(zipCode != sharedPreferences.getString(ZIP_CODE, "")) {
                zipCode = sharedPreferences.getString(ZIP_CODE, "");
                getLocationKey(zipCode);
            }
        }
        else{
            location = sharedPreferences.getString(LOCATION, "Set location");
            locationKey = sharedPreferences.getString(LOCATION_KEY, "");
        }

        // get alarm times
        timeOfAlarm1 = sharedPreferences.getLong(ALARM_1_TIME, 0);
        timeOfAlarm2 = sharedPreferences.getLong(ALARM_2_TIME, 0);

        // calculate time left
        timeLeftInMillis1 = getTimeLeftMillis(timeOfAlarm1);
        timeLeftInMillis2 = getTimeLeftMillis(timeOfAlarm2);

        // get time of last report
        timeOfLastReport = sharedPreferences.getLong(TIME_OF_LAST_REPORT, 0);

        // get weather data from last call
        Gson gson = new Gson();
        String jsonString = sharedPreferences.getString(DATA, "");
        JSONArray jsonArray = new JSONArray(jsonString);
        data = new Weather[jsonArray.length()];
        for(int i = 0; i < jsonArray.length(); i++){
            data[i] = gson.fromJson(jsonArray.get(i).toString(), Weather.class);
        }
    }

    public void updateViews(){
        // update location text view
        locationTextView.setText(location);

        // update use location switch
        locationSwitch.setChecked(useLocation);

        // update alert switch
        toggleBtn.setChecked(alertOn);

        // update set alarm button
        setAlarmBtn.setEnabled(alertOn);
    }

    private void updateCountDownText(){
        if(alertOn) {
            long timeLeftInMillis;

            // determine which count down to display
            if (timeLeftInMillis1 < timeLeftInMillis2) {
                timeLeftInMillis = timeLeftInMillis1;
            }
            else {
                timeLeftInMillis = timeLeftInMillis2;
            }

            // calculate hours, minutes, and seconds from millis
            int hours = (int) ((timeLeftInMillis / (1000 * 60 * 60)) % 24);
            int mins = (int) ((timeLeftInMillis / (1000 * 60)) % 60);
            int secs = (int) (timeLeftInMillis / 1000) % 60;

            // format time and display
            String timeLeftFormatted = String.format("Next alert: %02d:%02d:%02d", hours, mins, secs);
            countDownTextView.setText(timeLeftFormatted);
        }
        else countDownTextView.setText("");
    }

    private void updateWeatherDisplay(){
        validateData();

        reportTextView.setText(detailedReport);
        if(data != null) {
            temperatureTextView.setText(data[0].temperature + " \u00B0" + data[0].temperatureUnits);

            weatherIconImageView.setVisibility(View.VISIBLE);
            int resID = getResources().getIdentifier("icon" + data[0].iconId, "drawable", getPackageName());
            weatherIconImageView.setImageResource(resID);
            weatherDescriptionTextView.setText(data[0].iconDescription);
        }
        else{
            temperatureTextView.setText("");
            weatherIconImageView.setVisibility(View.INVISIBLE);
            weatherDescriptionTextView.setText("");
        }
    }

    private void setAlarm(int hour, int min) {
        // set time for first alarm
        Calendar alarm = Calendar.getInstance();
        alarm.set(Calendar.HOUR_OF_DAY, hour);
        alarm.set(Calendar.MINUTE, min);
        alarm.set(Calendar.SECOND, 0);
        timeOfAlarm1 = alarm.getTimeInMillis() % AlarmManager.INTERVAL_DAY;

        // start first alarm
        Intent alarmIntent1 = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingAlarmIntent1 = PendingIntent.getBroadcast(this, 1, alarmIntent1, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC, timeOfAlarm1, AlarmManager.INTERVAL_DAY, pendingAlarmIntent1);

        // set time for second alarm
        alarm.set(Calendar.HOUR_OF_DAY, hour + 6);
        timeOfAlarm2 = alarm.getTimeInMillis() % AlarmManager.INTERVAL_DAY;

        // start second alarm
        Intent alarmIntent2 = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingAlarmIntent2 = PendingIntent.getBroadcast(this, 2, alarmIntent2, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC, timeOfAlarm2, AlarmManager.INTERVAL_DAY, pendingAlarmIntent2);

        // start count downs
        startCountDown();
    }

    private BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            validateData();

            // send report as notification
            sendNotification();
        }
    };

    private long getTimeLeftMillis(long setTime){
        // calculate time left to alarms
        clock = Calendar.getInstance();
        long currentTime = clock.getTimeInMillis() % AlarmManager.INTERVAL_DAY;

        // check if current time is past time set for today
        if(setTime < currentTime)
            return AlarmManager.INTERVAL_DAY - currentTime + setTime;
        else
            return setTime - currentTime;
    }

    private void startCountDown(){
        // get time left in timer icon1
        timeLeftInMillis1 = getTimeLeftMillis(timeOfAlarm1);

        // start countdown timer icon1
        countDownTimer1 = new CustomCountDownTimer(timeLeftInMillis1, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis1 = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                cancel();
                setMillisInFuture(AlarmManager.INTERVAL_DAY);
                start();
            }
        }.start();

        // get time left in timer 2
        timeLeftInMillis2 = getTimeLeftMillis(timeOfAlarm2);

        // start countdown timer 2
        countDownTimer2 = new CustomCountDownTimer(timeLeftInMillis2, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis2 = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                cancel();
                setMillisInFuture(AlarmManager.INTERVAL_DAY);
                start();
            }
        }.start();
    }

    private void cancelAlarms(){
        // get alarm intents
        Intent cancelIntent1 = new Intent(MainActivity.this, AlarmReceiver.class);
        PendingIntent pendingCancelIntent1 = PendingIntent.getBroadcast(MainActivity.this, 1, cancelIntent1, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent cancelIntent2 = new Intent(MainActivity.this, AlarmReceiver.class);
        PendingIntent pendingCancelIntent2 = PendingIntent.getBroadcast(MainActivity.this, 2, cancelIntent2, PendingIntent.FLAG_CANCEL_CURRENT);

        // cancel intents
        alarmManager.cancel(pendingCancelIntent1);
        alarmManager.cancel(pendingCancelIntent2);

        // cancel count downs
        countDownTimer1.cancel();
        countDownTimer2.cancel();
    }
}
