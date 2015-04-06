/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.punchthrough.bean.sdk.message;

/**
 * Callbacks emulate lambdas since Java 7 doesn't have lambdas. They act much like
 * {@link java.lang.Runnable}s and are passed into methods that will complete asynchronously. To get
 * results on method completion, override the
 * {@link com.punchthrough.bean.sdk.message.Callback#onResult(Object)} method and use the result
 * passed in as a parameter.
 *
 * @param <T> The type of the callback, passed into
 *            {@link com.punchthrough.bean.sdk.message.Callback#onResult(Object)} as a parameter
 */
public interface Callback<T> {
    /**
     * Override this method to get results back from an asynchronous process.
     *
     * @param result The result passed back from the asynchronous process
     */
    public void onResult(T result);
}
