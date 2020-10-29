/*
 * Created by ADT author on 9/23/20 12:27 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/23/20 12:27 PM
 */

package com.adt.vpm.model

import android.net.Uri
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import kotlin.jvm.Throws

class UriDeserializer : JsonDeserializer<Uri?> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        src: JsonElement, srcType: Type?,
        context: JsonDeserializationContext?
    ): Uri {
        return Uri.parse(src.asString)
    }
}