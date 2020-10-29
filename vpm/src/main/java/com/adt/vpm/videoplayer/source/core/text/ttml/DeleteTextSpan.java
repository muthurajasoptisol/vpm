/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.ttml;

import android.text.Spanned;

/**
 * A span used to mark a section of text for later deletion.
 *
 * <p>This is deliberately package-private because it's not generally supported by Android and
 * results in surprising behaviour when simply calling {@link Spanned#toString} (i.e. the text isn't
 * deleted).
 *
 * <p>This span is explicitly handled in {@code TtmlNode#cleanUpText}.
 */
/* package */ final class DeleteTextSpan {}
