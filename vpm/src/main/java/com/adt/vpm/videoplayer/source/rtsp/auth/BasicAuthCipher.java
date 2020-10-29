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
package com.adt.vpm.videoplayer.source.rtsp.auth;

import android.util.Base64;

public final class BasicAuthCipher extends AuthCipher {
    final private String username;
    final private String password;

    final private Credentials credentials;

    BasicAuthCipher(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.credentials = builder.credentials;
    }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getToken() {
        String token = username + ":" + password;
        return new String(Base64.encode(token.getBytes(), Base64.NO_WRAP));
    }

    public static final class Builder implements AuthCipher.Builder {
        String username;
        String password;
        Credentials credentials;

        @Override
        public Builder setUsername(String username) {
            if (username == null) throw new NullPointerException("username == null");

            this.username = username;
            return this;
        }

        @Override
        public Builder setPassword(String password) {
            if (password == null) throw new NullPointerException("password == null");

            this.password = password;
            return this;
        }

        @Override
        public Builder setCredentials(Credentials credentials) {
            if (credentials == null) throw new NullPointerException("credentials == null");

            this.credentials = credentials;
            return this;
        }

        @Override
        public BasicAuthCipher build() {
            if (username == null) throw new IllegalStateException("username is null");
            if (password == null) throw new IllegalStateException("password is null");

            return new BasicAuthCipher(this);
        }
    }
}
