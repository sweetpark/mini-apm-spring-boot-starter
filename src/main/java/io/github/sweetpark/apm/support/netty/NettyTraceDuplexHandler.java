package io.github.sweetpark.apm.support.netty;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.CommonUtil;
import io.github.sweetpark.apm.core.support.util.SamplingDecider;
import io.github.sweetpark.apm.core.support.util.TraceIdUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.springframework.lang.NonNull;

/** Netty TCP 환경에서 요청/응답 패킷 및 SQL 실행 정보를 추적하는 듀플렉스 핸들러입니다. */
@ChannelHandler.Sharable
public class NettyTraceDuplexHandler extends ChannelDuplexHandler {

  private final NettyLogProcessor logProcessor;
  private final ApmProperties properties;

  private static final AttributeKey<String> REAL_IP_KEY = AttributeKey.valueOf("APM_REAL_IP");
  private static final AttributeKey<Long> START_NANO_KEY = AttributeKey.valueOf("APM_START_NANO");
  private static final AttributeKey<String> TRACE_ID_KEY = AttributeKey.valueOf("APM_TRACE_ID");
  private static final AttributeKey<String> SPAN_ID_KEY = AttributeKey.valueOf("APM_SPAN_ID");
  private static final AttributeKey<String> REQUEST_DATA_KEY =
      AttributeKey.valueOf("APM_REQUEST_DATA");
  private static final AttributeKey<String> RESPONSE_DATA_KEY =
      AttributeKey.valueOf("APM_RESPONSE_DATA");
  private static final AttributeKey<SqlTraceContext> SQL_CONTEXT_KEY =
      AttributeKey.valueOf("APM_SQL_CONTEXT");
  private static final AttributeKey<Exception> ERROR_CONTEXT_KEY =
      AttributeKey.valueOf("APM_ERROR_CONTEXT");
  private static final AttributeKey<Boolean> FORCE_TRACE_KEY =
      AttributeKey.valueOf("APM_FORCE_TRACE");
  private static final AttributeKey<Integer> LAST_INBOUND_ID =
      AttributeKey.valueOf("APM_LAST_INBOUND_ID");
  private static final AttributeKey<Integer> LAST_OUTBOUND_ID =
      AttributeKey.valueOf("APM_LAST_OUTBOUND_ID");

  public NettyTraceDuplexHandler(ApmProperties properties) {
    this.properties = properties;
    this.logProcessor = new NettyLogProcessor(properties);
  }

  @Override
  public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg)
      throws Exception {
    String traceId = ctx.channel().attr(TRACE_ID_KEY).get();
    String spanId = ctx.channel().attr(SPAN_ID_KEY).get();
    Boolean forceTrace = ctx.channel().attr(FORCE_TRACE_KEY).get();

    if (traceId == null) {
      traceId = TraceIdUtil.generateTraceId();
      spanId = TraceIdUtil.generateSpanId();
      forceTrace = SamplingDecider.shouldForceTrace(properties, false);

      ctx.channel().attr(TRACE_ID_KEY).set(traceId);
      ctx.channel().attr(SPAN_ID_KEY).set(spanId);
      ctx.channel().attr(FORCE_TRACE_KEY).set(forceTrace);
      ctx.channel().attr(START_NANO_KEY).set(System.nanoTime());
      ctx.channel().attr(REQUEST_DATA_KEY).set("");
      ctx.channel().attr(RESPONSE_DATA_KEY).set("");

      SqlTraceContext sqlCtx = SqlTraceContextHolder.init();
      ctx.channel().attr(SQL_CONTEXT_KEY).set(sqlCtx);
    }

    TraceContextHolder.init(
        traceId, spanId, properties.getTrace().getLevel(), Boolean.TRUE.equals(forceTrace));
    SqlTraceContextHolder.set(ctx.channel().attr(SQL_CONTEXT_KEY).get());

    int msgId = System.identityHashCode(msg);
    Integer lastInId = ctx.channel().attr(LAST_INBOUND_ID).get();
    if (lastInId == null || lastInId != msgId) {
      ctx.channel().attr(LAST_INBOUND_ID).set(msgId);
      String currentReq = ctx.channel().attr(REQUEST_DATA_KEY).get();
      ctx.channel().attr(REQUEST_DATA_KEY).set(accumulate(currentReq, safeToString(msg)));
    }

    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    int msgId = System.identityHashCode(msg);
    Integer lastOutId = ctx.channel().attr(LAST_OUTBOUND_ID).get();
    if (lastOutId == null || lastOutId != msgId) {
      ctx.channel().attr(LAST_OUTBOUND_ID).set(msgId);
      String data = safeToString(msg);
      String currentRes = ctx.channel().attr(RESPONSE_DATA_KEY).get();
      ctx.channel().attr(RESPONSE_DATA_KEY).set(accumulate(currentRes, data));
    }

    promise.addListener(
        future -> {
          try {
            if (future.isSuccess()) {
              logNetty(ctx, null);
            } else {
              logNetty(
                  ctx,
                  future.cause() instanceof Exception e ? e : new RuntimeException(future.cause()));
            }
          } finally {
            clearContext(ctx);
          }
        });

    try {
      super.write(ctx, msg, promise);
    } catch (Exception ex) {
      logNetty(ctx, ex);
      clearContext(ctx);
      throw ex;
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    try {
      logNetty(ctx, null);
      clearContext(ctx);
    } finally {
      super.channelInactive(ctx);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    try {
      Exception e = cause instanceof Exception ex ? ex : new RuntimeException(cause);
      ctx.channel().attr(ERROR_CONTEXT_KEY).set(e);
      logNetty(ctx, e);
      clearContext(ctx);
    } finally {
      super.exceptionCaught(ctx, cause);
    }
  }

  private void logNetty(ChannelHandlerContext ctx, Exception ex) {
    String traceId = ctx.channel().attr(TRACE_ID_KEY).get();
    if (traceId == null) return;

    if (ex == null) {
      ex = ctx.channel().attr(ERROR_CONTEXT_KEY).get();
    }
    if (ex == null && ctx.channel().attr(ERROR_CONTEXT_KEY).get() != null) {
      return;
    }

    ctx.channel().attr(TRACE_ID_KEY).set(null);
    String spanId = ctx.channel().attr(SPAN_ID_KEY).get();

    Long startNano = ctx.channel().attr(START_NANO_KEY).get();
    double elapsedMs = (startNano != null) ? (System.nanoTime() - startNano) / 1_000_000.0 : 0;

    String clientIp = ctx.channel().attr(REAL_IP_KEY).get();
    if (clientIp == null && ctx.channel().remoteAddress() != null) {
      clientIp = ctx.channel().remoteAddress().toString();
    }

    String reqData = ctx.channel().attr(REQUEST_DATA_KEY).get();
    String resData = ctx.channel().attr(RESPONSE_DATA_KEY).get();
    SqlTraceContext sqlCtx = ctx.channel().attr(SQL_CONTEXT_KEY).get();
    Boolean forceTrace = ctx.channel().attr(FORCE_TRACE_KEY).get();

    if (sqlCtx != null) {
      SqlTraceContextHolder.set(sqlCtx);
    }

    TraceContextHolder.init(
        traceId, spanId, properties.getTrace().getLevel(), Boolean.TRUE.equals(forceTrace));

    LogNettyContext.Builder builder =
        new LogNettyContext.Builder()
            .traceId(traceId)
            .spanId(spanId)
            .interfaceId("NETTY_TCP")
            .clientIp(clientIp)
            .method("TCP")
            .status(ex == null ? "OK" : "ERROR")
            .elapsedMs(elapsedMs)
            .requestData(reqData != null ? reqData : "")
            .responseData(resData != null ? resData : "")
            .ex(ex);

    if (sqlCtx != null) {
      builder.sqlCount(sqlCtx.count()).sqlTotalElapsed(sqlCtx.getTotalElapsed());
    }

    logProcessor.process(builder.build());
  }

  private void clearContext(ChannelHandlerContext ctx) {
    ctx.channel().attr(START_NANO_KEY).set(null);
    ctx.channel().attr(TRACE_ID_KEY).set(null);
    ctx.channel().attr(SPAN_ID_KEY).set(null);
    ctx.channel().attr(REQUEST_DATA_KEY).set(null);
    ctx.channel().attr(RESPONSE_DATA_KEY).set(null);
    ctx.channel().attr(SQL_CONTEXT_KEY).set(null);
    ctx.channel().attr(ERROR_CONTEXT_KEY).set(null);
    ctx.channel().attr(FORCE_TRACE_KEY).set(null);
    ctx.channel().attr(LAST_INBOUND_ID).set(null);
    ctx.channel().attr(LAST_OUTBOUND_ID).set(null);

    SqlTraceContextHolder.clear();
    TraceContextHolder.clear();
  }

  private String accumulate(String current, String addition) {
    if (addition == null || addition.isEmpty()) return current;
    if (current == null) current = "";
    int max = properties.getLimit().getMaxBodyLength();
    if (current.length() >= max) return current;
    return CommonUtil.truncate(current + addition, max);
  }

  private String safeToString(Object msg) {
    if (msg == null) return "";
    if (msg instanceof String s) return s;
    if (msg instanceof io.netty.buffer.ByteBuf buf) {
      int max = properties.getLimit().getMaxBodyLength();
      int readableBytes = buf.readableBytes();
      if (readableBytes == 0) return "";
      int lengthToRead = Math.min(readableBytes, max);
      String result =
          buf.toString(buf.readerIndex(), lengthToRead, java.nio.charset.StandardCharsets.UTF_8);
      return readableBytes > max ? result + "...(TRUNCATED)" : result;
    }
    return msg.toString();
  }
}
