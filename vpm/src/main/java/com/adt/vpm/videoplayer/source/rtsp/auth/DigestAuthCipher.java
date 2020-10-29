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

import com.adt.vpm.videoplayer.source.rtsp.message.MessageBody;
import com.adt.vpm.videoplayer.source.rtsp.message.Method;

public final class DigestAuthCipher extends AuthCipher {

    final private String uri;
    final private String username;
    final private String password;
    final private Method method;
    final private DigestCredentials credentials;
    final private MessageBody body;

    DigestAuthCipher(Builder builder) {
        this.uri = builder.uri;
        this.body = builder.body;
        this.method = builder.method;
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
        String ha1 = MD5.hash(username + ":" + credentials.getRealm() + ":" + password);

        if ((credentials.getQop() == null)) {
            String ha2 = MD5.hash(method + ":" + uri);
            return MD5.hash(ha1 + ":" + credentials.getNonce() + ":" + ha2);

        } else if ((credentials.getQop().equalsIgnoreCase("auth"))) {
            String ha2 = MD5.hash(method + ":" + uri);
            return MD5.hash(ha1 + ':' + credentials.getNonce() + ':' + credentials.getNc() + ':' +
                    credentials.getCnonce() + ':' + "auth" + ':' + ha2);

        } else if ((credentials.getQop().equalsIgnoreCase("auth-int"))) {
            String ha2 = MD5.hash(method + ":" + uri + ":" + ((body == null) ? "" : body.getContent()));
            return MD5.hash(ha1 + ':' + credentials.getNonce() + ':' + credentials.getNc() + ':' +
                    credentials.getCnonce() + ':' + "auth-int" + ':' + ha2);
        }

        return null;
    }

    public static final class Builder implements AuthCipher.Builder {
        protected String uri;
        protected String username;
        protected String password;
        protected Method method;
        protected DigestCredentials credentials;
        protected MessageBody body;

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

            this.credentials = (DigestCredentials)credentials;
            return this;
        }

        public Builder setUri(String uri) {
            if (uri == null) throw new NullPointerException("uri == null");

            this.uri = uri;
            return this;
        }

        public Builder setMethod(Method method) {
            if (method == null) throw new NullPointerException("method == null");

            this.method = method;
            return this;
        }

        public Builder setBody(MessageBody body) {

            this.body = body;
            return this;
        }

        public DigestAuthCipher build() {
            if (uri == null) throw new IllegalStateException("uri is null");
            if (method == null) throw new IllegalStateException("method == null");
            if (username == null) throw new IllegalStateException("username is null");
            if (password == null) throw new IllegalStateException("password is null");
            if (credentials == null) throw new IllegalStateException("credentials == null");

            return new DigestAuthCipher(this);
        }
    }
}
