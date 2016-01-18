package de.in4matiker.ledvisualizer;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Controller {
    private static final int CHANNEL_COUNT = 2;
    private static final int CHANNEL_LENGTH = 3;
    private final String host;
    private final Channel[] channels;
    private final ScheduledExecutorService executorService;
    private final Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            if (socket != null && socket.isConnected()) {
                for (Channel channel : channels) {
                    channel.setBytes(data);
                }
                try {
                    OutputStream out = socket.getOutputStream();
                    out.write(data);
                    out.flush();
                } catch (IOException e) {
                    onConnectionChanged(false);
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                    e.printStackTrace();
                }
            }
        }
    };
    private ScheduledFuture future;
    private InetAddress address;
    private Socket socket;
    private byte[] data;

    public Controller(String host) {
        this.host = host;
        data = new byte[CHANNEL_COUNT * CHANNEL_LENGTH];
        executorService = Executors.newSingleThreadScheduledExecutor();
        channels = new Channel[CHANNEL_COUNT];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Channel(i);
        }
        ConnectRunnable connect = new ConnectRunnable();
        executorService.execute(connect);
    }

    public Channel getChannel(int index) {
        return channels[index];
    }

    public void update() {
        executorService.execute(sendRunnable);
    }

    public void close() {
        for (Channel channel : channels) {
            channel.setColor(0, 0, 0);
        }
        update();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onConnectionChanged(boolean connected) {
        Log.d("NET", "Connected to " + host + ": " + connected);
        if (connected) {
            future = executorService.scheduleAtFixedRate(sendRunnable, 0, 10, TimeUnit.MINUTES);
        } else if (future != null) {
            future.cancel(true);
        }
    }

    private class ConnectRunnable implements Runnable {
        private static final int PORT = 12345;

        @Override
        public void run() {
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            if (address != null) {
                try {
                    socket = new Socket(address, PORT);
                    socket.setTcpNoDelay(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            onConnectionChanged(socket != null && socket.isConnected());
        }
    }
}
