/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.network.zero.codec.packet;

/**
 * The PacketHeader class represents the metadata and flags associated with a network packet.
 * It encapsulates information about the packet's encoding, compression, size, and encryption status.
 * 
 * <p>Key features:
 * <ul>
 * <li>Binary/text data format indication</li>
 * <li>Compression status tracking</li>
 * <li>Large packet size handling</li>
 * <li>Encryption status indication</li>
 * </ul>
 * 
 * <p>The header is used by the packet encoder/decoder to determine how to process the packet data.
 * Each flag in the header affects the packet processing pipeline:
 * <ul>
 * <li>Binary flag: Determines if data should be processed as binary or text</li>
 * <li>Compression flag: Triggers compression/decompression</li>
 * <li>Big size flag: Enables large packet handling</li>
 * <li>Encryption flag: Enables encryption/decryption</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * var header = PacketHeader.newInstance(true, true, false, true);
 * if (header.isBinary() and header.isCompressed()) {
 *     // Handle compressed binary data
 * }
 * </pre>
 * 
 * @see com.tenio.core.network.zero.codec.encoder.BinaryPacketEncoder
 * @see com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder
 */
public final class PacketHeader {

  private final boolean binary;
  private final boolean compressed;
  private final boolean bigSized;
  private final boolean encrypted;

  private PacketHeader(boolean binary, boolean compressed, boolean bigSized, boolean encrypted) {
    this.binary = binary;
    this.compressed = compressed;
    this.bigSized = bigSized;
    this.encrypted = encrypted;
  }

  /**
   * Creates a new packet header instance with the specified flags.
   * The flags determine how the packet data should be processed:
   * <ul>
   * <li>Binary: Data is in binary format (true) or text format (false)</li>
   * <li>Compressed: Data should be compressed/decompressed</li>
   * <li>Big sized: Data exceeds standard packet size limits</li>
   * <li>Encrypted: Data requires encryption/decryption</li>
   * </ul>
   *
   * @param binary     {@code true} if the data is in binary format, {@code false} for text format
   * @param compressed {@code true} if the data is compressed, {@code false} otherwise
   * @param bigSized   {@code true} if the data size exceeds standard limits, {@code false} otherwise
   * @param encrypted  {@code true} if the data is encrypted, {@code false} otherwise
   * @return a new {@link PacketHeader} instance with the specified flags
   */
  public static PacketHeader newInstance(boolean binary, boolean compressed, boolean bigSized,
                                       boolean encrypted) {
    return new PacketHeader(binary, compressed, bigSized, encrypted);
  }

  /**
   * Checks if the packet data is in binary format.
   * Binary format indicates that the data should be processed as raw bytes rather than text.
   *
   * @return {@code true} if the data is in binary format, {@code false} for text format
   */
  public boolean isBinary() {
    return binary;
  }

  /**
   * Checks if the packet data is compressed.
   * Compressed data requires decompression before it can be processed.
   *
   * @return {@code true} if the data is compressed, {@code false} otherwise
   */
  public boolean isCompressed() {
    return compressed;
  }

  /**
   * Checks if the packet data exceeds standard size limits.
   * Big sized packets may require special handling or fragmentation.
   *
   * @return {@code true} if the data size exceeds standard limits, {@code false} otherwise
   */
  public boolean isBigSized() {
    return bigSized;
  }

  /**
   * Checks if the packet data is encrypted.
   * Encrypted data requires decryption before it can be processed.
   *
   * @return {@code true} if the data is encrypted, {@code false} otherwise
   */
  public boolean isEncrypted() {
    return encrypted;
  }

  @Override
  public String toString() {
    return "PacketHeader{" +
        "binary=" + binary +
        ", compressed=" + compressed +
        ", bigSized=" + bigSized +
        ", encrypted=" + encrypted +
        '}';
  }
}
