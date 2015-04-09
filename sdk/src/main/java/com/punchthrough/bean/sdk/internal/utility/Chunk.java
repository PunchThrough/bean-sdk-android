package com.punchthrough.bean.sdk.internal.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A set of utilities to make it easy for classes to generate discrete "chunks" from byte arrays of
 * arbitrary data.
 */
public class Chunk {

    /**
     * Chunkable classes must provide an array of byte data to be split into chunks.
     */
    public interface Chunkable {
        public byte[] getChunkableData();
    }

    /**
     * Retrieve a number of raw bytes at an offset.
     *
     * @param offset The byte at which to start, zero-indexed
     * @param length The number of bytes to return. If this is greater than the number of bytes
     *               available after <code>offset</code>, it will return all available bytes,
     *               truncated at the end.
     * @return       The bytes, starting at <code>offset</code> of length <code>length</code> or
     *               less if truncated
     */
    public static <T extends Chunkable> byte[] bytesFrom(T chunkable, int offset, int length) {

        byte[] data = chunkable.getChunkableData();

        if ( offset + length > data.length ) {
            // Arrays.copyOfRange appends 0s when the array end is exceeded.
            // Trim length manually to avoid appending extra data.
            return Arrays.copyOfRange(data, offset, data.length);

        } else {
            return Arrays.copyOfRange(data, offset, offset + length);

        }
    }

    /**
     * Retrieve a chunk of raw bytes. Chunks are created by slicing the array at even intervals.
     * The final chunk may be shorter than the other chunks if it's been truncated.
     *
     * @param chunkLength   The length of each chunk
     * @param chunkNum      The chunk at which to start, zero-indexed
     * @return              The chunk (array of bytes)
     */
    public static <T extends Chunkable> byte[] chunkFrom(T chunkable, int chunkLength,
                                                         int chunkNum) {
        int start = chunkNum * chunkLength;
        return bytesFrom(chunkable, start, chunkLength);
    }

    /**
     * Retrieve the count of chunks for a given chunk length.
     *
     * @param chunkLength   The length of each chunk
     * @return              The number of chunks generated for a given chunk length
     */
    public static <T extends Chunkable> int chunkCountFrom(T chunkable, int chunkLength) {
        byte[] data = chunkable.getChunkableData();
        return (int) Math.ceil(data.length * 1.0 / chunkLength);
    }

    /**
     * Retrieve all chunks for a given chunk length.
     * The final chunk may be shorter than the other chunks if it's been truncated.
     *
     * @param chunkLength   The length of each chunk
     * @return              A list of chunks (byte arrays)
     */
    public static <T extends Chunkable> List<byte[]> chunksFrom(T chunkable, int chunkLength) {

        List<byte[]> chunks = new ArrayList<>();

        int chunkCount = chunkCountFrom(chunkable, chunkLength);
        for (int i = 0; i < chunkCount; i++) {
            byte[] chunk = chunkFrom(chunkable, chunkLength, i);
            chunks.add(chunk);
        }

        return chunks;
    }

}
