package com.example.littledb


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.LittleDatabase
import com.example.littledb.annotations.ColumnName
import com.example.littledb.annotations.LittleEntity
import com.example.littledb.annotations.PrimaryKey
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dao = LittleDatabase.factory(this, "kek_db", 1)

        CoroutineScope(Dispatchers.Main).launch {
            dao.insert(Kek("1234510"))
            dao.insert(Kek("6789010"))

            delay(5000)
            findViewById<TextView>(R.id.txMessage).text = dao.read().toString()
        }
    }
}

@LittleEntity("kek")
data class Kek(
    @PrimaryKey
    @ColumnName("k")
    val k: String
)
