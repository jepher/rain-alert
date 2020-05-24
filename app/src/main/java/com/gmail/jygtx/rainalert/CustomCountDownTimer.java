package com.gmail.jygtx.rainalert;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

public abstract class CustomCountDownTimer  {
    private long millisInFuture;

    private long countDownInterval;

    private long stopTimeInFuture;

    public CustomCountDownTimer(long millisInFuture, long countDownInterval) {
        this.millisInFuture = millisInFuture;
        this.countDownInterval = countDownInterval;
    }

    public void setMillisInFuture(long millisInFuture) {
        this.millisInFuture = millisInFuture;
    }

    public void setCountdownInterval(long countDownInterval) {
        this.countDownInterval = countDownInterval;
    }

    public final void cancel() {
        handler.removeMessages(MSG);
    }

    public synchronized final CustomCountDownTimer start() {
        if (millisInFuture <= 0) {
            onFinish();
            return this;
        }
        stopTimeInFuture = SystemClock.elapsedRealtime() + millisInFuture;
        handler.sendMessage(handler.obtainMessage(MSG));
        return this;
    }

    public abstract void onTick(long millisUntilFinished);

    public abstract void onFinish();


    private static final int MSG = 1;


    // handles counting down
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            synchronized (CustomCountDownTimer.this) {
                final long millisLeft = stopTimeInFuture - SystemClock.elapsedRealtime();

                if (millisLeft <= 0) {
                    onFinish();
                } else if (millisLeft < countDownInterval) {
                    // no tick, just delay until done
                    sendMessageDelayed(obtainMessage(MSG), millisLeft);
                } else {
                    long lastTickStart = SystemClock.elapsedRealtime();
                    onTick(millisLeft);

                    // take into account user's onTick taking time to execute
                    long delay = lastTickStart + countDownInterval - SystemClock.elapsedRealtime();

                    // special case: user's onTick took more than interval to
                    // complete, skip to next interval
                    while (delay < 0) delay += countDownInterval;

                    sendMessageDelayed(obtainMessage(MSG), delay);
                }
            }
        }
    };
}
