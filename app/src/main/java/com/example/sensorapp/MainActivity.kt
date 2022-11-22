package com.example.sensorapp


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener,TextToSpeech.OnInitListener {
    private lateinit var mGravity: FloatArray
    private var mAccel = 0f
    private var mAccelCurrent = 0f
    private var mAccelLast = 0f


    private lateinit var tts: TextToSpeech
    private val myLocale : Locale = Locale("pt","BR")

    private var startTime : Double = 0.0
    private var finalTime : Double = 0.0

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekbar: SeekBar

    private val mHandler = Handler()

    //Progre
    private lateinit var currentTime : TextView
    private lateinit var songTitle : TextView
    private lateinit var remainingTitle : TextView

    private lateinit var btnStart : Button
    private lateinit var btnStop : Button

    private var oneTimeOnly : Boolean = false

    private lateinit var run : Runnable

    private lateinit var sensorManager: SensorManager

    var mLightSensor: Sensor?=null
    var mProximitySensor : Sensor?=null
    var mAcelerometerSensor : Sensor?=null

    private val max_proximity = 4

    private var hitCount = 0
    private var hitSum = 0.0
    private var hitResult = 0.0

    var SAMPLE_SIZE = 50;
    var THRESHOLD = 0.2;
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {

        if(event.sensor.type == Sensor.TYPE_LIGHT){
            if(event.values[0] > 20000){
                disableDarkMode()
            }else{
                changeToDarkMode()
            }
        }else if(event.sensor.type == Sensor.TYPE_PROXIMITY){
            if (event.values[0] >= -max_proximity && event.values[0] <= max_proximity) {
                Log.v("START","INICIANDO ${event.values[0]}")
                startStreaming()
            }else{
                stopStreaming()
            }
        }
        else if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values.clone();
            // Shake detection

            // Shake detection
            val x = mGravity[0]
            val y = mGravity[1]
            val z = mGravity[2]



            mAccelLast = mAccelCurrent
            mAccelCurrent = sqrt(x * x + y * y + z * z).toFloat()
            val delta = mAccelCurrent - mAccelLast
            mAccel = mAccel * 0.9f + delta;


            if (hitCount <= SAMPLE_SIZE) {
                hitCount++;
                hitSum += abs(mAccel);
            }else{
                hitResult = hitSum / SAMPLE_SIZE;

                Log.d("MOVIMENTO", hitResult.toString());

                if (hitResult > THRESHOLD) {
                    Log.d("MOVIMENTO", "Walking");
                    speakOut("TOCANDO MÚSICA")
                } else {
                    Log.d("MOVIMENTO", "Stop Walking");
                }

                hitCount = 0;
                hitSum = 0.0;
                hitResult = 0.0;
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //Sensors
        mLightSensor= sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
        mProximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        mAcelerometerSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        currentTime = findViewById(R.id.textCurrentProgress)
        songTitle = findViewById(R.id.textSongTittle)
        remainingTitle = findViewById(R.id.textRemainingTime)

        mediaPlayer = MediaPlayer.create(this,R.raw.sound)

        tts = TextToSpeech(this, this)

        seekbar = findViewById(R.id.seekBar)

        seekbar?.isClickable = false

        run = object : Runnable {
            override fun run() {
                Log.v("POST DELAYED","Rodando...")
                startTime = mediaPlayer.currentPosition.toDouble()
                finalTime = mediaPlayer.duration.toDouble()

                currentTime.text = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
                )

                seekbar.progress = startTime.toInt()
                //Scheduling messages
                mHandler.postDelayed(this, 100)
            }
        }
    }
    private fun changeToDarkMode(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
    private fun disableDarkMode(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
    private fun startStreaming() {
        Toast.makeText(applicationContext, "Playing sound",Toast.LENGTH_SHORT)

        mediaPlayer.start()

        finalTime = mediaPlayer.duration.toDouble()
        startTime = mediaPlayer.currentPosition.toDouble()
        songTitle.text = "Musicona"

        //Primeira vez que executar, irá setar a barra de probresso da música para o tamanho máximo
        //áudio
        if(oneTimeOnly === false){
            seekbar.max = finalTime.toInt()
            oneTimeOnly = true
        }

        currentTime.text = String.format("%d min, %d sec",
            TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
        )

        remainingTitle.text = String.format("%d min, %d sec",
            TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()))
        )

        seekbar.progress = startTime.toInt()

        //The runnable (run) will be run on the thread to which this handler is attached
        // Each Handler instance is associated with a single thread
        mHandler.post(run) // schedule runnable
    }

    private fun stopStreaming(){
        Toast.makeText(applicationContext, "Pausing sound",Toast.LENGTH_SHORT).show();
        if(oneTimeOnly){
            mediaPlayer.seekTo(0)
            mediaPlayer.pause()
        }


    }
    //Quando o TextToSpeech é inicializado, irá cair no onInit
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts.setLanguage(this.myLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {

            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    private fun speakOut(text : String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }
    override fun onDestroy() {
        // Shutdown TTS
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
    override fun onResume() {
        super.onResume()
        mLightSensor?.also{ light ->
            sensorManager.registerListener(this,light,SensorManager.SENSOR_DELAY_NORMAL)
        }
        mProximitySensor?.also{ proximity ->
            sensorManager.registerListener(this,proximity,SensorManager.SENSOR_DELAY_NORMAL)
        }
        mAcelerometerSensor?.also{ ac ->
            sensorManager.registerListener(this,ac,SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}