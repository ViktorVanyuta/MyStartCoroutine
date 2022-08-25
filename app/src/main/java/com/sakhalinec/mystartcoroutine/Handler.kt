package com.sakhalinec.mystartcoroutine

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.sakhalinec.mystartcoroutine.databinding.ActivityMainBinding
import kotlin.concurrent.thread

// Handler - можно создать на главном потоке, затем из любого потока ему можно передавать
// объекты Runnable и тогда метод Runnable.Run() будет вызван на главном потоке

class Handler : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Handler создан на главном потоке
    private val handler = Handler()

    // Handler для приема объекта Message, тоесть если нужен не только вызов метода run(), а делать
    // какие то другие действия в зависимости от переданного сообщения. Наследуемся от Handler-a
    // и переопределяем метод handleMessage()
    private val handlerMsg = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            println("HANDLE_MSG $msg")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.buttonLoad.setOnClickListener {
            loadData()
        }

        // передается сам handler, тип сообщения это константа на которую мы будет как то реагировать
        // и передается какой то объект. Чтобы обрабатывать это сообщение, нужно унаследоваться от Handler-a
        // и переопределить метод handleMessage()
        handlerMsg.sendMessage(Message.obtain(handlerMsg, 0, 55))
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
            // Handler-у передаем объект типа Runnable это можно сделать через лямбду или передать
            // объект анонимного класса, это можно сделать двумя способами через метод post() и тогда
            // метод run() будет вызван мгновенно, либо через метод postDelayed() тогда можно будет
            // установить какую то задержку вызова метода run()
            handler.post {
                // вызов callback-а и передаем необходимый параметр, вызов callback-а будет
                // происходить не в созданном потоке thread{} а будет вызван в потоке Handler-а,
                // Handler был создан на главном потоке, поэтому метод run() тоесть callback выражение
                // будет вызвано на главном потоке
                callback.invoke("Moscow")
            }

        }
    }

    private fun loadTemperature(city: String, callback: (Int) -> Unit) {
        thread {
            handler.post {
                // тут мы работаем в View поэтому нужно все делать в главном потоке! Вся работа
                // с View должна проводиться в главном потоке.
                Toast.makeText(
                    this,
                    getString(R.string.loading_temperature_toast, city),
                    Toast.LENGTH_SHORT
                ).show()
            }
            Thread.sleep(5000)
            handler.post {
                callback.invoke(17)
            }
        }
    }
}

/*=================================== ОБЯЗАТЕЛЬНО ПРОЧИТАЙ!!!!! =================================*/
/*
1. Работать с View элементами можно только из главного потока он же main поток.
2. Чтобы из фоновых потоков взаимодействовать с main потоком используется класс Handler.
3. Handler использует очередь сообщений из класса Looper.
4. При создании объекта Handler ему нужно передать Looper в качестве параметра, если событие нужно
обрабатывать на main потоке то передается mainLooper (Looper.getMainLooper()), если событие нужно
обрабатывать не на главном потоке то перед созданием объекта Handler необходимо вызвать Looper.prepare()
после чего в качестве параметров передать (Looper.myLooper())
5. Handler-у можно передавать сообщения типа Runnable или Message.
6. При передачи объектов типа Runnable нужно вызвать метод post(Runnable) или если сообщение
 должно быть отложенным на какое то время вызвать метод postDelayed(Runnable, long delay)
7. При передачи Message используется метод sandMessage(Message) или sendMessageDelayed(Message, long delay),
чтобы Handler мог обработать объекты типа Message, нужно унаследоваться от Handler и переопределить метод handleMessage()
8. Объекты типа Runnable явно обрабатывать не нужно, у них метод run() будет вызван автоматически.

==================== МИНУСЫ ТАКОГО ПОДХОДА К АСИНХРОННОМУ ПРОГРАММИРОВАНИЮ ========================

1. Callback hell вложеных друг в друга callback-ов может быть очень много. Такой код сложно
читать и дебажить.
2. Работать с View элементами можно только из main потока, поэтому вынуждена использовать Handler
3. Во время работы приложения при каждом перевороте экрана будут создаваться новые потоки для каждого
экрана. Тоесть всегда будет создаваться новый экземпляр, а предыдущий уничтожаться. Но! если высти логи
и покрутить экран то, каждый созданный поток при перевороте экрана не будет завершен моментально, а
будет завершен только когда выполнит свою работу! Это проводит к утечке памяти, потому что сборщик мусора
не может удалить ранее созданные экраны, потому что потоки держат на эти экраны ссылки. Почему потоки не могут
завершиться моментально, у них нет никакого жизненного цикла. Они умрут только когда завершат свою работу.
4. AsyncTask такое себе решение БЫЛО! Гугл их за-deprecated... и рекомендуют юзать корутины!

*/
