package com.agritrace.service;

import com.agritrace.common.BatchStatus;
import com.agritrace.common.BusinessException;
import com.agritrace.common.PageResult;
import com.agritrace.dto.BatchDetail;
import com.agritrace.entity.Batch;
import com.agritrace.entity.FarmBase;
import com.agritrace.entity.Logistics;
import com.agritrace.entity.TraceCode;
import com.agritrace.entity.TraceScanLog;
import com.agritrace.mapper.TraceCodeMapper;
import com.agritrace.mapper.TraceScanLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TraceCodeService extends ServiceImpl<TraceCodeMapper, TraceCode> {
    /** 去掉了 0/O、1/I 这些容易看错的字符，方便手工输入 */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BatchService batchService;
    private final TraceScanLogMapper scanLogMapper;

    public PageResult<TraceCode> page(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<TraceCode> q = new LambdaQueryWrapper<TraceCode>()
                .eq(status != null, TraceCode::getStatus, status)
                .orderByDesc(TraceCode::getId);
        if (StringUtils.hasText(keyword)) {
            List<Long> batchIds = batchService.list(new LambdaQueryWrapper<Batch>().like(Batch::getBatchNo, keyword))
                    .stream().map(Batch::getId).toList();
            q.and(w -> w.like(TraceCode::getCode, keyword).or(!batchIds.isEmpty(), x -> x.in(TraceCode::getBatchId, batchIds)));
        }
        Page<TraceCode> p = page(new Page<>(page, size), q);
        batchService.fillBatchInfo(p.getRecords(), TraceCode::getBatchId, (r, b) -> {
            r.setBatchNo(b.getBatchNo());
            r.setProductName(b.getProductName());
            r.setBaseName(b.getBaseName());
            r.setBatchStatus(b.getStatus());
        });
        return PageResult.of(p);
    }

    /** 给批次生成溯源码；已有就直接返回，保证一批一码 */
    public TraceCode generate(Long batchId) {
        Batch b = batchService.mustGet(batchId);
        TraceCode exist = getOne(new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getBatchId, batchId));
        if (exist != null) {
            return exist;
        }
        if (!BatchStatus.PASSED.contains(b.getStatus())) {
            throw new BusinessException("批次 " + b.getBatchNo() + " 质检合格后才能生成溯源码");
        }
        TraceCode c = new TraceCode();
        c.setBatchId(batchId);
        c.setScanCount(0);
        c.setStatus(1);
        do {
            c.setCode(randomCode());
        } while (exists(new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getCode, c.getCode())));
        save(c);
        return c;
    }

    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态不正确");
        }
        TraceCode upd = new TraceCode();
        upd.setId(id);
        upd.setStatus(status);
        if (!updateById(upd)) {
            throw new BusinessException("溯源码不存在");
        }
    }

    /** 消费者扫码：记一次扫码并返回整条溯源链 */
    @Transactional
    public BatchDetail scan(String code, String ip, String userAgent) {
        TraceCode c = getOne(new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getCode, code == null ? "" : code.trim().toUpperCase()));
        if (c == null) {
            throw new BusinessException(404, "没有找到这个溯源码，请核对后再试");
        }
        if (c.getStatus() != 1) {
            throw new BusinessException(410, "该溯源码已作废，请谨慎购买");
        }
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<TraceCode>().eq(TraceCode::getId, c.getId())
                .setSql("scan_count = scan_count + 1").set(TraceCode::getLastScanAt, now));
        TraceScanLog log = new TraceScanLog();
        log.setCode(c.getCode());
        log.setBatchId(c.getBatchId());
        log.setIp(ip);
        log.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(300, userAgent.length())));
        log.setScannedAt(now);
        scanLogMapper.insert(log);

        BatchDetail d = batchService.detail(c.getBatchId());
        // 对外展示时隐藏手机号中间四位
        FarmBase base = d.base();
        if (base != null) {
            base.setPhone(mask(base.getPhone()));
        }
        for (Logistics l : d.logistics()) {
            l.setDriverPhone(mask(l.getDriverPhone()));
        }
        return d;
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder("T");
        for (int i = 0; i < 11; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
