/*
 * Created by ADT author on 9/29/20 10:36 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/28/20 9:48 PM
 */

package com.adt.vpm.videoplayer.source.extractor;

import androidx.annotation.Nullable;

/**
 * An {@link Exception} whose cause is an unsupported format exception.
 */
public class UnsupportedFormatException extends Exception {

    /**
     * Default constructor.
     */
    public UnsupportedFormatException() {
        super();
    }

    /**
     * Optional constructor.
     *
     * @param message The description of the failure.
     */
    public UnsupportedFormatException(String message) {
        super(message);
    }

    /**
     * Optional constructor.
     *
     * @param cause The cause of the failure.
     */
    public UnsupportedFormatException(@Nullable Throwable cause) {
        super(cause);
    }

    /**
     * Full constructor.
     *
     * @param message The description of the failure.
     * @param cause   The cause of the failure.
     */
    private UnsupportedFormatException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

}