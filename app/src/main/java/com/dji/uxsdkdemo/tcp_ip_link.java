package com.dji.uxsdkdemo;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;

public class tcp_ip_link {
    private static tcp_ip_link instance;
    private Socket socket;
    private Thread thread;
    //接收消息回调
    private OnReceiveCallbackBlock receivedCallback;

    //    提供一个全局的静态方法
    public static tcp_ip_link sharedCenter() {
        if (instance == null) {
            synchronized (tcp_ip_link.class) {
                if (instance == null) {
                    instance = new tcp_ip_link();
                }
            }
        }
        return instance;
    }

    /**
     * 通过IP地址(域名)和端口进行连接
     *
     * @param ipAddress IP地址(域名)
     * @param port      端口
     */
    public void connect(final String ipAddress, final int port) {

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ipAddress, port);
//                    socket.setSoTimeout ( 2 * 1000 );//设置超时时间
                    if (socket.isConnected()) {
                        receive();
                        Log.i("TAG", "连接成功");
                    } else {
                        Log.i("TAG", "连接失败");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("TAG", "连接异常");
                }
            }
        });
        thread.start();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (socket.isConnected()) {
            try {
                socket.close();
                if (socket.isClosed()) {
                    Log.i("TAG", "断开连接");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 接收数据
     */
    byte[] bt = new byte[1024];

    public void receive() {
        while (socket.isConnected()) {
            try {
                /**得到的是16进制数，需要进行解析*/

//                获取接收到的字节和字节数
                int length = socket.getInputStream().read(bt);
//                获取正确的字节
                byte[] bs = new byte[length];
                System.arraycopy(bt, 0, bs, 0, length);

                String str = new String(bs, "UTF-8");
                if (str != null) {
                    if (receivedCallback != null) {
                        receivedCallback.callback(str);
                    }
                }
                Log.i("TAG", "接收成功");
            } catch (IOException e) {
                Log.i("TAG", "接收失败");
            }
        }
    }

    /**
     * 发送数据
     *
     * @param data 数据
     */
    public void send(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    try {
                        socket.getOutputStream().write(data);
                        socket.getOutputStream().flush();
                        Log.i("TAG", "发送成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("TAG", "发送失败");
                    }
                } else {
                }
            }
        }).start();
    }

    public interface OnReceiveCallbackBlock {
        void callback(String receicedMessage);
    }

    public void setReceivedCallback(OnReceiveCallbackBlock receivedCallback) {
        this.receivedCallback = receivedCallback;
    }
}
