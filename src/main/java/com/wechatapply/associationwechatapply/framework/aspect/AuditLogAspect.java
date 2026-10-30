package com.wechatapply.associationwechatapply.framework.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechatapply.associationwechatapply.entity.SysAuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.mapper.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志切面：拦截标注 @AuditLog 的接口，关键操作全量留痕（SRS FR-10）。
 * 日志失败不影响主业务流程（SRS 6.4）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SysAuditLogMapper auditLogMapper;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object result;
        long start = System.currentTimeMillis();
        try {
            result = pjp.proceed();
            save(pjp, auditLog.action(), "SUCCESS", null, start);
        } catch (Throwable e) {
            save(pjp, auditLog.action(), "FAIL", e.getMessage(), start);
            throw e;
        }
        return result;
    }

    private void save(ProceedingJoinPoint pjp, String action, String result, String errMsg, long start) {
        try {
            SysAuditLog row = new SysAuditLog();
            row.setOperateBy(LoginContextHolder.getUserId() == null
                    ? "ANONYMOUS" : String.valueOf(LoginContextHolder.getUserId()));
            row.setAction(action);
            row.setParamsBrief(brief(pjp));
            row.setIp(request.getRemoteAddr());
            row.setOperateAt(LocalDateTime.now());
            row.setResult(result + (errMsg == null ? "" : ": " + errMsg));
            row.setCostMs(System.currentTimeMillis() - start);
            auditLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("审计日志写入失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /** 参数摘要：仅保留简单类型的参数值，避免整包序列化大对象与敏感文件内容。 */
    private String brief(ProceedingJoinPoint pjp) {
        try {
            Object[] args = pjp.getArgs();
            StringBuilder sb = new StringBuilder("{");
            for (Object a : args) {
                if (a == null) continue;
                String s = a.getClass().getSimpleName().contains("MultipartFile")
                        ? "<file>" : String.valueOf(a);
                if (sb.length() > 1) sb.append(",");
                sb.append(s.length() > 120 ? s.substring(0, 120) + "…" : s);
            }
            return sb.append("}").toString();
        } catch (Exception e) {
            return "-";
        }
    }
}
