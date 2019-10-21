package com.example.progressbar

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart.setOnClickListener {
            job?.cancel()
            job = GlobalScope.launch(Dispatchers.Main) {
                qualityProgressBar.clearColors()
                qualityProgressBar.clearArcs()

                val times = 5
                val step = qualityProgressBar.totalAnimationDuration / times
                repeat(times){
//                    delay(step)
                    qualityProgressBar.setColor(QualityProgressBar.RecolorInfo.Builder()
                        .setBoundariesMillis(it * step, (it + 1) * step)
                        .setColorRes(this@MainActivity, R.color.colorPrimaryDark)
                        .setAnimate(false)
                        .build())
                }
                qualityProgressBar.animateProgress()

//                val times = 5
//                val step = qualityProgressBar.totalAnimationDuration / times
                repeat(times){
                    delay(step)
                    qualityProgressBar.setColor(QualityProgressBar.RecolorInfo.Builder()
                        .setBoundariesMillis(it * step, (it + 1) * step)
                        .setColorRes(this@MainActivity, if(it % 2 == 0) R.color.colorPrimary else R.color.colorAccent)
                        .setAnimate(true)
                        .build())
                }
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        super.onDestroy()
    }
}


