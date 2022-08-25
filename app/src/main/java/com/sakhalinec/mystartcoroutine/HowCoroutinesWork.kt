package com.sakhalinec.mystartcoroutine

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.sakhalinec.mystartcoroutine.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/*================================== ВАЖНО ПРОЧИТАТЬ!!! =====================================*/
/*
1. suspend функции нельзя вызывать из обычной функции, если представить что метод loadData()
это обычная функция без модификатора suspend то, метод loadCity() это по сути метод с коллбэком
тоесть сам по себе он ничего не возвращает и тогда не понятно что делать с функцией loadCity()

2. suspend функции никогда не должны блокировать поток, в room и retrofit этот принцип поддерживается,
вызов suspend функции на любом потоке не блокирует этот самый поток, но если сами пишем suspend функцию
то нужно самим заботиться о том чтобы поток не блокировался:

private suspend fun loadCity(): String {
        Thread.sleep(5555) ТАК ДЕЛАТЬ НЕЛЬЗЯ! ЭТО ПРИВЕДЕТ К БЛОКИРОВКЕ ПОТОКА.
        return "Moscow"
    }

3. использование корутин это программирование с коллбэками которые будут созданны компилятором.
4. suspend функции в своей реализации используют state-machine чтобы один и тот же метод можно было
вызывать с разными состояниями
5. suspend функции можно запустить только из другой suspend функции или из корутины
*/

class HowCoroutinesWork : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.buttonLoad.setOnClickListener {
            lifecycleScope.launch {
                loadData()
            }
//            loadWithoutCoroutine()
        }
    }

    // работу этой функции можно разделить на 3 шага
    private suspend fun loadData() {
        Log.d("MainActivity", "Load started: $this")
        binding.progress.isVisible = true
        binding.buttonLoad.isEnabled = false
        // при вызове этого метода, программа выйдет из метода loadData() и когда вся работа
        // в loadCity() завершится, программа вернется в метод loadData() и продолжит дальше
        // выполнять работу со следующей строчки
        val city = loadCity() // выход из функции

        // та самая следующая строчка, тут программа продолжит работу когда вернется из loadCity()
        binding.tvLocation.text = city
        val temp = loadTemperature(city) // выход из функции

        // тут программа продолжит работу когда вернется из loadTemperature(city)
        binding.tvTemperature.text = temp.toString()
        binding.progress.isVisible = false
        binding.buttonLoad.isEnabled = true
        Log.d("MainActivity", "Load finished: $this")
    }

    // приблизительный принцип работы корутин, попытка иммитации state machine
    // запустится с шагом 0, выполнит код до вызова самого себя, но уже с шагом 1
    private fun loadWithoutCoroutine(step: Int = 0, obj: Any? = null) {
        // step это иммитация тех самых трех блоков программы в функции loadData()
        when (step) {
            0 -> {
                Log.d("MainActivity", "Load started: $this")
                binding.progress.isVisible = true
                binding.buttonLoad.isEnabled = false
                // тут выполнение программы будет остановлено, так как чтобы попасть в другие блоки
                // нужно дождаться получения результатов и запустить снова функцию loadWithoutCoroutine()
                // но уже с другим шагом
                loadCityWithoutCoroutine {
                    // запустится с шагом 1
                    loadWithoutCoroutine(1, it) // передаем город и на шаге 1 он будет установлен
                }
            }
            1 -> {
                // установка города
                val city = obj as String
                binding.tvLocation.text = city
                loadTemperatureWithoutCoroutine(city) {
                    // после получения температуры, запустится с шагом 2
                    loadWithoutCoroutine(2, it) // передаем температуру
                }
            }
            2 -> {
                // установка температуры и продолжение работы программы, уже до конца
                val temp = obj as Int
                binding.tvTemperature.text = temp.toString()
                binding.progress.isVisible = false
                binding.buttonLoad.isEnabled = true
                Log.d("MainActivity", "Load finished: $this")
            }
        }
    }

    // функция с колбеком для шага 0
    private fun loadCityWithoutCoroutine(callback: (String) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            callback.invoke("Moscow")
        }, 5000)
    }

    // функция с колбеком для шага 1
    private fun loadTemperatureWithoutCoroutine(city: String, callback: (Int) -> Unit) {
        Toast.makeText(
            this,
            getString(R.string.loading_temperature_toast, city),
            Toast.LENGTH_SHORT
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({
            callback.invoke(17)
        }, 5000)
    }

    private suspend fun loadCity(): String {
        delay(5000)
        return "Moscow"
    }

    private suspend fun loadTemperature(city: String): Int {
        Toast.makeText(
            this,
            getString(R.string.loading_temperature_toast, city),
            Toast.LENGTH_SHORT
        ).show()
        delay(5000)
        return 17
    }
}