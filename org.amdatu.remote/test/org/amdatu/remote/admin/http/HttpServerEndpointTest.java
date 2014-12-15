/*
 * Copyright (c) 2010-2013 The Amdatu Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.remote.admin.http;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.amdatu.remote.admin.http.TestUtil.ServiceA;
import org.amdatu.remote.admin.http.TestUtil.ServiceAImpl;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Test cases for {@link HttpServerEndpoint}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpServerEndpointTest extends TestCase {

    private BundleContext m_context;
    private ServiceReference<Object> m_serviceRef;
    private HttpServletRequest m_servletRequest;
    private HttpServletResponse m_servletResponse;
    private MockServletOutputStream m_outputStream;

    /**
     * Tests that we can call a method that fails with a checked exception.
     */
    public void testCallMethodWithCheckedExceptionOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"doException()V\",\"a\":[]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doException();

        Throwable t = m_outputStream.getThrowable();
        assertNotNull(t);
        assertEquals(IOException.class, t.getClass());
        assertEquals("Exception!", t.getMessage());
    }

    /**
     * Tests that ....
     */
    public void testCallMethodWithIncorrectArgumentsOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"doNothing()V\",\"a\":[3]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_BAD_REQUEST);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallMethodWithNullResultOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"returnNull()Ljava/lang/Object;\",\"a\":[]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).returnNull();

        m_outputStream.assertContent("{\"r\":null}");
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallMethodWithoutServiceOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"qqq()V\",\"a\":[]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_NOT_FOUND);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Tests that we can call a method successfully and process its result.
     */
    public void testCallMethodWithResultOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"doubleIt(I)I\",\"a\":[3]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(service).doubleIt(eq(3));
        verify(m_servletResponse).setStatus(SC_OK);

        m_outputStream.assertContent("{\"r\":6}");
    }

    /**
     * Tests that we can call a method that fails with a runtime exception.
     */
    public void testCallMethodWithRuntimeExceptionOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"doubleIt(I)I\",\"a\":[0]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doubleIt(eq(0));

        Throwable t = m_outputStream.getThrowable();
        assertNotNull(t);
        assertEquals(IllegalArgumentException.class, t.getClass());
        assertEquals("Invalid value!", t.getMessage());
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallUnknownMethodOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"hashCode()Ljava/lang/String;\",\"a\":[]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_NOT_FOUND);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallVoidMethodOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"doNothing()V\",\"a\":[]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doNothing();

        m_outputStream.assertContent("{}");
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallWithoutMethodOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, "{\"m\":\"()V\",\"a\":[]}");

        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_NOT_FOUND);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Test that listing method signatures is correct
     */
    public void testListMethodSignatures() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        HttpServerEndpoint endpoint = createEndpoint(type);
        endpoint.listMethodSignatures(m_servletRequest, m_servletResponse);

        String content = m_outputStream.getBodyContent();
        assertTrue("start array", content.startsWith("["));
        assertTrue("end array", content.endsWith("]"));
        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String signature = HttpAdminUtil.getMethodSignature(method);
            assertTrue(content.contains(signature));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        m_outputStream = new MockServletOutputStream();

        m_context = mock(BundleContext.class);
        m_serviceRef = mock(ServiceReference.class);
        m_servletRequest = mock(HttpServletRequest.class);
        m_servletResponse = mock(HttpServletResponse.class);
        when(m_servletResponse.getOutputStream()).thenReturn(m_outputStream);
    }

    private HttpServerEndpoint createEndpoint(Class<?>... interfaces) {
        return new HttpServerEndpoint(m_context, m_serviceRef, interfaces);
    }

    private void mockServiceLookup(Object service, String content) throws IOException {
        when(m_servletRequest.getInputStream()).thenReturn(new MockServletInputStream(content));
        when(m_context.getService(eq(m_serviceRef))).thenReturn(service);
    }

    static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream m_bais;

        public MockServletInputStream(String input) {
            m_bais = new ByteArrayInputStream(input.getBytes());
        }

        @Override
        public int read() throws IOException {
            return m_bais.read();
        }
    }

    static class MockServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream m_baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            m_baos.write(b);
        }

        void assertContent(String content) {
            assertEquals(content, getBodyContent());
        }

        String getBodyContent() {
            return new String(m_baos.toByteArray());
        }

        Throwable getThrowable() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tree = mapper.readTree(getBodyContent());
            JsonNode exceptionNode = tree.get("e");
            ExceptionWrapper exception = mapper.readValue(exceptionNode, ExceptionWrapper.class);
            return exception.getException();
        }
    }
}
