/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.codec.encoder;

import com.tenio.common.data.DataType;
import com.tenio.common.logger.SystemLogger;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.codec.CodecUtility;
import com.tenio.core.network.codec.compression.BinaryPacketCompressor;
import com.tenio.core.network.codec.encryption.BinaryPacketEncryptor;
import com.tenio.core.network.codec.packet.PacketHeader;
import java.nio.ByteBuffer;

/**
 * The default implementation for the binary packet encoding.
 *
 * @see BinaryPacketEncoder
 */
public final class BinaryPacketEncoderImpl extends SystemLogger implements BinaryPacketEncoder {

  private BinaryPacketCompressor compressor;
  private BinaryPacketEncryptor encryptor;
  private int compressionThresholdBytes;

  /**
   * Initialization.
   */
  public BinaryPacketEncoderImpl() {
    compressionThresholdBytes = DEFAULT_COMPRESSION_THRESHOLD_BYTES;
  }

  @Override
  public Packet encode(Packet packet) {
    // retrieve the packet data first
    byte[] binaries = packet.getData();

    // check if the data needs to be encrypted
    boolean isEncrypted = packet.isEncrypted();
    if (isEncrypted) {
      if (encryptor != null) {
        try {
          binaries = encryptor.encrypt(binaries);
        } catch (Exception exception) {
          error(exception);
          isEncrypted = false;
        }
      } else {
        throw new IllegalStateException("Expected the interface BinaryPacketEncryptor was " +
            "implemented, but it is null");
      }
    }

    // check if the data needs to be compressed
    boolean isCompressed = false;
    if (compressionThresholdBytes > 0 && binaries.length >= compressionThresholdBytes) {
      if (compressor != null) {
        try {
          binaries = compressor.compress(binaries);
          isCompressed = true;
        } catch (Exception exception) {
          error(exception);
        }
      } else {
        throw new IllegalStateException("Expected the interface BinaryPacketCompressor was " +
            "implemented due to the packet-compression-threshold-bytes configuration, but it is" +
            " null");
      }
    }

    // if the original size of data exceeded threshold, it needs to be resized the
    // header bytes value
    int headerSize = Short.BYTES;
    if (binaries.length > MAX_BYTES_FOR_NORMAL_SIZE) {
      headerSize = Integer.BYTES;
    }

    // create new packet header and encode the first indicated byte
    var packetHeader =
        PacketHeader.newInstance(true, isCompressed, headerSize > Short.BYTES, isEncrypted,
            packet.getDataType() == DataType.ZERO, packet.getDataType() == DataType.MSG_PACK);
    byte headerByte = CodecUtility.encodeFirstHeaderByte(packetHeader);

    // allocate bytes for the new data and put all value to form a new packet
    var packetBuffer = ByteBuffer.allocate(Byte.BYTES + headerSize + binaries.length);

    // put header byte indicator
    packetBuffer.put(headerByte);

    // put original data size for header bases on its length
    if (headerSize > Short.BYTES) {
      packetBuffer.putInt(binaries.length);
    } else {
      packetBuffer.putShort((short) binaries.length);
    }

    // put original data
    packetBuffer.put(binaries);

    // form new data for the packet
    packet.setData(packetBuffer.array());

    return packet;
  }

  @Override
  public void setCompressor(BinaryPacketCompressor compressor) {
    this.compressor = compressor;
  }

  @Override
  public void setEncryptor(BinaryPacketEncryptor encryptor) {
    this.encryptor = encryptor;
  }

  @Override
  public void setCompressionThresholdBytes(int numberBytes) {
    compressionThresholdBytes = numberBytes;
  }
}
