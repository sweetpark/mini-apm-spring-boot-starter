package io.github.sweetpark.apm.support.servlet;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.error.ErrorEvaluator;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.SamplingDecider;
import io.github.sweetpark.apm.core.support.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/** HTTP 요청 및 응답, SQL 추적 정보를 수집하고 Fail-safe하게 통합 로깅하는 서블릿 필터입니다. */
public class LoggingFilter extends OncePerRequestFilter {

  private final ApmProperties properties;
  private final ServletLogProcessor logProcessor;

  public LoggingFilter(ApmProperties properties, ErrorEvaluator errorEvaluator) {
    this.properties = properties;
    this.logProcessor = new ServletLogProcessor(properties, errorEvaluator);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    TraceLevel level = properties.getTrace().getLevel();

    // 1. Trace ID / Span ID 추출 및 생성 (W3C traceparent 지원)
    String traceHeaderName = properties.getTrace().getHeaderName();
    String traceparent = request.getHeader("traceparent");
    String traceId;
    String spanId = TraceIdUtil.generateSpanId();

    if (traceparent != null
        && traceparent.startsWith("00-")
        && traceparent.split("-").length >= 3) {
      String[] parts = traceparent.split("-");
      traceId = parts[1];
    } else if (traceHeaderName != null && request.getHeader(traceHeaderName) != null) {
      traceId = request.getHeader(traceHeaderName);
    } else {
      traceId = TraceIdUtil.generateTraceId();
    }

    MDC.put("traceId", traceId);
    MDC.put("spanId", spanId);

    // 2. 강제 추적 및 샘플링 결정
    boolean forceTrace =
        "true".equalsIgnoreCase(request.getHeader("X-Debug-Trace"))
            || "true".equalsIgnoreCase(request.getParameter("trace"));
    forceTrace = SamplingDecider.shouldForceTrace(properties, forceTrace);

    TraceContextHolder.init(traceId, spanId, level, forceTrace);
    SqlTraceContextHolder.init();

    boolean binaryRequest = isBinaryRequest(request);
    long startNano = System.nanoTime();
    Exception exception = null;
    RequestWrapper req = null;
    ResponseWrapper res = null;

    try {
      if (!binaryRequest) {
        req = new RequestWrapper(request);
        res = new ResponseWrapper(response);
        filterChain.doFilter(req, res);
      } else {
        filterChain.doFilter(request, response);
      }
    } catch (Exception e) {
      exception = e;
      throw e;
    } finally {
      long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;

      if (res != null) {
        try {
          res.copyBodyToResponse();
        } catch (Exception ignored) {
        }
      }

      logUnified(request, req, res, elapsedMs, exception);

      SqlTraceContextHolder.clear();
      TraceContextHolder.clear();
      MDC.remove("traceId");
      MDC.remove("spanId");
    }
  }

  private void logUnified(
      HttpServletRequest originalReq,
      RequestWrapper wrappedReq,
      ResponseWrapper wrappedRes,
      long elapsedMs,
      Exception ex) {
    logProcessor.process(buildApiContext(originalReq, wrappedReq, wrappedRes, elapsedMs, ex));
  }

  private LogApiContext buildApiContext(
      HttpServletRequest originalReq,
      RequestWrapper wrappedReq,
      ResponseWrapper wrappedRes,
      long elapsedMs,
      Exception ex) {
    String responseBody =
        (wrappedRes != null
                && !isBinaryResponse(wrappedRes)
                && isTextContent(wrappedRes.getContentType()))
            ? wrappedRes.getBodyAsString()
            : "[BINARY DATA/EMPTY]";
    String status = (wrappedRes != null) ? String.valueOf(wrappedRes.getStatus()) : "-";

    String uri = (wrappedReq != null) ? wrappedReq.getRequestURI() : originalReq.getRequestURI();
    String method = (wrappedReq != null) ? wrappedReq.getMethod() : originalReq.getMethod();

    String ifHeader = properties.getTrace().getInterfaceHeaderName();
    String interfaceId =
        (wrappedReq != null) ? wrappedReq.getHeader(ifHeader) : originalReq.getHeader(ifHeader);
    if (interfaceId == null) {
      interfaceId =
          (wrappedReq != null) ? wrappedReq.getHeader("IFID") : originalReq.getHeader("IFID");
    }

    String requestParam =
        (wrappedReq != null)
            ? wrappedReq.getParameterMap().toString()
            : originalReq.getParameterMap().toString();
    String requestBody = (wrappedReq != null) ? wrappedReq.getBody() : "[BINARY DATA/NOT WRAPPED]";

    return new LogApiContext.Builder()
        .traceId(MDC.get("traceId"))
        .spanId(MDC.get("spanId"))
        .interfaceId(interfaceId)
        .uri(uri)
        .method(method)
        .status(status)
        .elapsedMs(elapsedMs)
        .requestParam(requestParam)
        .requestBody(requestBody)
        .responseBody(responseBody)
        .ex(ex)
        .build();
  }

  private boolean isBinaryRequest(HttpServletRequest req) {
    String accept = req.getHeader("Accept");
    if (accept != null && accept.contains("application/octet-stream")) {
      return true;
    }
    String contentType = req.getContentType();
    if (contentType != null && contentType.contains("multipart/form-data")) {
      return true;
    }
    return req.getHeader("Range") != null;
  }

  private boolean isBinaryResponse(ResponseWrapper res) {
    String cd = res.getHeader("Content-Disposition");
    if (cd != null && cd.toLowerCase().contains("attachment")) {
      return true;
    }
    String ct = res.getContentType();
    return ct != null && !isTextContent(ct);
  }

  private boolean isTextContent(String contentType) {
    if (contentType == null) {
      return true;
    }
    String lower = contentType.toLowerCase();
    return lower.contains("json")
        || lower.startsWith("text/")
        || lower.contains("xml")
        || lower.contains("urlencoded");
  }
}
