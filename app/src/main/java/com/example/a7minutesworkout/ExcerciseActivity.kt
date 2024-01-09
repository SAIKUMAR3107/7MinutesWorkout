package com.example.a7minutesworkout

import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a7minutesworkout.databinding.ActivityExcerciseBinding
import com.example.a7minutesworkout.databinding.ActivityMainBinding
import com.example.a7minutesworkout.databinding.DialogCustomBackConfirmationBinding
import java.util.*
import kotlin.collections.ArrayList

class ExcerciseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var binding: ActivityExcerciseBinding? = null

    private var restTimer : CountDownTimer? = null
    private var restProgress = 0

    private var excerciseTimer : CountDownTimer? = null
    private var excerciseProgress = 0

    private var excerciseList : ArrayList<ExcerciseModel>? = null
    private var currentExcercisePosition = -1

    private var tts: TextToSpeech? = null
    private var player : MediaPlayer? = null

    private var excerciseAdapter : ExcerciseStatusAdapter? = null

    private var restTimerDuration : Long = 10
    private var excerciseTimeDuration : Long = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcerciseBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarExcercise)

        if(supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        excerciseList = Constants.defaultExcerciseList()

        tts = TextToSpeech(this,this,)

        binding?.toolbarExcercise?.setNavigationOnClickListener {
            customDialogForBackButton()
        }
        setupRestView()
        setupExcerciseStatusRecyclerView()

    }

    override fun onBackPressed() {
        customDialogForBackButton()
        //super.onBackPressed()
    }

    private fun customDialogForBackButton(){
        val customDialog = Dialog(this)
        val dialogBinding = DialogCustomBackConfirmationBinding.inflate(layoutInflater)
        customDialog.setContentView(dialogBinding.root)
        customDialog.setCanceledOnTouchOutside(false)
        dialogBinding.btnYes.setOnClickListener {
            //Todo 6 We need to specify that we are finishing this activity if not the player
            // continues beeping even after the screen is not visibile
            this@ExcerciseActivity.finish()
            customDialog.dismiss()
        }
        dialogBinding.btnNo.setOnClickListener {
            customDialog.dismiss()
        }
        customDialog.show()
    }

    private fun setupExcerciseStatusRecyclerView() {
        binding?.rvExcerciseStatus?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        excerciseAdapter = ExcerciseStatusAdapter(excerciseList!!)
        binding?.rvExcerciseStatus?.adapter = excerciseAdapter
    }

    private fun setupRestView() {

        try{
            val soundURI = Uri.parse("android.resource://com.example.a7minutesworkout/" + R.raw.press_start)
            player = MediaPlayer.create(applicationContext,soundURI)
            player?.isLooping = false
            player?.start()

        }catch (e: Exception) {
            e.printStackTrace()
        }



        binding?.flRestView?.visibility = View.VISIBLE
        binding?.tvTitle?.visibility = View.VISIBLE
        binding?.tvExerciseName?.visibility = View.INVISIBLE
        binding?.flExcerciseView?.visibility = View.INVISIBLE
        binding?.ivImage?.visibility = View.INVISIBLE
        binding?.tvUpcomingLabel?.visibility = View.VISIBLE
        binding?.tvUpcomingExcerciseName?.visibility = View.VISIBLE
        binding?.tvUpcomingExcerciseName?.text = excerciseList!![currentExcercisePosition + 1].getName()

        if(restTimer != null) {
            restTimer?.cancel()
            restProgress = 0
        }
        setRestProgressBar()
    }
    private fun setupExcerciseView() {

        binding?.flRestView?.visibility = View.INVISIBLE
        binding?.tvTitle?.visibility = View.INVISIBLE
        binding?.tvExerciseName?.visibility = View.VISIBLE
        binding?.flExcerciseView?.visibility = View.VISIBLE
        binding?.ivImage?.visibility = View.VISIBLE
        binding?.tvUpcomingLabel?.visibility = View.INVISIBLE
        binding?.tvUpcomingExcerciseName?.visibility = View.INVISIBLE

        if(excerciseTimer != null) {
            excerciseTimer?.cancel()
            excerciseProgress = 0
        }

        speakOut(excerciseList!![currentExcercisePosition].getName())

        binding?.ivImage?.setImageResource(excerciseList!![currentExcercisePosition].getImage())
        binding?.tvExerciseName?.text = excerciseList!![currentExcercisePosition].getName()

        setExcerciseProgressBar()
    }

    private fun setRestProgressBar() {
        binding?.progressBar?.progress = restProgress

        restTimer = object : CountDownTimer(restTimerDuration * 1000,1000){
            override fun onTick(p0: Long) {
                restProgress++
                binding?.progressBar?.progress = 10- restProgress
                binding?.tvTimer?.text = (10 - restProgress).toString()
            }
            override fun onFinish() {
                currentExcercisePosition++
                excerciseList!![currentExcercisePosition].setIsSelected(true)
                excerciseAdapter!!.notifyDataSetChanged()
                setupExcerciseView()
            }
        }.start()
    }
    private fun setExcerciseProgressBar() {
        binding?.progressBarExcercise?.progress = excerciseProgress

        excerciseTimer = object : CountDownTimer(excerciseTimeDuration * 1000,1000){
            override fun onTick(p0: Long) {
                excerciseProgress++
                binding?.progressBarExcercise?.progress = 30- excerciseProgress
                binding?.tvTimerExcercise?.text = (30 - excerciseProgress).toString()
            }
            override fun onFinish() {
                if(currentExcercisePosition < excerciseList?.size!! - 1){
                    excerciseList!![currentExcercisePosition].setIsSelected(false)
                    excerciseList!![currentExcercisePosition].setIsCompleted(true)
                    excerciseAdapter!!.notifyDataSetChanged()
                    setupRestView()
                }else{
                    finish()
                    val intent = Intent(this@ExcerciseActivity,FinishActivity :: class.java)
                    startActivity(intent)
                }
            }
        }.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        if(restTimer != null) {
            restTimer?.cancel()
            restProgress = 0
        }
        if(excerciseTimer != null) {
            excerciseTimer?.cancel()
            excerciseProgress = 0
        }
        if(tts != null){
            tts!!.stop()
            tts!!.shutdown()
        }
        if(player != null){
            player!!.stop()
        }


        binding = null
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun speakOut(text: String) {
        tts!!.speak(text,TextToSpeech.QUEUE_FLUSH,null,"")
    }
}