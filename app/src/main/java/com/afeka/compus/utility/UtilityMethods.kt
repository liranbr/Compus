package com.afeka.compus.utility
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

enum class Direction {
    UP, RIGHT, DOWN, LEFT
}

class UtilityMethods {
    companion object {
        fun switchActivity(context: Context, activity: Class<*>) {
            val intent = Intent(context, activity)
            context.startActivity(intent)
        }

        fun switchActivityWithData(context: Context, activity: Class<*>, vararg data: String) {
            val intent = Intent(context, activity)
            // refactor: can make key attributes optional?
            for (i in data.indices) {
                intent.putExtra("key$i", data[i])
            }
            context.startActivity(intent)
        }

        // Kotlin class version of switchActivityWithData, using KClass
        fun switchActivityWithData(context: Context, activity: Class<*>, data: Map<String, String>) {
            val intent = Intent(context, activity)
            for ((key, value) in data) {
                intent.putExtra(key, value)
            }
            context.startActivity(intent)
        }

        fun closeKeyboard(context: Context, view: View) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}