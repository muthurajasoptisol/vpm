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
package com.adt.vpm.videoplayer.source.rtsp.core;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import com.adt.vpm.videoplayer.source.rtsp.message.Header;
import com.adt.vpm.videoplayer.source.rtsp.message.InterleavedFrame;
import com.adt.vpm.videoplayer.source.rtsp.message.Message;
import com.adt.vpm.videoplayer.source.rtsp.message.Method;
import com.adt.vpm.videoplayer.source.rtsp.message.Request;
import com.adt.vpm.videoplayer.source.rtsp.message.Response;
import com.adt.vpm.videoplayer.source.rtsp.message.Status;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import static com.adt.vpm.videoplayer.source.rtsp.message.Protocol.RTSP_1_0;

/* package */ final class Dispatcher implements Sender.EventListener, IEventListener  {

    public interface EventListener {
        void onAnnounceRequest(Request request);
        void onRedirectRequest(Request request);
        void onOptionsRequest(Request request);
        void onGetParameterRequest(Request request);
        void onSetParameterRequest(Request request);
        void onAnnounceResponse(Response response);
        void onOptionsResponse(Response response);
        void onDescribeResponse(Response response);
        void onSetupResponse(Response response);
        void onPlayResponse(Response response);
        void onPauseResponse(Response response);
        void onGetParameterResponse(Response response);
        void onRecordResponse(Response response);
        void onSetParameterResponse(Response response);
        void onTeardownResponse(Response response);
        void onEmbeddedBinaryData(InterleavedFrame frame);
        void onUnauthorized(Request request, Response response);
        void onUnSuccess(Request request, Response response);
        void onMalformedResponse(Response response);
        void onNoResponse(Request request);
        void onRequestTimeOut();
        void onIOError();
    }

    static final List<Method> METHODS = Collections.unmodifiableList(Arrays.asList(
            Method.ANNOUNCE, Method.OPTIONS, Method.TEARDOWN));

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {NONE, CONNECTED, FAILURE, DISCONNECTED})
    @interface State {}
    final static int NONE = 0;
    final static int CONNECTED = 1;
    final static int FAILURE = 2;
    final static int DISCONNECTED = 3;

    private final static int DEFAULT_TIMEOUT_MILLIS = 5000;
    private final static int DEFAULT_PORT = 554;

    private Socket socket;

    private Uri uri;
    private String userAgent;

    private IReceiver receiver;
    private Sender sender;

    private final EventListener listener;
    private final RequestMonitor requestMonitor;

    private final Map<Integer, Request> outstanding;
    private final Map<Integer, Request> requests;

    private boolean opened;

    Dispatcher(Builder builder) {
        uri = builder.uri;
        listener = builder.listener;
        userAgent = builder.userAgent;

        outstanding =  Collections.synchronizedMap(new LinkedHashMap<>());
        requests = Collections.synchronizedMap(new LinkedHashMap<>());

        requestMonitor = new RequestMonitor();
    }

    private SocketFactory getSocketFactory(Uri uri) {

        Log.d("RTSP", "getSocketFactory : " + uri);
        SocketFactory factory;

        if(uri.getScheme().equalsIgnoreCase("rtsps")) {
            Log.d("RTSP", "getSocketFactory : RTSPS...creating SSL Factory" );
            try {
                SSLContext context = SSLContext.getDefault();
                Log.d("RTSP", "Getting SSLContext: " + context);
                factory = context.getSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Log.d("RTSP", "getSocketFactory : RTSP..creating default factory" );
            factory = SocketFactory.getDefault();
        }

        return factory;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    void connect() throws IOException{
        if (!opened) {

            SSLSocket sslsocket = null;

            socket = getSocketFactory(uri).createSocket();
            if(uri.getScheme().equalsIgnoreCase("rtsps")) {
                sslsocket = (SSLSocket) socket;
                sslsocket.setUseClientMode(true);
                sslsocket.setEnableSessionCreation(true);

                socket.setKeepAlive(true);
                socket.setSoTimeout(400);
            }

            InetAddress address = InetAddress.getByName(uri.getHost());
            int port = uri.getPort();
            socket.connect(new InetSocketAddress(address, (port > 0) ? port : DEFAULT_PORT),
                    DEFAULT_TIMEOUT_MILLIS); //120*1000;

            if(sslsocket != null) {
                SSLSocket finalSslsocket = sslsocket;
                sslsocket.addHandshakeCompletedListener(event -> {
                    Log.d("RTSP", "handshakeCompleted");

                    SSLSession session = finalSslsocket.getSession();
                    String host = session.getPeerHost();
                    Log.d("RTSP", "Peer Host: " + host);
                    Log.d("RTSP", "Is Session valid: " + session.isValid());

                    opened = true;
                });
                sslsocket.startHandshake();
                sender = new Sender(socket.getOutputStream(), this);
                receiver = new SSLReceiver(socket.getInputStream(), this);
                opened = true;
            } else {

                sender = new Sender(socket.getOutputStream(), this);
                receiver = new Receiver(socket.getInputStream(), this);
                opened = true;
            }

            if(uri.getScheme().equalsIgnoreCase("rtsps")) {

                SSLSession session = sslsocket.getSession();
                Certificate[] certificates = session.getPeerCertificates();
                String host = session.getPeerHost();
                Log.d("RTSP", "Peer Host: " + host);

                Log.d("RTSP", "Peer Port: " + session.getPeerPort());

                Log.d("RTSP", "Peer Port: " + sslsocket.getPort());
                Log.d("RTSP", "Is Session valid: " + session.isValid());
            }
        }
    }

    void close() {
        if (opened) {
            opened = false;

            sender.cancel();
            receiver.cancel();
            requestMonitor.stop();

            requests.clear();
            outstanding.clear();
        }

        closeQuietly();
    }

    private void closeQuietly() {
        try {

            if (socket != null) {
                socket.close();
                socket = null;
            }

        } catch (IOException e) {
            // Ignore.
        }
    }

    void execute(InterleavedFrame interleavedFrame) {
        synchronized (this) {
            sender.send(interleavedFrame);
        }
    }

    void execute(Message message) {
        synchronized (this) {
            if (message.getType() == Message.REQUEST) {
                if (requests.isEmpty()) {
                    sender.send(message);
                }

                Integer cSeq = Integer.parseInt(message.getHeaders().getValue(Header.CSeq));
                requests.put(cSeq, (Request)message);
            }
        }
    }


    // Sender.EventListener implementation
    @Override
    public void onSendSuccess(Message message) {
        if (message.getType() == Message.REQUEST) {
            Integer cSeq = Integer.parseInt(message.getHeaders().getValue(Header.CSeq));
            outstanding.put(cSeq, (Request)message);
            requestMonitor.wait(message);
        }
    }

    @Override
    public void onSendSuccess(InterleavedFrame message) {
        // Do nothing
        Log.e("Dispatcher", "onSendSuccess InterleavedFrame ");
    }

    @Override
    public void onSendFailure(Message message) {
        if (message.getType() == Message.REQUEST) {
            Integer cSeq = Integer.parseInt(message.getHeaders().getValue(Header.CSeq));
            requests.remove(cSeq);
        }
        listener.onIOError();
    }

    @Override
    public void onSendFailure(InterleavedFrame message) {
        listener.onIOError();
    }

    // Receiver.EventListener implementation
    @Override
    public void onReceiveSuccess(Request request) {
        if (RTSP_1_0.equals((request.getProtocol()))) {

            Method method = request.getMethod();

            if (METHODS.contains(method)) {

                if (request.getHeaders().getValue(Header.CSeq) == null) {
                    Response.Builder builder = new Response.Builder().setStatus(Status.BadRequest);
                    builder.setHeader(Header.UserAgent, userAgent);

                    execute(builder.build());

                    Log.e("Dispatcher", "OnReceiveSuccess Request BadRequest");

                } else {

                    String require = request.getHeaders().getValue(Header.Require);

                    if (require == null) {

                        if (method.equals(Method.ANNOUNCE) ||
                                method.equals(Method.GET_PARAMETER) ||
                                method.equals(Method.SET_PARAMETER) ||
                                method.equals(Method.REDIRECT)) {

                            if (request.getHeaders().getValue(Header.Session) == null) {

                                Response.Builder builder = new Response.Builder().
                                    setStatus(Status.SessionNotFound);
                                builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
                                builder.setHeader(Header.UserAgent, userAgent);

                                execute(builder.build());

                            } else {

                                if (method.equals(Method.ANNOUNCE)) {
                                    listener.onAnnounceRequest(request);

                                } else if (method.equals(Method.GET_PARAMETER)) {
                                    listener.onGetParameterRequest(request);

                                } else if (method.equals(Method.SET_PARAMETER)) {
                                    listener.onSetParameterRequest(request);

                                } else {
                                    listener.onRedirectRequest(request);
                                }
                            }

                        } else if (method.equals(Method.OPTIONS)) {
                            listener.onOptionsRequest(request);

                        } else {

                            Response.Builder builder = new Response.Builder().
                                setStatus(Status.MethodNotAllowed);
                            builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
                            builder.setHeader(Header.UserAgent, userAgent);

                            execute(builder.build());
                        }

                    } else {

                        Response.Builder builder = new Response.Builder().setStatus(
                                Status.OptionNotSupported);
                        builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
                        builder.setHeader(Header.UserAgent, userAgent);
                        builder.setHeader(Header.Unsupported, require);

                        execute(builder.build());
                    }
                }

            } else {

                Response.Builder builder = new Response.Builder().setStatus(Status.NotImplemented);
                builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
                builder.setHeader(Header.UserAgent, userAgent);

                execute(builder.build());
            }

        } else {
            Response.Builder builder = new Response.Builder().setStatus(Status.RtspVersionNotSupported);
            builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
            builder.setHeader(Header.UserAgent, userAgent);

            execute(builder.build());
        }
    }

    @Override
    public void onReceiveSuccess(Response response) {
        if (RTSP_1_0.equals(response.getProtocol())) {

            if (response.getHeaders() != null && response.getHeaders().contains(Header.CSeq)) {
                Integer cSeq = Integer.parseInt(response.getHeaders().getValue(Header.CSeq));

                if (outstanding.containsKey(cSeq)) {

                    if (response.isSuccess()) {

                        Request request = outstanding.remove(cSeq);
                        Method method = request.getMethod();

                        requestMonitor.cancel(request);

                        if (method.equals(Method.ANNOUNCE)) {
                            listener.onAnnounceResponse(response);
                        } else if (method.equals(Method.DESCRIBE)) {
                            listener.onDescribeResponse(response);
                        } else if (method.equals(Method.GET_PARAMETER)) {
                            listener.onGetParameterResponse(response);
                        } else if (method.equals(Method.OPTIONS)) {
                            listener.onOptionsResponse(response);
                        } else if (method.equals(Method.PAUSE)) {
                            listener.onPauseResponse(response);
                        } else if (method.equals(Method.PLAY)) {
                            listener.onPlayResponse(response);
                        } else if (method.equals(Method.RECORD)) {
                            listener.onRecordResponse(response);
                        } else if (method.equals(Method.SET_PARAMETER)) {
                            listener.onSetParameterResponse(response);
                        } else if (method.equals(Method.SETUP)) {
                            listener.onSetupResponse(response);
                        } else if (method.equals(Method.TEARDOWN)) {
                            listener.onTeardownResponse(response);
                        }

                    } else {

                        Request request = outstanding.remove(cSeq);
                        requestMonitor.cancel(request);

                        if (response.getStatus().equals(Status.Unauthorized)) {
                            listener.onUnauthorized(request, response);

                        } else {
                            listener.onUnSuccess(request, response);
                        }
                    }

                    dispatchNextRequestInQueue(cSeq);

                } else {

                    if (response.getStatus().equals(Status.RequestTimeOut)) {
                        listener.onRequestTimeOut();

                    } else {

                        Response.Builder builder = new Response.Builder().setStatus(Status.BadRequest);
                        builder.setHeader(Header.UserAgent, userAgent);
                        execute(builder.build());

                        Log.e("Dispatcher", "OnReceiveSuccess Response BadRequest");
                    }
                }
            } else {
                listener.onMalformedResponse(response);
            }

        } else {

            Response.Builder builder = new Response.Builder().setStatus(Status.RtspVersionNotSupported);
            builder.setHeader(Header.CSeq, response.getHeaders().getValue(Header.CSeq));
            builder.setHeader(Header.UserAgent, userAgent);

            execute(builder.build());
        }
    }

    @Override
    public void onReceiveSuccess(InterleavedFrame interleavedFrame) {
        listener.onEmbeddedBinaryData(interleavedFrame);
    }

    @Override
    public void onReceiveFailure(@Receiver.ErrorCode int errorCode) {
        if (errorCode == Receiver.PARSE_ERROR) {
            Response.Builder builder = new Response.Builder().setStatus(Status.BadRequest);
            builder.setHeader(Header.UserAgent, userAgent);

            execute(builder.build());

            Log.e("Dispatcher", "OnReceiveFailure Response BadRequest");

        } else if (errorCode == Receiver.IO_ERROR) {
            listener.onIOError();
        }
    }

    private void dispatchNextRequestInQueue(Integer CSeq) {
        synchronized (this) {
            requests.remove(CSeq);

            if (requests.containsKey(CSeq + 1)) {
                Request request = requests.get(CSeq + 1);
                sender.send(request);
            }
        }
    }

    /**
     * Monitor the request/reply message.
     */
    /* package */ final class RequestMonitor {
        private static final long DEFAULT_TIMEOUT_REQUEST = 10000;

        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private final Map<Integer, Future<?>> tasks;

        RequestMonitor() {
            tasks = Collections.synchronizedMap(new LinkedHashMap<>());
        }

        void cancel(Message message) {
            Integer cSeq = Integer.parseInt(message.getHeaders().getValue(Header.CSeq));
            if (tasks.containsKey(cSeq)) {
                Future<?> task = tasks.remove(cSeq);
                task.cancel(true);
            }
        }

        void stop() {
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
        }

        synchronized void wait(final Message message) {
            final Integer cSeq = Integer.parseInt(message.getHeaders().getValue(Header.CSeq));

            try {

                if (!Thread.currentThread().isInterrupted()) {
                    tasks.put(cSeq, executorService.submit(() -> {
                            try {

                                if (!Thread.currentThread().isInterrupted()) {
                                    Thread.sleep(DEFAULT_TIMEOUT_REQUEST);
                                }

                                if (outstanding.containsKey(cSeq)) {
                                    requests.remove(cSeq);
                                    listener.onNoResponse(outstanding.remove(cSeq));
                                }

                            } catch (InterruptedException ex) {
                            }
                        }
                    ));
                }

            } catch (RejectedExecutionException ex) {
            }
        }
    }

    public static class Builder {
        private Uri uri;
        private String userAgent;
        private EventListener listener;

        Builder(EventListener listener) {
            if (listener == null) throw new NullPointerException("listener == null");

            this.listener = listener;
        }

        public Builder setUri(Uri uri) {
            this.uri = uri;

            return this;
        }

        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;

            return this;
        }

        public Dispatcher build() {
            if (uri == null) throw new IllegalStateException("uri == null");

            return new Dispatcher(this);
        }
    }
}
