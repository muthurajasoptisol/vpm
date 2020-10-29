package com.adt.vpm.videoplayer.source.rtp.format;

import android.util.Pair;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.CodecSpecificDataUtil;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.Util;

import java.util.Collections;
import java.util.List;

public final class RtpAudioPayload extends RtpPayloadFormat {
    private int channels;
    private final long ptime;
    private final long maxptime;

    private int numSubFrames;

    private String codecs;
    private List<byte[]> codecSpecificData = null;

    RtpAudioPayload(Builder builder) {
        super(builder);

        this.channels = builder.channels;
        this.ptime = builder.ptime;
        this.maxptime = builder.maxptime;
    }

    public int getChannels() { return channels; }

    public void setChannels(int channels) { this.channels = channels; }

    public long getPtime() { return ptime; }

    public long getMaxPtime() { return maxptime; }

    public String getCodecs() { return codecs; }

    public int getNumSubFrames() { return numSubFrames; }

    @Override
    public void buildCodecProfileLevel() {
        if (MimeTypes.AUDIO_AAC.equals(getSampleMimeType())) {
            if (parameters.contains(FormatSpecificParameter.PROFILE_LEVEL_ID)) {
                //     1: AAC Main
                //     2: AAC LC (Low Complexity)
                //     3: AAC SSR (Scalable Sample Rate)
                //     4: AAC LTP (Long Term Prediction)
                //     5: SBR (Spectral Band Replication)
                //     6: AAC Scalable
                codecs = "mp4a.40." + parameters.getValue(FormatSpecificParameter.PROFILE_LEVEL_ID);
            }
        } else if (MimeTypes.AUDIO_MP4.equals(getSampleMimeType())) {
            if (parameters.contains(FormatSpecificParameter.PROFILE_LEVEL_ID)) {
                //     1: Main Audio Profile Level 1
                //     9: Speech Audio Profile Level 1
                //    15: High Quality Audio Profile Level 2
                //    30: Natural Audio Profile Level 1
                //    44: High Efficiency AAC Profile Level 2
                //    48: High Efficiency AAC v2 Profile Level 2
                //    55: Baseline MPEG Surround Profile (see ISO/IEC 23003-1) Level 3
                codecs = "mp4a.40." + parameters.getValue(FormatSpecificParameter.PROFILE_LEVEL_ID);

            } else {
                codecs = "mp4a.40.1";
            }
        }
    }

    @Override
    public List<byte[]> buildCodecSpecificData() {
        if (codecSpecificData == null) {
            if (MimeTypes.AUDIO_AAC.equals(getSampleMimeType())) {
                if (channels > 0 && clockrate > 0) {
                    codecSpecificData = Collections.singletonList(
                            CodecSpecificDataUtil.buildAacLcAudioSpecificConfig(clockrate,
                                    channels));
                } else {

                    if (parameters.contains(FormatSpecificParameter.CONFIG)) {
                        byte[] config = parameters.getValue(FormatSpecificParameter.CONFIG).getBytes();

                        try {
                            Pair<Integer, Integer> specConfigParsed = CodecSpecificDataUtil
                                    .parseAacAudioSpecificConfig(config);

                            clockrate = (clockrate > 0) ? clockrate : specConfigParsed.first;
                            channels = (channels > 0) ? channels : specConfigParsed.second;

                            codecSpecificData = Collections.singletonList(CodecSpecificDataUtil
                                    .buildAacLcAudioSpecificConfig(clockrate, channels));

                        } catch (ParserException ex) {
                            codecSpecificData = Collections.singletonList(new byte[0]);
                        }
                    }
                }

            } else if (MimeTypes.AUDIO_MP4.equals(getSampleMimeType())) {
                boolean cpresent = true;
                if (parameters.contains(FormatSpecificParameter.CPRESENT) &&
                        "0".equals(parameters.getValue(FormatSpecificParameter.CPRESENT))) {
                    cpresent = false;
                }

                String config = parameters.getValue(FormatSpecificParameter.CONFIG);
                if (config != null) {
                    try {
                        if (config.length() % 2 == 0) {
                            byte[] smc = Util.getBytesFromHexString(config);
                            Pair<Integer, Pair<Integer, Integer>> cfgOpts = CodecSpecificDataUtil.
                                    parseMpeg4AudioStreamMuxConfig(smc);

                            numSubFrames = cfgOpts.first;
                            clockrate = cfgOpts.second.first;
                            channels = cfgOpts.second.second;

                            codecSpecificData = Collections.singletonList(
                                    CodecSpecificDataUtil.buildAacLcAudioSpecificConfig(
                                            clockrate, channels));
                        }

                    } catch (IllegalArgumentException | ParserException ex) {

                    }

                } else {
                    if (cpresent) {
                        if (channels > 0 && clockrate > 0) {
                            codecSpecificData = Collections.singletonList(
                                    CodecSpecificDataUtil.buildAacLcAudioSpecificConfig(clockrate,
                                            channels));
                        }
                    }
                }

            } else {
                codecSpecificData = Collections.singletonList(new byte[0]);
            }
        }

        return codecSpecificData;
    }

    public static final class Builder extends RtpPayloadFormat.Builder {
        private static final int DEFAULT_NUM_CHANNELS = 1;

        int channels = DEFAULT_NUM_CHANNELS;
        long ptime;
        long maxptime;

        public Builder() {
            super(AUDIO);
            ptime = Format.NO_VALUE;
            maxptime = Format.NO_VALUE;
        }

        public Builder setChannels(int channels) {
            this.channels = channels;
            return this;
        }

        public Builder setPtime(long ptime) {
            this.ptime = ptime;
            return this;
        }

        public Builder setMaxPtime(long maxptime) {
            this.maxptime = ptime;
            return this;
        }

        @Override
        public RtpPayloadFormat build() {
            return new RtpAudioPayload(this);
        }
    }
}