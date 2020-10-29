/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.core.text.cea.Cea608Decoder;
import com.adt.vpm.videoplayer.source.core.text.cea.Cea708Decoder;
import com.adt.vpm.videoplayer.source.core.text.dvb.DvbDecoder;
import com.adt.vpm.videoplayer.source.core.text.pgs.PgsDecoder;
import com.adt.vpm.videoplayer.source.core.text.ssa.SsaDecoder;
import com.adt.vpm.videoplayer.source.core.text.subrip.SubripDecoder;
import com.adt.vpm.videoplayer.source.core.text.ttml.TtmlDecoder;
import com.adt.vpm.videoplayer.source.core.text.tx3g.Tx3gDecoder;
import com.adt.vpm.videoplayer.source.core.text.webvtt.Mp4WebvttDecoder;
import com.adt.vpm.videoplayer.source.core.text.webvtt.WebvttDecoder;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;

/**
 * A factory for {@link SubtitleDecoder} instances.
 */
public interface SubtitleDecoderFactory {

  /**
   * Returns whether the factory is able to instantiate a {@link SubtitleDecoder} for the given
   * {@link Format}.
   *
   * @param format The {@link Format}.
   * @return Whether the factory can instantiate a suitable {@link SubtitleDecoder}.
   */
  boolean supportsFormat(Format format);

  /**
   * Creates a {@link SubtitleDecoder} for the given {@link Format}.
   *
   * @param format The {@link Format}.
   * @return A new {@link SubtitleDecoder}.
   * @throws IllegalArgumentException If the {@link Format} is not supported.
   */
  SubtitleDecoder createDecoder(Format format);

  /**
   * Default {@link SubtitleDecoderFactory} implementation.
   *
   * <p>The formats supported by this factory are:
   *
   * <ul>
   *   <li>WebVTT ({@link WebvttDecoder})
   *   <li>WebVTT (MP4) ({@link Mp4WebvttDecoder})
   *   <li>TTML ({@link TtmlDecoder})
   *   <li>SubRip ({@link SubripDecoder})
   *   <li>SSA/ASS ({@link SsaDecoder})
   *   <li>TX3G ({@link Tx3gDecoder})
   *   <li>Cea608 ({@link Cea608Decoder})
   *   <li>Cea708 ({@link Cea708Decoder})
   *   <li>DVB ({@link DvbDecoder})
   *   <li>PGS ({@link PgsDecoder})
   * </ul>
   */
  SubtitleDecoderFactory DEFAULT =
      new SubtitleDecoderFactory() {

        @Override
        public boolean supportsFormat(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          return MimeTypes.TEXT_VTT.equals(mimeType)
              || MimeTypes.TEXT_SSA.equals(mimeType)
              || MimeTypes.APPLICATION_TTML.equals(mimeType)
              || MimeTypes.APPLICATION_MP4VTT.equals(mimeType)
              || MimeTypes.APPLICATION_SUBRIP.equals(mimeType)
              || MimeTypes.APPLICATION_TX3G.equals(mimeType)
              || MimeTypes.APPLICATION_CEA608.equals(mimeType)
              || MimeTypes.APPLICATION_MP4CEA608.equals(mimeType)
              || MimeTypes.APPLICATION_CEA708.equals(mimeType)
              || MimeTypes.APPLICATION_DVBSUBS.equals(mimeType)
              || MimeTypes.APPLICATION_PGS.equals(mimeType);
        }

        @Override
        public SubtitleDecoder createDecoder(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          if (mimeType != null) {
            switch (mimeType) {
              case MimeTypes.TEXT_VTT:
                return new WebvttDecoder();
              case MimeTypes.TEXT_SSA:
                return new SsaDecoder(format.initializationData);
              case MimeTypes.APPLICATION_MP4VTT:
                return new Mp4WebvttDecoder();
              case MimeTypes.APPLICATION_TTML:
                return new TtmlDecoder();
              case MimeTypes.APPLICATION_SUBRIP:
                return new SubripDecoder();
              case MimeTypes.APPLICATION_TX3G:
                return new Tx3gDecoder(format.initializationData);
              case MimeTypes.APPLICATION_CEA608:
              case MimeTypes.APPLICATION_MP4CEA608:
                return new Cea608Decoder(
                    mimeType,
                    format.accessibilityChannel,
                    Cea608Decoder.MIN_DATA_CHANNEL_TIMEOUT_MS);
              case MimeTypes.APPLICATION_CEA708:
                return new Cea708Decoder(format.accessibilityChannel, format.initializationData);
              case MimeTypes.APPLICATION_DVBSUBS:
                return new DvbDecoder(format.initializationData);
              case MimeTypes.APPLICATION_PGS:
                return new PgsDecoder();
              default:
                break;
            }
          }
          throw new IllegalArgumentException(
              "Attempted to create decoder for unsupported MIME type: " + mimeType);
        }
      };
}
