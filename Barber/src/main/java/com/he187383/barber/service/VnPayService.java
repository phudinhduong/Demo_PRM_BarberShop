package com.he187383.barber.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class VnPayService {

    @Value("${vnpay.tmnCode}")   private String tmnCode;
    @Value("${vnpay.secretKey}") private String secretKey;
    @Value("${vnpay.payUrl}")    private String payUrl;     // https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
    @Value("${vnpay.returnUrl}") private String returnUrl;  // http://localhost:8080/api/payment/vnpay-return

    public String createPayUrl(String ipAddr, String txnRef, int amountVnd, String orderInfo) {
        Map<String,String> v = new TreeMap<>();
        v.put("vnp_Version", "2.1.0");
        v.put("vnp_Command", "pay");
        v.put("vnp_TmnCode", tmnCode);
        v.put("vnp_Amount", String.valueOf(amountVnd * 100L));
        v.put("vnp_CurrCode", "VND");
        v.put("vnp_TxnRef", txnRef);
        v.put("vnp_OrderInfo", orderInfo);
        v.put("vnp_OrderType", "other");
        v.put("vnp_ReturnUrl", returnUrl);
        v.put("vnp_IpAddr", ipAddr);
        v.put("vnp_Locale", "vn");
        v.put("vnp_CreateDate", java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        String query = toQuery(v);
        String hash = hmacSHA512(secretKey, query);
        return payUrl + "?" + query + "&vnp_SecureHash=" + hash;
    }

    public boolean verify(Map<String,String> params, String secureHash) {
        Map<String,String> v = new TreeMap<>(params);
        v.remove("vnp_SecureHash");
        v.remove("vnp_SecureHashType");
        String query = toQuery(v);
        String calc = hmacSHA512(secretKey, query);
        return calc.equalsIgnoreCase(secureHash);
    }

    private String toQuery(Map<String,String> v) {
        StringBuilder sb = new StringBuilder();
        for (var e : v.entrySet()) {
            if (sb.length()>0) sb.append('&');
            sb.append(url(e.getKey())).append('=').append(url(e.getValue()));
        }
        return sb.toString();
    }
    private String url(String s){ return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return bytesToHex(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e){ throw new RuntimeException(e); }
    }
    private String bytesToHex(byte[] b){ StringBuilder sb=new StringBuilder(); for(byte x:b) sb.append(String.format("%02x",x)); return sb.toString(); }
}

