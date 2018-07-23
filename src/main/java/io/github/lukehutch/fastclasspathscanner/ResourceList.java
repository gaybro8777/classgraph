/*
 * This file is part of FastClasspathScanner.
 *
 * Author: Luke Hutchison (luke.hutch@gmail.com)
 *
 * Hosted at: https://github.com/lukehutch/fast-classpath-scanner
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Luke Hutchison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.lukehutch.fastclasspathscanner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** An AutoCloseable list of AutoCloseable {@link Resource} objects. */
public class ResourceList extends ArrayList<Resource> implements AutoCloseable {
    public ResourceList() {
        super();
    }

    public ResourceList(final int sizeHint) {
        super(sizeHint);
    }

    public ResourceList(final Collection<Resource> collection) {
        super(collection);
    }

    // -------------------------------------------------------------------------------------------------------------

    /**
     * Returns a list containing the path of each {@link Resource} in this list, relative to the package root within
     * its respective classpath element.
     * 
     * @returns the paths of each {@link Resource} relative to the package root within its respective classpath
     *          element. For example, for a resource path of "BOOT-INF/classes/com/xyz/resource.xml" and a package
     *          root of "BOOT-INF/classes/", returns "com/xyz/resource.xml".
     */
    public List<String> getAllPathsRelativeToPackageRoot() {
        final List<String> resourcePaths = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourcePaths.add(resource.getPathRelativeToPackageRoot());
        }
        return resourcePaths;
    }

    /**
     * Returns a list containing the path of each {@link Resource} in this list, relative to its respective
     * classpath element.
     * 
     * @returns the paths of of each {@link Resource} within its respective classpath element. For example, for a
     *          resource path of "BOOT-INF/classes/com/xyz/resource.xml", returns
     *          "BOOT-INF/classes/com/xyz/resource.xml" (even if the package root is "BOOT-INF/classes/").
     */
    public List<String> getAllPathsRelativeToClasspathElement() {
        final List<String> resourcePaths = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourcePaths.add(resource.getPathRelativeToClasspathElement());
        }
        return resourcePaths;
    }

    /**
     * Returns a list of the URLs of each {@link Resource} in this list.
     * 
     * @returns the paths of of each {@link Resource} within its respective classpath element. For example, for a
     *          resource path of "BOOT-INF/classes/com/xyz/resource.xml", returns
     *          "BOOT-INF/classes/com/xyz/resource.xml" (even if the package root is "BOOT-INF/classes/").
     */
    public List<URL> getAllURLs() {
        final List<URL> resourceURLs = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourceURLs.add(resource.getURL());
        }
        return resourceURLs;
    }

    // -------------------------------------------------------------------------------------------------------------

    /** A {@link FunctionalInterface} for consuming the contents of a {@link Resource} as a byte array. */
    @FunctionalInterface
    public interface ByteArrayConsumer {
        public void accept(final Resource resource, final byte[] byteArray);
    }

    /**
     * Fetch the content of each {@link Resource} in this {@link ResourceList} as a byte array, pass the byte array
     * to the given {@link ByteArrayConsumer}, then close the underlying InputStream or release the underlying
     * ByteBuffer by calling {@link Resource#close()}.
     * 
     * @param byteArrayConsumer
     *            The {@link ByteArrayConsumer}.
     * @param ignoreIOExceptions
     *            if true, any {@link IOException} thrown while trying to load any of the resources will be silently
     *            ignored.
     * @throws IllegalArgumentException
     *             if ignoreExceptions is false, and an {@link IOException} is thrown while trying to load any of
     *             the resources.
     */
    public void forEachByteArray(final ByteArrayConsumer byteArrayConsumer, final boolean ignoreIOExceptions) {
        for (final Resource resource : this) {
            try {
                final byte[] resourceContent = resource.load();
                byteArrayConsumer.accept(resource, resourceContent);
            } catch (final IOException e) {
                if (!ignoreIOExceptions) {
                    throw new IllegalArgumentException("Could not load resource " + this, e);
                }
            } finally {
                resource.close();
            }
        }
    }

    /**
     * Fetch the content of each {@link Resource} in this {@link ResourceList} as a byte array, pass the byte array
     * to the given {@link ByteArrayConsumer}, then close the underlying InputStream or release the underlying
     * ByteBuffer by calling {@link Resource#close()}.
     * 
     * @param byteArrayConsumer
     *            The {@link ByteArrayConsumer}.
     * @throws IllegalArgumentException
     *             if trying to load any of the resources results in an {@link IOException} being thrown.
     */
    public void forEachByteArray(final ByteArrayConsumer byteArrayConsumer) {
        forEachByteArray(byteArrayConsumer, /* ignoreIOExceptions = */ false);
    }

    // -------------------------------------------------------------------------------------------------------------

    /** A {@link FunctionalInterface} for consuming the contents of a {@link Resource} as an {@link InputStream}. */
    @FunctionalInterface
    public interface InputStreamConsumer {
        public void accept(final Resource resource, final InputStream inputStream);
    }

    /**
     * Fetch an {@link InputStream} for each {@link Resource} in this {@link ResourceList}, pass the
     * {@link InputStream} to the given {@link InputStreamConsumer}, then close the {@link InputStream} after the
     * {@link InputStreamConsumer} returns, by calling {@link Resource#close()}.
     * 
     * @param inputStreamConsumer
     *            The {@link InputStreamConsumer}.
     * @param ignoreIOExceptions
     *            if true, any {@link IOException} thrown while trying to load any of the resources will be silently
     *            ignored.
     * @throws IllegalArgumentException
     *             if ignoreExceptions is false, and an {@link IOException} is thrown while trying to open any of
     *             the resources.
     */
    public void forEachInputStream(final InputStreamConsumer inputStreamConsumer,
            final boolean ignoreIOExceptions) {
        for (final Resource resource : this) {
            try {
                final InputStream inputStream = resource.open();
                inputStreamConsumer.accept(resource, inputStream);
            } catch (final IOException e) {
                if (!ignoreIOExceptions) {
                    throw new IllegalArgumentException("Could not load resource " + this, e);
                }
            } finally {
                resource.close();
            }
        }
    }

    /**
     * Fetch an {@link InputStream} for each {@link Resource} in this {@link ResourceList}, pass the
     * {@link InputStream} to the given {@link InputStreamConsumer}, then close the {@link InputStream} after the
     * {@link InputStreamConsumer} returns, by calling {@link Resource#close()}.
     * 
     * @param inputStreamConsumer
     *            The {@link InputStreamConsumer}.
     * @throws IllegalArgumentException
     *             if trying to open any of the resources results in an {@link IOException} being thrown.
     */
    public void forEachInputStream(final InputStreamConsumer inputStreamConsumer) {
        forEachInputStream(inputStreamConsumer, /* ignoreIOExceptions = */ false);
    }

    // -------------------------------------------------------------------------------------------------------------

    /** A {@link FunctionalInterface} for consuming the contents of a {@link Resource} as a {@link ByteBuffer}. */
    @FunctionalInterface
    public interface ByteBufferConsumer {
        public void accept(final Resource resource, final ByteBuffer byteBuffer);
    }

    /**
     * Read each {@link Resource} in this {@link ResourceList} as a {@link ByteBuffer}, pass the {@link ByteBuffer}
     * to the given {@link InputStreamConsumer}, then release the {@link ByteBuffer} after the
     * {@link ByteBufferConsumer} returns, by calling {@link Resource#close()}.
     * 
     * @param byteBufferConsumer
     *            The {@link ByteBufferConsumer}.
     * @param ignoreIOExceptions
     *            if true, any {@link IOException} thrown while trying to load any of the resources will be silently
     *            ignored.
     * @throws IllegalArgumentException
     *             if ignoreExceptions is false, and an {@link IOException} is thrown while trying to load any of
     *             the resources.
     */
    public void forEachByteBuffer(final ByteBufferConsumer byteBufferConsumer, final boolean ignoreIOExceptions) {
        for (final Resource resource : this) {
            try {
                final ByteBuffer byteBuffer = resource.read();
                byteBufferConsumer.accept(resource, byteBuffer);
            } catch (final IOException e) {
                if (!ignoreIOExceptions) {
                    throw new IllegalArgumentException("Could not load resource " + this, e);
                }
            } finally {
                resource.close();
            }
        }
    }

    /**
     * Read each {@link Resource} in this {@link ResourceList} as a {@link ByteBuffer}, pass the {@link ByteBuffer}
     * to the given {@link InputStreamConsumer}, then release the {@link ByteBuffer} after the
     * {@link ByteBufferConsumer} returns, by calling {@link Resource#close()}.
     * 
     * @param byteBufferConsumer
     *            The {@link ByteBufferConsumer}.
     * @throws IllegalArgumentException
     *             if trying to load any of the resources results in an {@link IOException} being thrown.
     */
    public void forEachByteBuffer(final ByteBufferConsumer byteBufferConsumer) {
        forEachByteBuffer(byteBufferConsumer, /* ignoreIOExceptions = */ false);
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        for (int i = 0, n = size(); i < n; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(get(i));
        }
        buf.append(']');
        return buf.toString();
    }

    // -------------------------------------------------------------------------------------------------------------

    /** Close all the {@link Resource} objects in this {@link ResourceList}. */
    @Override
    public void close() {
        for (final Resource resource : this) {
            try {
                resource.close();
            } catch (final Exception e) {
            }
        }
    }
}
