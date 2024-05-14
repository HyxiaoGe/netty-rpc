package com.hyxiao.rpc.client;

import com.hyxiao.rpc.codec.RpcRequest;
import com.hyxiao.rpc.codec.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RpcFuture implements Future<Object> {

    private RpcRequest rpcRequest;

    private RpcResponse rpcResponse;

    private long startTime;

    private static final long TIMETHRESHOLD = 5000;

    private List<RpcCallback> pendingCallbacks= new ArrayList<>();

    private Sync sync;

    private ReentrantLock lock = new ReentrantLock();

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(16,16,60,TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    public RpcFuture(RpcRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        this.startTime = System.currentTimeMillis();
        this.sync = new Sync();
    }

    public void done(RpcResponse rpcResponse) {
        this.rpcResponse = rpcResponse;
        boolean isRelease = sync.release(1);
        if (!isRelease) {
            throw new RuntimeException("release error");
        }
        invokeCallback();

        long costTime = System.currentTimeMillis() - startTime;

        if (TIMETHRESHOLD < costTime) {
            log.warn("The response time is too long. Request id: {}, Response Time: {}ms", rpcResponse.getRequestId(), costTime);
        }

    }

    private void invokeCallback() {
        lock.lock();
        try {
            for (final RpcCallback pendingCallback : pendingCallbacks) {
                runCallback(pendingCallback);
            }
        } finally {
            lock.unlock();
        }

    }

    private void runCallback(RpcCallback pendingCallback) {
        final RpcResponse response = this.rpcResponse;
        executor.submit(() -> {
            if (response.getThrowable() == null) {
                pendingCallback.success(response.getResult());
            } else {
                pendingCallback.failure(response.getThrowable());
            }
        });
    }

    public RpcFuture addCallback(RpcCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() {
        sync.acquire(-1);
        if (this.rpcResponse != null) {
            return this.rpcResponse.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException {
        boolean isSuccess = sync.tryAcquireSharedNanos(-1, unit.toNanos(timeout));
        if (isSuccess) {
            if (this.rpcResponse != null) {
                return this.rpcResponse.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.rpcRequest.getRequestId());
        }

    }

    class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 2852387826706661964L;

        private final int done = 1;

        private final int pending = 0;

        protected boolean tryAcquire(int acquires) {
            return getState() == done;
        }

        protected boolean tryRelease(int releases) {
            if (getState() == pending) {
                return compareAndSetState(pending, done);
            }
            return false;
        }

        public boolean isDone() {
            return getState() == done;
        }

    }

}
