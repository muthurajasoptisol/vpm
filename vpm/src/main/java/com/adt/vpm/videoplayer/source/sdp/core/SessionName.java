/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adt.vpm.videoplayer.source.sdp.core;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SessionName {
    private static final Pattern regexSDPSession = Pattern.compile("(.+)",
            Pattern.CASE_INSENSITIVE);

    private String name;

    SessionName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public static SessionName parse(String line) {
        try {

            Matcher matcher = regexSDPSession.matcher(line);

            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    return new SessionName(matcher.group(1).trim());
                }
            }

        } catch (Exception ex) {
            // Do nothing
        }

        return null;
    }
}
