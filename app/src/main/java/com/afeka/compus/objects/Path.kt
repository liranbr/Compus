package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName
import com.google.gson.*
import java.lang.reflect.*

enum class A11y {
    WALK,
    WHEELCHAIR;

    class Deserializer : JsonDeserializer<A11y> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): A11y {
            val a11yInt = json?.asInt ?: throw JsonParseException("Invalid A11y value")
            return A11y.values()[a11yInt]
        }
    }
}

class Path (
    @SerializedName("time") private val time: Int,
    @SerializedName("a11y") private val a11y: List<A11y>) {
    fun getTime() = time
    fun getA11y() = a11y

    override fun toString(): String {
        val a11yStr = this.a11y.joinToString(", ") { it.name }
        return "Path: time=${this.time}, a11y=[$a11yStr]"
    }
}