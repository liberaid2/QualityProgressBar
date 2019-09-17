package com.example.progressbar

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
                qualityProgressBar.animateArc()
                var currentColor = 0
                val times = 10
                val step = qualityProgressBar.totalAnimationDuration / times
                repeat(times){
                    delay(step)
                    qualityProgressBar.setQualityMillis(it * step, (it + 1) * step, QualityProgressBar.Quality.values()[currentColor++ % 3])
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


