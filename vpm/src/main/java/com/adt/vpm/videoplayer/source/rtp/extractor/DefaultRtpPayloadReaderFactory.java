/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.adt.vpm.videoplayer.source.rtp.extractor;


import androidx.annotation.NonNull;

import com.adt.vpm.videoplayer.source.rtp.format.RtpAudioPayload;
import com.adt.vpm.videoplayer.source.rtp.format.RtpPayloadFormat;
import com.adt.vpm.videoplayer.source.rtp.format.RtpVideoPayload;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;

/**
 * Default {@link RtpPayloadReader.Factory} implementation.
 */
/*package*/ final class DefaultRtpPayloadReaderFactory implements RtpPayloadReader.Factory {

  @NonNull
  @Override
  public RtpPayloadReader createPayloadReader(RtpPayloadFormat format) {
    if (MimeTypes.VIDEO_H264.equals(format.getSampleMimeType())) {
      return new RtpH264PayloadReader((RtpVideoPayload) format);
    } else if (MimeTypes.VIDEO_H265.equals(format.getSampleMimeType())) {
      return new RtpH265PayloadReader((RtpVideoPayload)format);
    } else if (MimeTypes.VIDEO_MP4V.equals(format.getSampleMimeType())) {
      return new RtpMp4vPayloadReader((RtpVideoPayload)format);
    } else if (MimeTypes.VIDEO_VP8.equals(format.getSampleMimeType())) {
      return new RtpVp8PayloadReader((RtpVideoPayload)format);
    } else if (MimeTypes.VIDEO_VP9.equals(format.getSampleMimeType())) {
      return new RtpVp9PayloadReader((RtpVideoPayload)format);
    } else if (MimeTypes.VIDEO_MJPEG.equals(format.getSampleMimeType())) {
      return new RtpJpegPayloadReader((RtpVideoPayload)format);
    } else if (MimeTypes.AUDIO_ALAW.equals(format.getSampleMimeType()) ||
            MimeTypes.AUDIO_MLAW.equals(format.getSampleMimeType())) {
      return new RtpG711PayloadReader((RtpAudioPayload)format);
    } else if (MimeTypes.AUDIO_AC3.equals(format.getSampleMimeType())) {
      return new RtpAc3PayloadReader((RtpAudioPayload)format);
    } else if (MimeTypes.AUDIO_AAC.equals(format.getSampleMimeType())) {
      return new RtpAacPayloadReader((RtpAudioPayload)format);
    } else if (MimeTypes.AUDIO_MP4.equals(format.getSampleMimeType())) {
      return new RtpMp4aPayloadReader((RtpAudioPayload)format);
    }

    return null;
  }

}
