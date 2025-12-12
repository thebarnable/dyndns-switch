package com.example.dyndnsswitch.data

import androidx.compose.runtime.rememberCoroutineScope
import com.example.dyndnsswitch.model.IonosSubdomain
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
class ProviderRepositoryTest {
    private lateinit var ionosProvider: IonosProvider
    private lateinit var mockWebServer: MockWebServer
    private lateinit var providerRepository: ProviderRepository
    @Before
    fun setup() {
        ShadowLog.stream = System.out

        mockWebServer = MockWebServer()
        mockWebServer.start()

        ionosProvider = IonosProvider(
            zoneID = "testZoneID",
            apiKey = "testAPIKey",
            domain = listOf("test.de"),
            apiURL = mockWebServer.url("/").toString().trimEnd('/')
        )
        providerRepository = ProviderRepository(listOf(ionosProvider))

    }

    @Test
    fun test_resolve_correctlyResolvesExistingDomains() = runTest {
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
                        "content":"12.345.678.902",
                        "changeDate":"2024-07-29T07:18:41.778Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-abcdef-ab4"
                    },
                    {
                        "name":"www.vpn2.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"AAAA",
                        "content":"2a02:123:1234:b234:abcd:1abc:abc1:1235",
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

        providerRepository.updateSubdomains()
        val ipv41 = providerRepository.resolve("www.vpn.thebarnable.de")
        assertEquals("12.345.678.901", ipv41)

        val ipv61 = providerRepository.resolve("www.vpn.thebarnable.de", true)
        assertEquals("2a02:123:1234:b234:abcd:1abc:abc1:1234", ipv61)

        val ipv42 = providerRepository.resolve("www.vpn2.thebarnable.de")
        assertEquals("12.345.678.902", ipv42)

        val ipv62 = providerRepository.resolve("www.vpn2.thebarnable.de", true)
        assertEquals("2a02:123:1234:b234:abcd:1abc:abc1:1235", ipv62)

        val ipv43 = providerRepository.resolve("www.vpn3.thebarnable.de")
        assertEquals("99.999.999.901", ipv43)

        val ipv63 = providerRepository.resolve("www.vpn3.thebarnable.de", true)
        assertEquals("2a03:123:1234:b234:abcd:1abc:abc1:1234", ipv63)
    }

    @Test
    fun test_resolve_throwsErrorsWhenResolvingNonExistingDomains() = runTest {
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
                        "content":"12.345.678.902",
                        "changeDate":"2024-07-29T07:18:41.778Z",
                        "ttl":60,
                        "disabled":false,
                        "id":"12345-abcdef-ab4"
                    },
                    {
                        "name":"www.vpn2.thebarnable.de",
                        "rootName":"thebarnable.de",
                        "type":"AAAA",
                        "content":"2a02:123:1234:b234:abcd:1abc:abc1:1235",
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

        providerRepository.updateSubdomains()
        assertFailsWith<Exception> {providerRepository.resolve("doesnt-exist.de")}
        assertFailsWith<Exception> {providerRepository.resolve("doesnt-exist.de", true)}
    }


    @Test
    fun test_getSubdomainsOfServer_returnsCorrectDomains() = runTest {
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

        providerRepository.updateSubdomains()
        val subdomains = providerRepository.getSubdomainsOfServer("12.345.678.901")
        assertEquals(2, subdomains.size)
        assertEquals(
            IonosSubdomain(
                name="www.vpn.thebarnable.de",
                ipv4="12.345.678.901",
                ipv6="2a02:123:1234:b234:abcd:1abc:abc1:1234"
            ),
            subdomains[0]
        )
        assertEquals(
            IonosSubdomain(
                name="www.vpn2.thebarnable.de",
                ipv4="12.345.678.901",
                ipv6="2a02:123:1234:b234:abcd:1abc:abc1:1234"
            ),
            subdomains[1]
        )

        val subdomainsV6 = providerRepository.getSubdomainsOfServer("2a02:123:1234:b234:abcd:1abc:abc1:1234")
        assertEquals(2, subdomains.size)
        assertEquals(
            IonosSubdomain(
                name="www.vpn.thebarnable.de",
                ipv4="12.345.678.901",
                ipv6="2a02:123:1234:b234:abcd:1abc:abc1:1234"
            ),
            subdomainsV6[0]
        )
        assertEquals(
            IonosSubdomain(
                name="www.vpn2.thebarnable.de",
                ipv4="12.345.678.901",
                ipv6="2a02:123:1234:b234:abcd:1abc:abc1:1234"
            ),
            subdomainsV6[1]
        )

        val subdomainsSingle = providerRepository.getSubdomainsOfServer("99.999.999.901")
        assertEquals(2, subdomains.size)
        assertEquals(
            IonosSubdomain(
                name="www.vpn3.thebarnable.de",
                ipv4="99.999.999.901",
                ipv6="2a03:123:1234:b234:abcd:1abc:abc1:1234"
            ),
            subdomainsSingle[0]
        )

        val subdomainsNone = providerRepository.getSubdomainsOfServer("-1")
        assertEquals(0, subdomainsNone.size)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
}