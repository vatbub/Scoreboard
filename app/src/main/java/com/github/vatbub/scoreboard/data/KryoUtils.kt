package com.github.vatbub.scoreboard.data

import android.content.Context
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.KryoException
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

object KryoUtils {
    private val kryo = prepareKryo()

    private fun prepareKryo(): Kryo {
        val kryo = Kryo()
        kryo.isRegistrationRequired = false
        kryo.register(GameManager::class.java)
        kryo.register(Game::class.java)
        kryo.register(Player::class.java)
        kryo.register(ObservableMutableList::class.java)
        kryo.register(ArrayList::class.java)
        return kryo
    }

    fun saveObject(context: Context, filenameWithoutExtension: String, objectToSave: Any) {
        val output = Output(context.openFileOutput("$filenameWithoutExtension.bin", Context.MODE_PRIVATE))
        kryo.writeObject(output, objectToSave)
        output.close()
    }

    fun <T> readObject(context: Context, fileNameWithoutExtension: String, clazz: Class<T>): T? {
        return try {
            val input = Input(context.openFileInput("$fileNameWithoutExtension.bin"))
            val result = kryo.readObject(input, clazz) as T
            input.close()
            result
        } catch (e: KryoException) {
            e.printStackTrace()
            null
        }
    }
}
