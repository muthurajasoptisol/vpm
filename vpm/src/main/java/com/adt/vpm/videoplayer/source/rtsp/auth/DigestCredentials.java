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

import com.adt.vpm.videoplayer.source.rtsp.message.Header;
import com.adt.vpm.videoplayer.source.rtsp.message.Request;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DigestCredentials extends Credentials {
    public static final String REALM = "realm";
    public static final String NONCE = "nonce";
    public static final String CNONCE = "cnonce";
    public static final String QOP = "qop";
    public static final String NC = "nc";
    public static final String OPAQUE = "opaque";
    public static final String RESPONSE = "response";
    public static final String URI = "uri";
    public static final String USERNAME = "username";
    public static final String DOMAIN = "domain";
    public static final String ALGORITHM = "algorithm";
    public static final String STALE = "stale";

    private final String username;
    private final String password;

    private int nonceCount;
    private String lastNonce;

    DigestCredentials(DigestCredentials.Builder builder) {
        super(builder.params);

        username = builder.username;
        password = builder.password;
    }

    public final String getRealm() { return params.get(REALM); }

    public final String getNonce() { return params.get(NONCE); }

    public final String getCnonce() { return params.get(CNONCE); }

    public final String getQop() { return params.get(QOP); }

    public final String getNc() { return params.get(NC); }

    public final String getOpaque() { return params.get(OPAQUE); }

    public final String getUri() { return params.get(URI); }

    public final String getUsername() { return username; }

    public final String getPassword() { return password; }

    public final String getDomain() { return params.get(DOMAIN); }

    public final String getAlgorithm() { return params.get(ALGORITHM); }

    public final String getStale() { return params.get(STALE); }

    public final String getResponse() { return params.get(RESPONSE); }

    private String generate() {
        StringBuilder str = new StringBuilder();
        str.append(AuthScheme.DIGEST).append(' ');

        if (getUsername() != null) {
            str.append(USERNAME).append('=').append('\"').append(getUsername()).append('\"').append(", ");
        }

        str.append(REALM).append('=').append('\"').append(getRealm()).append('\"');
        str.append(", ").append(NONCE).append('=').append('\"').append(getNonce()).append('\"');

        if (getQop() != null) {
            str.append(", ").append(QOP).append('=').append('\"').append(getQop()).append('\"');
        }

        if (getUri() != null) {
            str.append(", ").append(URI).append('=').append('\"').append(getUri()).append('\"');
        }

        if (getNc() != null) {
            str.append(", ").append(NC).append('=').append('\"').append(getNc()).append('\"');
        }

        if (getCnonce() != null) {
            str.append(", ").append(CNONCE).append('=').append('\"').append(getCnonce()).append('\"');
        }

        if (getOpaque() != null) {
            str.append(", ").append(OPAQUE).append('=').append('\"').append(getOpaque()).append('\"');
        }

        if (getDomain() != null) {
            str.append(", ").append(DOMAIN).append('=').append('\"').append(getDomain()).append('\"');
        }

        if (getAlgorithm() != null) {
            str.append(", ").append(ALGORITHM).append('=').append('\"').append(getAlgorithm()).append('\"');
        }

        if (getStale() != null) {
            str.append(", ").append(STALE).append('=').append('\"').append(getStale()).append('\"');
        }

        if (getResponse() != null) {
            str.append(", ").append(RESPONSE).append('=').append('\"').append(getResponse()).append('\"');
        }

        return str.toString();
    }

    private String getNonceCount(String nonce) {
        nonceCount = nonce.equals(lastNonce) ? nonceCount + 1 : 1;
        lastNonce = nonce;

        NumberFormat nf;
        nf = NumberFormat.getIntegerInstance();
        ((DecimalFormat) nf).applyPattern("#00000000");

        return nf.format(nonceCount);
    }

    private int getRandom() {
        return 10 + (int) (Math.random() * ((Integer.MAX_VALUE - 10) + 1));
    }

    private String generateClientNonce(String nonce, String nc) {
        return MD5.hash(nc + nonce + System.currentTimeMillis() + getRandom());
    }

    public final void applyToRequest(Request request) {

        String nc = getNonceCount(getNonce());
        String cNonce = generateClientNonce(getNonce(), nc);

        DigestCredentials.Builder builder = new DigestCredentials.
                Builder(this);

        if ("auth".equals(getQop()) ||
                "auth-int".equals(getQop())) {
            builder.setParam(DigestCredentials.CNONCE, cNonce);
            builder.setParam(DigestCredentials.NC, nc);
        }

        DigestCredentials newCredentials = (DigestCredentials) builder.build();

        DigestAuthCipher digestAuthCipher = new DigestAuthCipher.Builder().
                setCredentials(newCredentials).setUsername(getUsername()).setPassword(getPassword()).
                setMethod(request.getMethod()).setUri(request.getUrl()).
                setBody(request.getMessageBody())
                .build();

        Credentials credentials = new DigestCredentials.
                Builder(newCredentials).
                setParam(DigestCredentials.URI, request.getUrl()).
                setParam(DigestCredentials.USERNAME, getUsername()).
                setParam(DigestCredentials.RESPONSE, digestAuthCipher.getToken()).build();

        this.lastNonce = getNonce();
        this.params = credentials.params;

        request.getHeaders().add(Header.Authorization.toString(),
                generate());
    }

    public static DigestCredentials parse(String credentials) {
        if (credentials == null) throw new NullPointerException("credentials is null");

        DigestCredentials.Builder builder = new DigestCredentials.Builder();

        String[] attrs = credentials.split(",");

        for (String attr : attrs) {
            String[] params = attr.split("=");

            String name = params[0].trim();
            String value = params[1].trim().replaceAll("^\"+|\"+$", "");;

            builder.setParam(name, value);
        }

        return (DigestCredentials) builder.build();
    }

    public static class Builder implements Credentials.Builder {
        private final Map<String, String> params;
        private String username;
        private String password;

        public Builder() {
              params = new LinkedHashMap<>();
        }

        public Builder(String credentials) {
            params = new LinkedHashMap<>();

            String[] attrs = credentials.split(",");

            for (String attr : attrs) {
                String[] params = attr.split("=");

                String name = params[0].trim();
                String value = params[1].trim();

                setParam(name, value.substring(1, value.length()-1));
            }
        }

        Builder(DigestCredentials credentials) {
            if (credentials == null) {
                params = new LinkedHashMap<>();
            }
            else {
                params = credentials.params;
                username = credentials.username;
                password = credentials.password;
            }
        }

        @Override
        public Credentials.Builder setUsername(String username) {
            if (username == null) throw new NullPointerException("username is null");

            this.username = username;
            return this;
        }

        @Override
        public Credentials.Builder setPassword(String password) {
            if (password == null) throw new NullPointerException("password is null");

            this.password = password;
            return this;
        }

        @Override
        public Credentials.Builder setParam(String name, String value) {
            if (name == null) throw new NullPointerException("name is null");
            if (value == null) throw new NullPointerException("value is null");

            if (name.equalsIgnoreCase(REALM) || name.equalsIgnoreCase(NONCE) ||
                    name.equalsIgnoreCase(CNONCE) || name.equalsIgnoreCase(NC) ||
                    name.equalsIgnoreCase(OPAQUE) || name.equalsIgnoreCase(RESPONSE) ||
                    name.equalsIgnoreCase(URI) || name.equalsIgnoreCase(USERNAME) ||
                    name.equalsIgnoreCase(DOMAIN) || name.equalsIgnoreCase(ALGORITHM) ||
                    name.equalsIgnoreCase(STALE)) {
                params.put(name, value);
            }
            else if (name.equalsIgnoreCase(QOP)) {
                if (value.equalsIgnoreCase("auth") || value.equalsIgnoreCase("auth-int")) {
                    params.put(name, value);
                }
                else
                    throw new IllegalStateException("value is invalid");
            }
            else
                throw new IllegalStateException("param is unknown");

            return this;
        }

        @Override
        public Credentials build() {
            if (username == null) throw new NullPointerException("username is null");
            if (password == null) throw new NullPointerException("password is null");
            if (params.get(REALM) == null) throw new NullPointerException("realm is null");
            if (params.get(NONCE) == null) throw new NullPointerException("nonce is null");
            if (params.get(URI) == null) throw new NullPointerException("uri is null");

            return new DigestCredentials(this);
        }
    }
}
