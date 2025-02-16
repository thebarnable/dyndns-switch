package com.example.dyndnsswitch.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ServerTest {
    private lateinit var server: Server

    private lateinit var mockProcessBuilder: ProcessBuilder
    private lateinit var mockProcess: Process

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        MockitoAnnotations.openMocks(this)

        mockProcessBuilder = mock(ProcessBuilder::class.java)
        mockProcess = mock(Process::class.java)

        server = Server("192.168.0.1", "::1", "TestServer", mockProcessBuilder)
    }

    @Test
    fun test_ping_successful() = runTest {
        val mockInputStream = ByteArrayInputStream("64 bytes from 192.168.0.1: icmp_seq=1 ttl=64 time=1 ms\n 0.0% packet loss".toByteArray())

        `when`(mockProcessBuilder.command("ping", "-c", "4", "192.168.0.1")).thenReturn(mockProcessBuilder)
        `when`(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder)
        `when`(mockProcessBuilder.start()).thenReturn(mockProcess)
        `when`(mockProcess.inputStream).thenReturn(mockInputStream)
        `when`(mockProcess.waitFor()).thenReturn(0)

        val result = server.ping()

        assertTrue(result)
        assertTrue(server.isConnected)
    }

    @Test
    fun test_ping_fails() = runTest {
        val mockInputStream = ByteArrayInputStream("64 bytes from 192.168.0.1: icmp_seq=1 ttl=64 time=1 ms\n 100.0% packet loss".toByteArray())

        `when`(mockProcessBuilder.command("ping", "-c", "4", "192.168.0.1")).thenReturn(mockProcessBuilder)
        `when`(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder)
        `when`(mockProcessBuilder.start()).thenReturn(mockProcess)
        `when`(mockProcess.waitFor()).thenReturn(0)
        `when`(mockProcess.inputStream).thenReturn(mockInputStream)

        val result = server.ping()

        assertFalse(result)
        assertFalse(server.isConnected)
    }

    @Test
    fun test_ping_catchesException() = runTest {
        `when`(mockProcessBuilder.start()).thenThrow(RuntimeException("Process failed"))

        val result = server.ping()

        assertFalse(result)
        assertFalse(server.isConnected)
    }

    @After
    fun tearDown() {

    }
}