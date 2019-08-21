package com.oceanzhang.bubblemovie;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oceanzhang.bubblemovie.util.Base64;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.util.TextUtils;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MtopRequest {
    static OkHttpClient httpClient = getTrustAllClient();
    static final String IP = "192.168.102.10";

    public static String getMtopApiSign(HashMap<String, String> data, String appKey, String authCode) throws IOException {
        Request.Builder req = new Request.Builder()
                .url("http://" + IP + ":3296/getMtopApiSign/");
        FormBody formBody = new FormBody.Builder()
                .add("appKey", appKey)
                .add("data", Hex.encodeHexString(JSONObject.toJSONString(data).getBytes())).build();

        String sign = httpClient.newCall(req.post(formBody).build()).execute().body().string();
        System.out.println("sign->" + sign);
        return sign;
    }

    public static String getSecBodyDataEx(HashMap<String, String> data, String time, String appKey, String authCode) throws IOException {
        Request.Builder req = new Request.Builder()
                .url("http://" + IP + ":3296/getSecBodyDataEx/");
        FormBody formBody = new FormBody.Builder()
                .add("time", time)
                .add("appKey", appKey)
                .add("data", Hex.encodeHexString(JSONObject.toJSONString(data).getBytes())).build();
        String miua = httpClient.newCall(req.post(formBody).build()).execute().body().string();
        System.out.println("miua->" + miua);
        return miua;

    }

    public static String getAvmpSign(String sign, String authCode) throws IOException {
        Request.Builder req = new Request.Builder()
                .url("http://" + IP + ":3296/getAvmpSign/");
        FormBody formBody = new FormBody.Builder()
                .add("sign", sign).build();
        String miua = httpClient.newCall(req.post(formBody).build()).execute().body().string();
        System.out.println("avmpsign->" + miua);
        return miua;

    }

    public static void main(String... args) throws Exception {
        String mainItemId = "601263739214";
        String cookie = "munb=3728076965; cookie2=160c27584179ebd097d17a6191f385a2; csg=c5e3a02a; t=8322e990376e064ab275707b3f5be0bc; _tb_token_=3838618e1147; munb=3728076965; cookie2=160c27584179ebd097d17a6191f385a2; csg=c5e3a02a; t=8322e990376e064ab275707b3f5be0bc; _tb_token_=3838618e1147";
        String dm_header_loginkey = "fb3bc90ab42d4b8a8e1d57248ebfbbf7_3_2";
        while (true) {
            String buyParams = "";
            try {
                JSONObject data = JSONObject.parseObject("{\"appType\":\"1\",\"source\":\"10101\",\"itemId\":\"" + mainItemId + "\",\"osType\":\"2\",\"version\":\"6000051\",\"channel_from\":\"damai_market\"}");
                Map<String, String> params = new HashMap<>();
                params.put("source", "10101");
                params.put("data", data.toJSONString());
                params.put("appType", "1");
                params.put("osType", "2");
                params.put("version", "6000051");
                params.put("channel_from", "damai_market");
                params.put("type", "originaljson");
                JSONObject obj = mtop("https://acs.m.taobao.com/gw/",
                        "mtop.alibaba.damai.detail.getdetail",
                        "1.2",
                        "POST",
                        cookie,
                        data.toJSONString(),
                        params
                );
                String result = obj.getString("result");
                if (TextUtils.isEmpty(result)) {
                    continue;
                }
                obj = JSONObject.parseObject(result);
                List<String> prices = new ArrayList<>();
//                prices.add("221808001");
//                prices.add("221789012");
                //优先选择抢购的价格类型
                prices.add("221800200");
                prices.add("221800198");
                prices.add("221800199");
                prices.add("221800201");
                prices.add("221800202");
                prices.add("221800197");

                System.out.println(obj.toJSONString());
                JSONObject item = obj.getJSONObject("detailViewComponentMap").getJSONObject("item").getJSONObject("item");
                System.out.println(item.getString("buyBtnText"));
//                if (item.getString("buyBtnText").equals("即将开售")) {
                if (item.getString("buyBtnText").equals("立即购买") || item.getString("buyBtnText").equals("立即预订")) {
                    //哪些座位类型
                    JSONArray skuList = item.getJSONArray("performBases").getJSONObject(0).getJSONArray("performs").getJSONObject(0).getJSONArray("skuList");
                    String itemId = item.getJSONArray("performBases").getJSONObject(0).getJSONArray("performs").getJSONObject(0).getString("itemId");
                    List<JSONObject> skus = new ArrayList<>();
                    for (int i = 0; i < skuList.size(); i++) {
                        JSONObject sku = skuList.getJSONObject(i);

                        boolean skuEnable = sku.getBooleanValue("skuEnable");
                        if (skuEnable) {
                            skus.add(sku);
                        } else {
                            System.out.println("没票了->" + sku.getDoubleValue("price"));
                        }
                    }
                    out:
                    for (String id : prices) {
                        for (JSONObject sku : skus) {
                            String skuId = sku.getString("skuId");
                            double price = sku.getDoubleValue("price");
                            String priceId = sku.getString("priceId");
                            System.out.println("priceId->" + priceId + "  price->" + price);

                            if (priceId.equals(id)) {
                                System.out.println("抢：" + price);
                                buyParams = itemId + "_1_" + skuId;
                                break out;
                            }
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(buyParams)) {
                continue;
            }
                try {
                    JSONObject _data = JSONObject.parseObject("{\"buyNow\":\"true\",\"exParams\":\"{\\\"seatInfo\\\":\\\"\\\",\\\"umpChannel\\\":\\\"10001\\\",\\\"atomSplit\\\":\\\"1\\\",\\\"channel\\\":\\\"damai_app\\\"}\",\"buyParam\":\"" + buyParams + "\"}");
                    Map<String, String> mainparams = new HashMap<>();
                    mainparams.put("source", "10101");
                    mainparams.put("appType", "1");
                    mainparams.put("osType", "2");
                    mainparams.put("version", "6000051");
                    mainparams.put("channel_from", "damai_market");
                    mainparams.put("type", "originaljson");
                    mainparams.put("data", _data.toJSONString());
                    JSONObject obj = mtop("https://mtop.damai.cn/gw/",
                            "mtop.trade.buildorder",
                            "3.0",
                            "POST",
                            cookie,
                            _data.toJSONString(),
                            mainparams
                    );

                    {
                        System.out.println(obj.toJSONString());
                        JSONObject hierarchy = obj.getJSONObject("hierarchy");
                        JSONObject linkage = obj.getJSONObject("linkage");
                        JSONObject data = obj.getJSONObject("data");
                        JSONObject newdata = new JSONObject();
                        String operator = "";
                        for (String key : data.keySet()) {
                            if (key.startsWith("dmTicketBuyer")) {
                                operator = key;
                                newdata.put(key, data.get(key));
                            } else if (key.startsWith("dmExtraAttributes") || key.startsWith("dmPayType") || key.startsWith("dmDeliveryWay")) {
                                newdata.put(key, data.get(key));
                            }
                        }

                        JSONObject params = new JSONObject();
                        hierarchy.remove("root");
                        linkage.remove("input");
                        linkage.remove("request");
                        linkage.getJSONObject("common").remove("structures");
                        linkage.getJSONObject("common").remove("submitParams");
                        JSONObject fields = newdata.getJSONObject(operator).getJSONObject("fields");
                        JSONArray ticketBuyerList = fields.getJSONArray("ticketBuyerList");
                        System.out.println("buyerTotalNum->" + fields.getIntValue("buyerTotalNum"));
                        if (fields.getIntValue("buyerTotalNum") == 1) {
                            ticketBuyerList.getJSONObject(0).put("isUsed", "false");
                            ticketBuyerList.getJSONObject(1).put("isUsed", "true");
                        } else if (fields.getIntValue("buyerTotalNum") == 2) {
                            ticketBuyerList.getJSONObject(0).put("isUsed", "true");
                            ticketBuyerList.getJSONObject(1).put("isUsed", "true");
                        }

                        params.put("hierarchy", hierarchy);
                        params.put("linkage", linkage);
                        params.put("operator", operator);
                        params.put("data", newdata);
                        String paramsBase64 = Base64.encodeToString(compress(params.toJSONString(), "UTF-8"), Base64.DEFAULT);
                        JSONObject pp = new JSONObject();
                        pp.put("params", paramsBase64);
                        pp.put("feature", "{\"gzip\":\"true\"}");


                        HashMap<String, String> _params = new HashMap<>();
                        _params.put("data", pp.toJSONString());
                        obj = mtop("https://mtop.damai.cn/gw/",
                                "mtop.trade.adjustbuildorder",
                                "1.0",
                                "POST",
                                cookie,
                                pp.toJSONString(),
                                _params
                        );
                    }
                    {
                        System.out.println(obj.toJSONString());
                        JSONObject hierarchy = obj.getJSONObject("hierarchy");
                        JSONObject linkage = obj.getJSONObject("linkage");
                        JSONObject data = obj.getJSONObject("data");

                        JSONObject params = new JSONObject();
                        hierarchy.remove("root");
                        linkage.remove("input");
                        linkage.remove("request");
                        linkage.getJSONObject("common").remove("structures");
                        linkage.getJSONObject("common").remove("queryParams");
                        JSONObject newdata = new JSONObject();
                        for (String key : data.keySet()) {
                            if (key.startsWith("dmTicketBuyer") ||
                                    key.startsWith("dmExtraAttributes") ||
                                    key.startsWith("dmPayType") ||
                                    key.startsWith("dmDeliveryWay") ||
                                    key.startsWith("dmInvoice") ||
                                    key.startsWith("dmItem") ||
                                    key.startsWith("dmTerm"
                                    )) {
                                newdata.put(key, data.get(key));
                            }
                        }

                        params.put("hierarchy", hierarchy);
                        params.put("linkage", linkage);
                        params.put("data", newdata);
                        String paramsBase64 = Base64.encodeToString(compress(params.toJSONString(), "UTF-8"), Base64.DEFAULT);
                        JSONObject pp = new JSONObject();
                        pp.put("params", paramsBase64);
                        pp.put("feature", "{\"gzip\":\"true\"}");
                        pp.put("orderMarker", "v:utdid=WeRj4yB22MMDAKBXgWu87D/j");

                        HashMap<String, String> _params = new HashMap<>();
                        _params.put("data", pp.toJSONString());
                        obj = mtop("https://mtop.damai.cn/gw/",
                                "mtop.trade.createorder",
                                "3.0",
                                "POST",
                                cookie,
                                pp.toJSONString(),
                                _params
                        );
                        if (!TextUtils.isEmpty(obj.getString("alipayOrderId"))) {
                            System.out.println("抢票成功，去付款");
                            return;
                        }
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    public static byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] compress(String str, String encoding) {
        if (str == null || str.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes(encoding));
            gzip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private static JSONObject mtop(String host, String api, String v, String method, String cookie, String data, Map<String, String> params) throws Exception {
        String appKey = "23781390";
        String deviceId = "AlS3b3B0QEkfFod6jCxkSh0Tp-YEBwwk_xcR5QT5K0hF";
        String sid = "160c27584179ebd097d17a6191f385a2";
        long t = System.currentTimeMillis() / 1000;
        String ttid = "10005882@damai_android_7.5.3";
        String uid = "3728076965";
        String utdid = "WeRj4yB22MMDAKBXgWu87D/j";
        String x_features = "27";

        //getMtopApiSign

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("api", api);
        hashMap.put("appKey", appKey);
        hashMap.put("data", data);
        hashMap.put("deviceId", deviceId);
        hashMap.put("sid", sid);
        hashMap.put("t", String.valueOf(t));
        hashMap.put("ttid", ttid);
        hashMap.put("uid", uid);
        hashMap.put("utdid", utdid);
        hashMap.put("v", v);
        hashMap.put("x-features", x_features);

        String authCode = null;
        String xsign = getMtopApiSign(hashMap, appKey, authCode);

        HashMap<String, String> hashMap2 = new HashMap<>();
        hashMap2.put("pageId", "");
        hashMap2.put("pageName", "");
        String mini_wua = getSecBodyDataEx(hashMap2, String.valueOf(t), appKey, authCode);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Damai-UA", "damaiapp_android_v7.5.3");
        headers.put("connmode", "Wifi");
        headers.put("f-refer", "mtop");
        headers.put("iscard", "0");
        headers.put("ispos", "0");
        headers.put("model", "Kindle+Fire+HDX");
        headers.put("osVersion", "5.1.1");
        headers.put("posmode", "gps%2Cwifi");
        headers.put("user-agent", "MTOPSDK%2F3.0.5.4+%28Android%3B5.1.1%3Bamzn%3BKindle+Fire+HDX%29");
        headers.put("version", "7.5.3");
        headers.put("x-app-ver", "7.5.3");
        headers.put("x-app-conf-v", "0");
        headers.put("x-appkey", appKey);
//        headers.put("x-c-traceid", "WeRj4yB22MMDAKBXgWu87D%2Fj15662007736980009128300");
        headers.put("x-devid", deviceId);
        headers.put("x-features", x_features);
        headers.put("x-hm-mac", "10%3Aae%3A60%3A76%3Aa6%3Af4");
        headers.put("x-mini-wua", URLEncoder.encode(mini_wua));
        headers.put("x-nettype", "WIFI");
        headers.put("x-nq", "WIFI");
        headers.put("x-pv", "5.2");
        headers.put("x-sid", sid);
        headers.put("x-sign", xsign);
        headers.put("x-t", String.valueOf(t));
        headers.put("x-ttid", ttid);
        headers.put("x-umt", "4G1L9XJLOjD%2B1jVsrikkXON7OLtxuSVt");
        headers.put("x-utdid", utdid);
        headers.put("x-uid", uid);
        headers.put("Cookie", cookie);


        String url = host + api + "/" + v + "/";
        Request.Builder requestBuilder = new Request.Builder();
        if ("GET".equals(method)) {
            url = url + "?" + buildMap(params);
            requestBuilder = requestBuilder.url(url);
        } else {
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : params.keySet()) {
                builder.add(key, params.get(key));
            }
            if (api.equalsIgnoreCase("mtop.trade.createorder")) {
                String avmSign = getAvmpSign(xsign, authCode);
                builder.add("wua", avmSign);
            }
            requestBuilder = requestBuilder.url(url)
                    .post(builder.build());
        }


        for (String key : headers.keySet()) {
            requestBuilder.addHeader(key, headers.get(key));
        }
        Response resp = httpClient.newCall(requestBuilder.build()).execute();
        String body = resp.body().string();
        System.out.println(body);
        JSONObject obj = JSONObject.parseObject(body);
        if (obj.getString("ret").contains("调用成功")) {
            return obj.getJSONObject("data");
        }
        throw new IOException(obj.getString("ret"));
    }

    public static String buildMap(Map<String, String> map) {
        StringBuffer sb = new StringBuffer();
        if (map.size() > 0) {
            for (String key : map.keySet()) {
                sb.append(key + "=");
                if (TextUtils.isEmpty(map.get(key))) {
                    sb.append("&");
                } else {
                    String value = map.get(key);
                    try {
                        value = URLEncoder.encode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    sb.append(value + "&");
                }
            }
        }
        return sb.toString();
    }


    private static MyTrustManager mMyTrustManager;

    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            mMyTrustManager = new MyTrustManager();
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{mMyTrustManager}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        return ssfFactory;
    }

    //实现X509TrustManager接口
    public static class MyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    //实现HostnameVerifier接口
    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static OkHttpClient getTrustAllClient() {
        OkHttpClient.Builder mBuilder = new OkHttpClient.Builder();
        mBuilder.sslSocketFactory(createSSLSocketFactory(), mMyTrustManager)
                .hostnameVerifier(new TrustAllHostnameVerifier());
        return mBuilder.build();
    }
}
