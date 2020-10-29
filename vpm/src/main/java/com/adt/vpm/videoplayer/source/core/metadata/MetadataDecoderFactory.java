/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.metadata;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.metadata.MetadataDecoder;
import com.adt.vpm.videoplayer.source.common.metadata.emsg.EventMessageDecoder;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.core.metadata.dvbsi.AppInfoTableDecoder;
import com.adt.vpm.videoplayer.source.core.metadata.icy.IcyDecoder;
import com.adt.vpm.videoplayer.source.core.metadata.scte35.SpliceInfoDecoder;
import com.adt.vpm.videoplayer.source.common.metadata.id3.Id3Decoder;

/**
 * A factory for {@link MetadataDecoder} instances.
 */
public interface MetadataDecoderFactory {

  /**
   * Returns whether the factory is able to instantiate a {@link MetadataDecoder} for the given
   * {@link Format}.
   *
   * @param format The {@link Format}.
   * @return Whether the factory can instantiate a suitable {@link MetadataDecoder}.
   */
  boolean supportsFormat(Format format);

  /**
   * Creates a {@link MetadataDecoder} for the given {@link Format}.
   *
   * @param format The {@link Format}.
   * @return A new {@link MetadataDecoder}.
   * @throws IllegalArgumentException If the {@link Format} is not supported.
   */
  MetadataDecoder createDecoder(Format format);

  /**
   * Default {@link MetadataDecoder} implementation.
   *
   * <p>The formats supported by this factory are:
   *
   * <ul>
   *   <li>ID3 ({@link Id3Decoder})
   *   <li>EMSG ({@link EventMessageDecoder})
   *   <li>SCTE-35 ({@link SpliceInfoDecoder})
   *   <li>ICY ({@link IcyDecoder})
   * </ul>
   */
  MetadataDecoderFactory DEFAULT =
      new MetadataDecoderFactory() {

        @Override
        public boolean supportsFormat(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          return MimeTypes.APPLICATION_ID3.equals(mimeType)
              || MimeTypes.APPLICATION_EMSG.equals(mimeType)
              || MimeTypes.APPLICATION_SCTE35.equals(mimeType)
              || MimeTypes.APPLICATION_ICY.equals(mimeType)
              || MimeTypes.APPLICATION_AIT.equals(mimeType);
        }

        @Override
        public MetadataDecoder createDecoder(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          if (mimeType != null) {
            switch (mimeType) {
              case MimeTypes.APPLICATION_ID3:
                return new Id3Decoder();
              case MimeTypes.APPLICATION_EMSG:
                return new EventMessageDecoder();
              case MimeTypes.APPLICATION_SCTE35:
                return new SpliceInfoDecoder();
              case MimeTypes.APPLICATION_ICY:
                return new IcyDecoder();
              case MimeTypes.APPLICATION_AIT:
                return new AppInfoTableDecoder();
              default:
                break;
            }
          }
          throw new IllegalArgumentException(
              "Attempted to create decoder for unsupported MIME type: " + mimeType);
        }
      };
}
