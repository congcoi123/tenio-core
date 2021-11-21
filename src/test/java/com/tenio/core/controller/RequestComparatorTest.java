package com.tenio.core.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenio.core.network.define.RequestPriority;
import com.tenio.core.network.entity.protocol.Request;
import org.junit.jupiter.api.Test;

class RequestComparatorTest {
  @Test
  void testNewInstance() {
    RequestComparator actualNewInstanceResult = RequestComparator.newInstance();
    Request request = mock(Request.class);
    when(request.getTimestamp()).thenReturn(10L);
    when(request.getPriority()).thenReturn(RequestPriority.LOWEST);
    Request request1 = mock(Request.class);
    when(request1.getTimestamp()).thenReturn(10L);
    when(request1.getPriority()).thenReturn(RequestPriority.LOWEST);
    int actualCompareResult = actualNewInstanceResult.compare(request, request1);
    verify(request, atLeast(1)).getPriority();
    verify(request, atLeast(1)).getTimestamp();
    verify(request1, atLeast(1)).getPriority();
    verify(request1, atLeast(1)).getTimestamp();
    assertEquals(0, actualCompareResult);
  }

  @Test
  void testNewInstance2() {
    RequestComparator actualNewInstanceResult = RequestComparator.newInstance();
    Request request = mock(Request.class);
    when(request.getTimestamp()).thenReturn(0L);
    when(request.getPriority()).thenReturn(RequestPriority.LOWEST);
    Request request1 = mock(Request.class);
    when(request1.getTimestamp()).thenReturn(10L);
    when(request1.getPriority()).thenReturn(RequestPriority.LOWEST);
    int actualCompareResult = actualNewInstanceResult.compare(request, request1);
    verify(request, atLeast(1)).getPriority();
    verify(request).getTimestamp();
    verify(request1, atLeast(1)).getPriority();
    verify(request1).getTimestamp();
    assertEquals(-1, actualCompareResult);
  }

  @Test
  void testNewInstance3() {
    RequestComparator actualNewInstanceResult = RequestComparator.newInstance();
    Request request = mock(Request.class);
    when(request.getTimestamp()).thenReturn(Long.MAX_VALUE);
    when(request.getPriority()).thenReturn(RequestPriority.LOWEST);
    Request request1 = mock(Request.class);
    when(request1.getTimestamp()).thenReturn(10L);
    when(request1.getPriority()).thenReturn(RequestPriority.LOWEST);
    int actualCompareResult = actualNewInstanceResult.compare(request, request1);
    verify(request, atLeast(1)).getPriority();
    verify(request, atLeast(1)).getTimestamp();
    verify(request1, atLeast(1)).getPriority();
    verify(request1, atLeast(1)).getTimestamp();
    assertEquals(1, actualCompareResult);
  }

  @Test
  void testNewInstance4() {
    RequestComparator actualNewInstanceResult = RequestComparator.newInstance();
    Request request = mock(Request.class);
    when(request.getTimestamp()).thenReturn(10L);
    when(request.getPriority()).thenReturn(RequestPriority.LOW);
    Request request1 = mock(Request.class);
    when(request1.getTimestamp()).thenReturn(10L);
    when(request1.getPriority()).thenReturn(RequestPriority.LOWEST);
    int actualCompareResult = actualNewInstanceResult.compare(request, request1);
    verify(request, atLeast(1)).getPriority();
    verify(request1, atLeast(1)).getPriority();
    assertEquals(1, actualCompareResult);
  }

  @Test
  void testNewInstance5() {
    RequestComparator actualNewInstanceResult = RequestComparator.newInstance();
    Request request = mock(Request.class);
    when(request.getTimestamp()).thenReturn(10L);
    when(request.getPriority()).thenReturn(RequestPriority.LOWEST);
    Request request1 = mock(Request.class);
    when(request1.getTimestamp()).thenReturn(10L);
    when(request1.getPriority()).thenReturn(RequestPriority.LOW);
    int actualCompareResult = actualNewInstanceResult.compare(request, request1);
    verify(request).getPriority();
    verify(request1).getPriority();
    assertEquals(-1, actualCompareResult);
  }
}

