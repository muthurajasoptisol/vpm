/*
 * Created by ADT author on 9/23/20 12:27 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/23/20 12:27 PM
 */

package com.adt.vpm.model

import android.net.Uri
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class UriSerializer : JsonSerializer<Uri?> {

    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.toString())
    }
}