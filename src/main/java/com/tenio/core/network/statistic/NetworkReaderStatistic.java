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
 * This class supports creating an instance for holding the network reading data.
 */
public final class NetworkReaderStatistic {

  private volatile long readBytes;
  private volatile long readPackets;
  private volatile long readDroppedPackets;

  private NetworkReaderStatistic() {
    readBytes = 0L;
    readPackets = 0L;
    readDroppedPackets = 0L;
  }

/**
* Initialization.
*
* @return a new instance of {@link NetworkReaderStatistic}
*/
  public static NetworkReaderStatistic newInstance() {
    return new NetworkReaderStatistic();
  }

/**
* Updates the current number of received bytes from client sides.
*
* @param numberBytes the additional bytes received from client sides in <code>long</code> value 
*/
  public void updateReadBytes(long numberBytes) {
    readBytes += numberBytes;
  }

/**
* Updates the current number of received packets from client sides.
*
* @param numberPackets the additional packets received from client sides in <code>long</code> value 
*/
  public void updateReadPackets(long numberPackets) {
    readPackets += numberPackets;
  }

/**
* Updates the current number of refused packets from client sides which violated the policy.
*
* @param numberPackets the additional packets refused to handle from client sides in <code>long</code> value
* @see 
*/
  public void updateReadDroppedPackets(long numberPackets) {
    readDroppedPackets += numberPackets;
  }

/**
* Retrieves the current number of received bytes data from client sides.
*
* @return the current number of received bytes data in <code>long</code> value
*/
  public long getReadBytes() {
    return readBytes;
  }

/**
* Retrieves the current number of received packets from client sides.
*
* @return the current number of received packets in <code>long</code> value
*/
  public long getReadPackets() {
    return readPackets;
  }

/**
* Retrieves the current number of dropped packets which are refused to handle.
*
* @return the current number of dropped packets in <code>long</code> value
*/
  public long getReadDroppedPackets() {
    return readDroppedPackets;
  }
}
