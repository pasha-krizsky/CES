package com.ces.worker.infra.docker

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled.EMPTY_BUFFER
import io.netty.buffer.Unpooled.wrappedBuffer
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import io.netty.channel.unix.UnixChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH
import io.netty.handler.codec.http.HttpMethod.*
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import kotlinx.coroutines.runBlocking
import io.netty.channel.Channel as NettyChannel
import kotlinx.coroutines.channels.Channel as CoroutineChannel

typealias ResponseHeaders = Map<String, String>
typealias ResponseBody = ByteArray

class Response(
    val status: ResponseStatus,
    val headers: ResponseHeaders,
    val body: ResponseBody
)

fun ResponseBody.asString() = String(this)

class NettySocketClient(socketPath: String) {

    private val responseChannel = CoroutineChannel<Response>()
    private val requestChannel: NettyChannel
    private val epollEventLoopGroup: EpollEventLoopGroup

    private val responseHandler = object : SimpleChannelInboundHandler<HttpObject>() {
        private var status = -1
        private var headers = mutableMapOf<String, String>()
        private var body = ByteArray(0)

        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) = runBlocking {
            if (msg is HttpResponse) {
                status = msg.status().code()
                msg.headers().forEach {
                    headers[it.key] = it.value
                }
            }
            if (msg is HttpContent) {
                val bytes = ByteArray(msg.content().readableBytes())
                msg.content().duplicate().readBytes(bytes)
                body += bytes
            }
            if (msg is LastHttpContent) {
                responseChannel.send(Response(status, headers, body))
                clearState()
            }
        }

        private fun clearState() {
            status = -1
            headers = mutableMapOf()
            body = ByteArray(0)
        }
    }

    init {
        val bootstrap = Bootstrap()
        this.epollEventLoopGroup = EpollEventLoopGroup()

        bootstrap
            .group(epollEventLoopGroup)
            .channel(EpollDomainSocketChannel::class.java)
            .handler(object : ChannelInitializer<UnixChannel>() {
                override fun initChannel(ch: UnixChannel) {
                    ch.pipeline()
                        .addLast(HttpClientCodec())
                        .addLast(responseHandler)
                }
            })

        this.requestChannel = bootstrap.connect(DomainSocketAddress(socketPath)).sync().channel()
    }

    fun close() {
        requestChannel.close().await()
        epollEventLoopGroup.shutdownGracefully()
    }

    suspend fun httpGet(path: String): Response {
        val request: FullHttpRequest = DefaultFullHttpRequest(HTTP_1_1, GET, path, EMPTY_BUFFER)
        request.headers()[HOST] = EMPTY
        requestChannel.writeAndFlush(request)
        return responseChannel.receive()
    }

    suspend fun httpPost(
        path: String,
        headers: Map<String, String> = mapOf(CONTENT_TYPE to APPLICATION_JSON),
        body: String = EMPTY,
    ): Response {

        val request: FullHttpRequest = DefaultFullHttpRequest(
            HTTP_1_1, POST, path, wrappedBuffer(body.toByteArray())
        )
        request.headers()[HOST] = EMPTY
        headers.forEach {
            request.headers()[it.key] = it.value
        }
        request.headers()[CONTENT_LENGTH] = body.toByteArray().size
        requestChannel.writeAndFlush(request)
        return responseChannel.receive()
    }

    suspend fun httpPut(
        path: String,
        body: ByteArray,
        headers: Map<String, String> = mapOf(CONTENT_TYPE to APPLICATION_JSON),
    ): Response {

        val request: FullHttpRequest = DefaultFullHttpRequest(HTTP_1_1, PUT, path, wrappedBuffer(body))
        request.headers()[HOST] = EMPTY
        headers.forEach {
            request.headers()[it.key] = it.value
        }
        request.headers()[CONTENT_LENGTH] = body.size
        requestChannel.writeAndFlush(request)
        return responseChannel.receive()
    }

    suspend fun httpDelete(path: String): Response {
        val request: FullHttpRequest = DefaultFullHttpRequest(HTTP_1_1, DELETE, path, EMPTY_BUFFER)
        request.headers()[HOST] = EMPTY
        requestChannel.writeAndFlush(request)
        return responseChannel.receive()
    }

    companion object {
        const val EMPTY = ""
        const val HOST = "host"
        const val CONTENT_TYPE = "content-type"
        const val APPLICATION_JSON = "application/json"
    }
}