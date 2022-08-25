package com.sakhalinec.mystartcoroutine

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.sleep
import android.widget.Toast
import androidx.core.view.isVisible
import com.sakhalinec.mystartcoroutine.databinding.ActivityMainBinding
import java.lang.Thread.sleep
import kotlin.concurrent.thread

class Thread : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.buttonLoad.setOnClickListener {
            loadData()
        }
    }

    private fun loadData() {
        binding.progress.isVisible = true
        binding.buttonLoad.isEnabled = false
        // тут callback hell это ни есть хорошо! так как таких вложеных друг в друга callback-ов
        // может быть очень много. Такой код сложно читать и дебажить
        loadCity {
            binding.tvLocation.text = it
            loadTemperature(it) {
                binding.tvTemperature.text = it.toString()
                binding.progress.isVisible = false
                binding.buttonLoad.isEnabled = true
            }
        }
    }

    // чтобы вернуть какое то значение нужно в параметры функции передать callback который будет
    // функцией лямбда выражением с нужным для нас параметром и ничего не возвращает
    private fun loadCity(callback: (String) -> Unit) {
        // создание потока
        thread {
            // усыпляем поток на 5 секунд
            Thread.sleep(5000)
            // вызов callback-а и передаем необходимый параметр
            callback.invoke("Moscow")
        }
    }

    private fun loadTemperature(city: String, callback: (Int) -> Unit) {
        thread {
            Toast.makeText(
                this,
                getString(R.string.loading_temperature_toast, city),
                Toast.LENGTH_SHORT
            ).show()
            Thread.sleep(5000)
            callback.invoke(17)
        }
    }
}