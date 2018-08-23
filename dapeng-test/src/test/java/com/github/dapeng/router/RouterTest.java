package com.github.dapeng.router;

public class RouterTest {
    public static void main(String[] args) {
        ShutDownHookDemo demo = new ShutDownHookDemo();
        System.out.println("shutdown begin...");
        Runtime.getRuntime().addShutdownHook(new Thread(ShutDownHookDemo::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(ShutDownHookDemo::shutdown));

        System.out.println("shutdown end..");
    }
}

class ShutDownHookDemo {
    public static void shutdown() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Inside ShutDownHookDemo:shutdown..");
    }
}
