package com.example.david.edt;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.widget.TextView;
import android.widget.Toast;

import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.example.david.edt.views.EDTWeekView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by david on 09/10/17.
 */


public class WeekActivity extends AppCompatActivity {
    private Events events;
    private EDTWeekView weekView;

    public static final String PREFS_NAME = "MyPrefsFile";
    SharedPreferences mSettings;

    VoicePattern voicePattern;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int CHECK_CODE = 0x1;
    private final int SHORT_DURATION = 1000;
    private VoiceSynth voiceSynth;

    public static String[] MOIS = {"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Aout","Septembre","Octobre","Novembre","Décembre"};
    public static String[] JOURS = {"Dimanche", "Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi"};

    TextView textbox;

    OrientationEventListener orientationEventListener;
    boolean switchPortrait;
    boolean switchLandscape;

    private boolean mVoice;

    @Override
    protected void onStart(){
        super.onStart();

        checkTTS();

    }

    @Override
    public void onCreate(Bundle savedInstanteState){
        switchPortrait = false;
        switchLandscape = false;

        super.onCreate(savedInstanteState);

        mSettings = getSharedPreferences(PREFS_NAME,0);
        mVoice = mSettings.getBoolean("vocal_mode", false);

        setContentView(R.layout.activity_week);

        voicePattern = new VoicePattern();
        textbox = (TextView) findViewById(R.id.textbox);
        textbox.setText("Reco vocale");

        events = new Events();
        events.generateEvents();
        Log.v("CLASSES",Classes.values()[0].toString());


        weekView = (EDTWeekView)  findViewById(R.id.weekView);
        initMonth();


        Calendar today = Calendar.getInstance();
        Log.v("TODAYCAL", today.toString());

        checkOrientation();

    }

    public void checkOrientation(){
        Log.v("ORIENTATION", "CHECKING");
        Resources res = getResources();
        if(res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            weekView.setNumberOfVisibleDays(3);
            Log.v("ORIENTATION", "Portrait");
        }

        else if(res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            weekView.setNumberOfVisibleDays(7);
            Log.v("ORIENTATION", "Landscape");
        }
    }

    private void initMonth(){
        weekView.goToHour(8.00);
        weekView.setHourHeight(144);
        weekView.setMonthChangeListener(new MonthLoader.MonthChangeListener() {
            @Override
            public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
                ArrayList<WeekViewEvent> eventsMonth = new ArrayList<WeekViewEvent>();
                List<WeekViewEvent> allEvents = events.getEvents();
                for(int i = 0; i < allEvents.size(); i++)
                    if(allEvents.get(i).getStartTime().get(Calendar.MONTH) == newMonth-1)
                        eventsMonth.add(allEvents.get(i));

                Calendar calendar = Calendar.getInstance();
                if(newMonth == (calendar.get(Calendar.MONTH)+1))
                    weekView.goToToday();

                return eventsMonth;
            }
        });

            weekView.setEmptyViewLongPressListener(new WeekView.EmptyViewLongPressListener() {
                @Override
                public void onEmptyViewLongPress(Calendar time) {
                    Log.v("LONGCLICK", "Long click : "+mVoice);
                    //speakOut("Bonjour");
                    if(mVoice)
                        listen();
                }
            });

    }

    private void listen(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getDisplayLanguage());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Vous pouvez parler...");

        try{
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        }

        catch(ActivityNotFoundException e){
            Toast.makeText(getApplicationContext(), "La reconnaissance vocale n'est pas supportée sur cet appareil", Toast.LENGTH_SHORT).show();
        }

    }

    private void speakOut(String text){
        Log.v("SPEAKING", "test");
        if(!voiceSynth.isSpeaking())
            voiceSynth.speach(text);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case CHECK_CODE:
                if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    voiceSynth = new VoiceSynth(this);

                }
                else{
                    Intent install = new Intent();
                    install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(install);
                }

                break;

            case REQ_CODE_SPEECH_INPUT:
                if(resultCode == RESULT_OK && null != data){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    textbox.setText(result.get(0));

                    if(voicePattern.isNextCours(result.get(0)))
                        goToNextCourse();
                    else
                        speakOut("Je n'ai pas compris, pouvez vous répéter svp?");
                }
                break;

            default:
                break;
        }
    }

    public String goToNextCourse(){
        Log.v("NEXTCOURSE","Next course");

        Calendar today = Calendar.getInstance();

        //weekView.setNumberOfVisibleDays(1);
        List<WeekViewEvent> weekViewEvents = events.getEvents();
        for(int i = 0; i < weekViewEvents.size(); i++) {
            WeekViewEvent event = weekViewEvents.get(i);
            //Log.v("COURSE", weekViewEvents.get(i).getName());
            if(today.compareTo(event.getStartTime()) < 0){
                Log.v("COURSE", event.getName() + " , " + event.getStartTime().get(Calendar.HOUR_OF_DAY));
                speakEvent(event);
                Calendar date = (Calendar) event.getStartTime().clone();
                weekView.goToDate(date);
                weekView.goToHour(event.getStartTime().get(Calendar.HOUR_OF_DAY));
                return event.getName();
            }
        }

        speakOut("Aucun cours prochainement");
        Log.v("COURSE", "Nothing found");
        return null;
    }

    public void speakEvent(WeekViewEvent event){
        Calendar today = Calendar.getInstance();

        Calendar c = event.getStartTime();
        String name = event.getName();

        int min = c.get(Calendar.MINUTE);

        String day;
        if(c.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))
            day = "aujourd'hui";
        else if(c.get(Calendar.DAY_OF_MONTH)+0 == today.get(Calendar.DAY_OF_MONTH)+1)
            day = "deux main";
        else
            day = JOURS[c.get(Calendar.DAY_OF_WEEK)-1]+" "+c.get(Calendar.DAY_OF_MONTH) + " " + MOIS[c.get(Calendar.MONTH)];

        String hour = c.get(Calendar.HOUR_OF_DAY)+ " heure " + (min != 0 ? min : "");
        String location = event.getLocation();

        speakOut(name+ ", " + day + " à "+hour+", en "+location);
    }

    @Override
    protected void onStop(){
        // voiceSynth.shutdown();
        super.onStop();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        checkTTS();
    }

    private void checkTTS(){
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);

        boolean v = mSettings.getBoolean("vocal_mode", false);
        menu.findItem(R.id.checkAudioItem).setChecked(v);

        return true;
    }

    //gère le click sur une action de l'ActionBar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.exit:
               finish();
               System.exit(0);

            case R.id.checkAudioItem:
               item.setChecked(!item.isChecked());
                setVoice(item.isChecked());
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void setVoice(boolean v){
        mVoice = v;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("vocal_mode", v);
        editor.apply();

    }

}
