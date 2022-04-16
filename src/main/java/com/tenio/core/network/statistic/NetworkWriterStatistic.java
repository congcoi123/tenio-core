/*
The MIT License

Copyright (c) 2016-2021 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.statistic;

/**
 * This class supports creating an instance for holding the network written data.
 */
public final class NetworkWriterStatistic {

  private volatile long writtenBytes;
  private volatile long writtenPackets;
  private volatile long writtenDroppedPacketsByPolicy;
  private volatile long writtenDroppedPacketsByFull;

  private NetworkWriterStatistic() {
    writtenBytes = 0L;
    writtenPackets = 0L;
    writtenDroppedPacketsByPolicy = 0L;
    writtenDroppedPacketsByFull = 0L;
  }

/**
* Initialization.
*
* @return a new instance of {@link NetworkWriterStatistic}
*/
  public static NetworkWriterStatistic newInstance() {
    return new NetworkWriterStatistic();
  }

/**
* Updates the number of sent bytes data to client sides.
*
* @param numberBytes <code>long</code> value, the number of sent bytes data to client sides
*/
  public void updateWrittenBytes(long numberBytes) {
    writtenBytes += numberBytes;
  }

/**
* Updates the number of sent packets to client sides.
*
* @param numberPackets <code>long</code> value, the number of sent packets to client sides
*/
  public void updateWrittenPackets(long numberPackets) {
    writtenPackets += numberPackets;
  }
  
/**
* Updates the number of dropped packets which violated policies and not be able send to client sides.
*
* @param numberPackets <code>long</code> value, the number of dropped packets which violated policies
* @see PacketQueuePolicy
*/
  public void updateWrittenDroppedPacketsByPolicy(long numberPackets) {
    writtenDroppedPacketsByPolicy += numberPackets;
  }

/**
* Updates the number of dropped packets which cannot append to the full queue and not be able send to client sides.
*
* @param numberPackets <code>long</code> value, the number of dropped packets which cannot append to the full queue
* @see PacketQueuePolicy
*/
  public void updateWrittenDroppedPacketsByFull(long numberPackets) {
    writtenDroppedPacketsByFull += numberPackets;
  }

/**
* Retrieves the current number of sending bytes data to client sides.
*
* @return <code>long</code> value, the current number of sending bytes data to client sides
*/
  public long getWrittenBytes() {
    return writtenBytes;
  }

/**
* Retrieves the current number of sending packets to client sides.
*
* @return <code>long</code> value, the current number of sending packets to client sides
*/
  public long getWrittenPackets() {
    return writtenPackets;
  }

/**
* Retrieves the current number of dropped packets which violated policies and not be able send to client sides.
*
* @return <code>long</code> value, the number of dropped packets which violated policies
* @see PacketQueuePolicy
*/
  public long getWrittenDroppedPacketsByPolicy() {
    return writtenDroppedPacketsByPolicy;
  }

/**
* Retrieves the current number of dropped packets which cannot append to the full queue and not be able send to client sides.
*
* @return <code>long</code> value, the number of dropped packets which cannot append to the full queue
* @see PacketQueuePolicy
*/
  public long getWrittenDroppedPacketsByFull() {
    return writtenDroppedPacketsByFull;
  }

/**
* Retrieves the current number of dropped packets which are not able send to client sides.
*
* @return <code>long</code> value, the number of dropped packets which are not able send to client sides
* @see #getWrittenDroppedPacketsByPolicy
* @see #getWrittenDroppedPacketsByFull
*/
  public long getWrittenDroppedPackets() {
    return writtenDroppedPacketsByPolicy + writtenDroppedPacketsByFull;
  }
}
