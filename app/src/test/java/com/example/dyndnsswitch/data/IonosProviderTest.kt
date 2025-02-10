package com.example.dyndnsswitch.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Before
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class IonosProviderTest {
    private lateinit var ionosProvider: IonosProvider
    private lateinit var mockWebServer: MockWebServer
    @Before
    fun setup() {
        ShadowLog.stream = System.out

        mockWebServer = MockWebServer()
        mockWebServer.start()

        ionosProvider = IonosProvider(
            zoneID = "testZoneID",
            apiKey = "testAPIKey",
            domain = "test.de",
            apiURL = mockWebServer.url("/").toString().trimEnd('/')
        )
    }

    @Test
    fun test_getSubdomains_returnsCorrectData() = runTest {
        val mockResponse = """
            {
                "name":"thebarnable.de",
                "id":"abcdef-ghij-12345",
                "type":"NATIVE",
                "records":[
                    {
                        "name":"www.vpn.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"A",
                        "content":"12.345.678.901",
                        "changeDate":"2024-07-29T07:18:41.778Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-abcdef-abc"
                    },
                    {
                        "name":"www.vpn.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"AAAA",
                        "content":"2a02:123:1234:b234:abcd:1abc:abc1:1234",
                        "changeDate":"2025-01-30T14:34:04.432Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-1234bgd-ab5"
                    },
                    {
                        "name":"www.vpn2.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"A",
                        "content":"12.345.678.901",
                        "changeDate":"2024-07-29T07:18:41.778Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-abcdef-ab4"
                    },
                    {
                        "name":"www.vpn2.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"AAAA",
                        "content":"2a02:123:1234:b234:abcd:1abc:abc1:1234",
                        "changeDate":"2025-01-30T14:34:04.432Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-1234bgd-ab3"
                    },
                    {
                        "name":"www.vpn3.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"A",
                        "content":"99.999.999.901",
                        "changeDate":"2024-07-29T07:18:41.778Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-abcdef-ab2"
                    },
                    {
                        "name":"www.vpn3.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"AAAA",
                        "content":"2a03:123:1234:b234:abcd:1abc:abc1:1234",
                        "changeDate":"2025-01-30T14:34:04.432Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-1234bgd-ab1"
                    }          
                ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(mockResponse).setResponseCode(200))

        val subdomains = ionosProvider.getSubdomains()
        assertEquals(subdomains.size, 3)
        assertEquals( "www.vpn.thebarnable.de", subdomains[0].name,)
        assertEquals("12.345.678.901", subdomains[0].ipv4,)
        assertEquals("2a02:123:1234:b234:abcd:1abc:abc1:1234", subdomains[0].ipv6,)
        assertEquals("www.vpn2.thebarnable.de", subdomains[1].name)
        assertEquals("12.345.678.901", subdomains[1].ipv4)
        assertEquals("2a02:123:1234:b234:abcd:1abc:abc1:1234", subdomains[1].ipv6)
        assertEquals("www.vpn3.thebarnable.de", subdomains[2].name)
        assertEquals("99.999.999.901", subdomains[2].ipv4)
        assertEquals("2a03:123:1234:b234:abcd:1abc:abc1:1234", subdomains[2].ipv6)
    }

    @Test
    fun test_getSubdomains_throwsExceptionOnHTTPError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        assertFailsWith<Exception> {ionosProvider.getSubdomains()}
    }

    @Test
    fun test_getSubdomains_throwsExceptionWhenHTTPResponseIsEmpty() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        assertFailsWith<Exception> {ionosProvider.getSubdomains()}
    }


    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
}