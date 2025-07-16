package com.tenio.core.utility;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import org.json.JSONObject;
import java.io.StringWriter;

class HttpUtilityTest {

  @Test
  void testAllEnumValues() {
    for (HttpUtility util : HttpUtility.values()) {
      assertEquals(util.name(), util.toString());
    }
  }

  @Test
  void testHasHeaderKey() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Enumeration<String> headers = Collections.enumeration(Collections.singletonList("X-Test-Header"));
    Mockito.when(request.getHeaderNames()).thenReturn(headers);
    assertTrue(HttpUtility.INSTANCE.hasHeaderKey(request, "X-Test-Header"));
    assertFalse(HttpUtility.INSTANCE.hasHeaderKey(request, "Non-Existent"));
  }

  @Test
  void testGetBodyJson() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getMethod()).thenReturn("POST");
    String json = "{\"key\":\"value\"}";
    BufferedReader reader = new BufferedReader(new StringReader(json));
    Mockito.when(request.getReader()).thenReturn(reader);
    JSONObject obj = HttpUtility.INSTANCE.getBodyJson(request);
    assertEquals("value", obj.getString("key"));
  }

  @Test
  void testGetBodyText() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getMethod()).thenReturn("POST");
    String text = "plain text body";
    BufferedReader reader = new BufferedReader(new StringReader(text));
    Mockito.when(request.getReader()).thenReturn(reader);
    String result = HttpUtility.INSTANCE.getBodyText(request);
    assertEquals(text, result);
  }

  @Test
  void testSendResponseJson() throws Exception {
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    Mockito.when(response.getWriter()).thenReturn(pw);
    HttpUtility.INSTANCE.sendResponseJson(response, 200, "{\"ok\":true}");
    Mockito.verify(response).setContentType(Mockito.anyString());
    Mockito.verify(response).setStatus(200);
    pw.flush();
    assertTrue(sw.toString().contains("ok"));
  }
} 