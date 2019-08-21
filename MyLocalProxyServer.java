package ghost.com.thundervodplay;


import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.io.IOException;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;

public class MyLocalProxyServer extends NanoHTTPD {
    public MyLocalProxyServer(int port) {
        super(port);
    }

    protected static int toDigit(final char ch, final int index) throws Exception {
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new Exception("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    public static byte[] decodeHex(final char[] data) throws Exception {

        final int len = data.length;

        if ((len & 0x01) != 0) {
            throw new Exception("Odd number of characters.");
        }

        final byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri.startsWith("/getMtopApiSign")) {
            try {
                session.parseBody(new HashMap<String, String>());
                String data = session.getParms().get("data");
                data = new String(decodeHex(data.toCharArray()));
                String appKey = session.getParms().get("appKey");
                String authCode = session.getParms().get("authCode");
                Class mtopClz = HookDebug.context.getClassLoader().loadClass("mtopsdk.mtop.intf.Mtop");
                Object mtopObj = ReflectionUtils.invoke(mtopClz, null, "instance", new Class[]{Context.class}, HookDebug.context);
                Object mtopConfigObj = ReflectionUtils.getField(mtopClz, mtopObj, "mtopConfig");
                Object signObj = ReflectionUtils.getField(mtopConfigObj.getClass(), mtopConfigObj, "sign");
                HashMap<String, String> map = JSON.parseObject(data, new TypeReference<HashMap<String, String>>() {
                });
                String xsign = (String) ReflectionUtils.invoke(signObj.getClass(), signObj, "getMtopApiSign", new Class[]{HashMap.class, String.class, String.class}, map, appKey, authCode);
                return newFixedLengthResponse(xsign);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.startsWith("/getSecBodyDataEx")) {
            try {
                session.parseBody(new HashMap<String, String>());
                //String.valueOf(time), appKey, authCode, hashMap2, 8
                String data = session.getParms().get("data");
                data = new String(decodeHex(data.toCharArray()));
                String appKey = session.getParms().get("appKey");
                String authCode = session.getParms().get("authCode");
                String time = session.getParms().get("time");
                Class mtopClz = HookDebug.context.getClassLoader().loadClass("mtopsdk.mtop.intf.Mtop");
                Object mtopObj = ReflectionUtils.invoke(mtopClz, null, "instance", new Class[]{Context.class}, HookDebug.context);
                Object mtopConfigObj = ReflectionUtils.getField(mtopClz, mtopObj, "mtopConfig");
                Object signObj = ReflectionUtils.getField(mtopConfigObj.getClass(), mtopConfigObj, "sign");
                HashMap<String, String> map = JSON.parseObject(data, new TypeReference<HashMap<String, String>>() {
                });
                String mini_wua = (String) ReflectionUtils.invoke(signObj.getClass(), signObj, "getSecBodyDataEx", new Class[]{String.class, String.class, String.class, HashMap.class, int.class}, String.valueOf(time), appKey, authCode, map, 8);
                return newFixedLengthResponse(mini_wua);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.startsWith("/getAvmpSign")) {
            try {
                session.parseBody(new HashMap<String, String>());
                String sign = session.getParms().get("sign");
                String authCode = session.getParms().get("authCode");
                Class mtopClz = HookDebug.context.getClassLoader().loadClass("mtopsdk.mtop.intf.Mtop");
                Object mtopObj = ReflectionUtils.invoke(mtopClz, null, "instance", new Class[]{Context.class}, HookDebug.context);
                Object mtopConfigObj = ReflectionUtils.getField(mtopClz, mtopObj, "mtopConfig");
                Object signObj = ReflectionUtils.getField(mtopConfigObj.getClass(), mtopConfigObj, "sign");

                String mini_wua = (String) ReflectionUtils.invoke(signObj.getClass(), signObj, "getAvmpSign", new Class[]{String.class, String.class, int.class}, sign, authCode, 4);
                return newFixedLengthResponse(mini_wua);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.serve(session);
    }

    public static void asyncStart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new MyLocalProxyServer(3296).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
