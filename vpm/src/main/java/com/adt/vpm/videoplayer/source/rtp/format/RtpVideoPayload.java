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
package com.adt.vpm.videoplayer.source.rtp.format;

import android.util.Base64;
import android.util.Pair;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.CodecSpecificDataUtil;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.NalUnitUtil;
import com.adt.vpm.videoplayer.source.common.util.Util;

import java.util.Collections;
import java.util.List;

public final class RtpVideoPayload extends RtpPayloadFormat {
    private int width;
    private int height;
    private float framerate;
    private final int quality;
    private String codecs;

    private float pixelWidthAspectRatio;
    private List<byte[]> codecSpecificData = null;

    private RtpVideoPayload(Builder builder) {
        super(builder);

        this.width = builder.width;
        this.height = builder.height;
        this.framerate = builder.framerate;
        this.quality = builder.quality;

        pixelWidthAspectRatio = 1.0f;
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public float getFramerate() { return framerate; }

    public int getQuality() { return quality; }

    public String getCodecs() { return codecs; }

    public float getPixelWidthAspectRatio() { return pixelWidthAspectRatio; }

    @Override
    public void buildCodecProfileLevel() {
        if (MimeTypes.VIDEO_H264.equals(sampleMimeType)) {
            if (parameters.contains(FormatSpecificParameter.PROFILE_LEVEL_ID)) {
                //  42:  Baseline
                //  4d:  Main
                //  58:  Extended
                //  64:  High
                //  6e:  High 10
                //  7a:  High 4:2:2
                //  f4:  High 4:4:4
                //  2c:  CAVLC 4:4:4
                codecs = "avc1." + parameters.getValue(FormatSpecificParameter.PROFILE_LEVEL_ID);
            }
        } else if (MimeTypes.VIDEO_MP4V.equals(sampleMimeType)) {
            if (parameters.contains(FormatSpecificParameter.PROFILE_LEVEL_ID)) {
                codecs = "mp4v.20." + parameters.getValue(FormatSpecificParameter.PROFILE_LEVEL_ID);

            } else {
                codecs = "mp4v.20.1";
            }
        } else if (MimeTypes.VIDEO_VP8.equals(sampleMimeType)) {
            codecs = "vp8";
        }
    }

    @Override
    public List<byte[]> buildCodecSpecificData() {
        if (codecSpecificData == null) {
            if (MimeTypes.VIDEO_H264.equals(sampleMimeType)) {
                if (parameters.contains(FormatSpecificParameter.SPROP_PARAMETER_SETS)) {

                    String spropParamSets = parameters.getValue(
                            FormatSpecificParameter.SPROP_PARAMETER_SETS);
                    if (spropParamSets != null && spropParamSets.length() > 0) {
                        codecSpecificData = CodecSpecificDataUtil.
                                buildH264SpecificConfig(spropParamSets);

                        byte[] nalSps = codecSpecificData.get(0);
                        NalUnitUtil.SpsData spsData = NalUnitUtil.parseSpsNalUnit(nalSps,4,
                            nalSps.length);

                        pixelWidthAspectRatio = spsData.pixelWidthAspectRatio;

                        if (width == Format.NO_VALUE && height == Format.NO_VALUE) {
                            width = spsData.width;
                            height = spsData.height;

                        } else if (
                            (width != Format.NO_VALUE && spsData.width != width) ||
                                (height != Format.NO_VALUE && spsData.height != height)) {
                            codecSpecificData.clear();
                            codecSpecificData = null;
                        }
                    }
                }

            } else if (MimeTypes.VIDEO_H265.equals(sampleMimeType)) {
                String vps = parameters.getValue(FormatSpecificParameter.SPROP_VPS);
                String sps = parameters.getValue(FormatSpecificParameter.SPROP_SPS);
                String pps = parameters.getValue(FormatSpecificParameter.SPROP_PPS);

                if (vps != null && sps != null && pps != null) {
                    codecSpecificData = CodecSpecificDataUtil.buildH265SpecificConfig(vps, sps, pps);

                    byte[] spsDec = Base64.decode(sps, Base64.DEFAULT);
                    NalUnitUtil.H265SpsData spsData = NalUnitUtil.parseH265SpsNalUnit(spsDec,0,
                        spsDec.length);

                    pixelWidthAspectRatio = spsData.pixelWidthAspectRatio;

                    if (width == Format.NO_VALUE && height == Format.NO_VALUE) {
                        width = spsData.width;
                        height = spsData.height;

                    } else if (
                        (width != Format.NO_VALUE && spsData.width != width) ||
                            (height != Format.NO_VALUE && spsData.height != height)) {
                        codecSpecificData.clear();
                        codecSpecificData = null;
                    }
                }

            } else if (MimeTypes.VIDEO_MP4V.equals(sampleMimeType)) {
                String config = parameters.getValue(FormatSpecificParameter.CONFIG);
                if (config != null) {

                    try {

                        if (config.length() % 2 == 0) {
                            byte[] csd = Util.getBytesFromHexString(config);
                            codecSpecificData = Collections.singletonList(csd);

                            if (width == Format.NO_VALUE || height == Format.NO_VALUE) {
                                Pair<Integer, Integer> dimensions = CodecSpecificDataUtil.
                                        parseMpeg4VideoSpecificConfig(csd);
                                width = dimensions.first;
                                height = dimensions.second;
                            }
                        }

                    } catch (IllegalArgumentException | ParserException ex) {
                        // Do nothing
                    }
                }
            }
        }

        return codecSpecificData;
    }

    public static final class Builder extends RtpPayloadFormat.Builder {
        int width;
        int height;
        float framerate;
        int quality;

        public Builder() {
            super(VIDEO);
            width = Format.NO_VALUE;
            height = Format.NO_VALUE;
            framerate = Format.NO_VALUE;
        }

        public Builder getWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder getHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder getFramerate(float framerate) {
            this.framerate = framerate;
            return this;
        }

        public Builder getQuality(int quality) {
            this.quality = quality;
            return this;
        }

        @Override
        public RtpPayloadFormat build() {
            return new RtpVideoPayload(this);
        }
    }
}
