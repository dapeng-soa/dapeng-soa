package com.github.dapeng.impl.filters;


import com.github.dapeng.core.FreqControlRule;

public class testShmCallerIp {

    public static void main(String[] args) {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        boolean result = false;
        rule.app = "com.today.servers0";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 200;

        for (int i = 0; i < 100; i++){

            result = manager.reportAndCheck(rule, 2145463247);

            if ( i == 0){
                System.out.println(" first call :");
                System.out.println(" flowControl = "+ result);
                System.out.println();
            }
            if (i == 19){
                System.out.println(" 20th call :");
                System.out.println(" flowControl = "+ result );
                System.out.println();
            }
            if (i == 20){
                System.out.println(" 21th call :");
                System.out.println(" flowControl = "+ result );
                System.out.println();
            }
            try {
                if (i == 78) {
                    System.out.println(" 79th call :");
                    System.out.println(" flowControl = " + result );
                    System.out.println("sleep 1 minute");
                    Thread.sleep(60000);
                    System.out.println();
                }
            }catch (InterruptedException e){
                System.out.println(" InterruptedException ");
            }
            if (i == 79){
                System.out.println(" 80th call :");
                System.out.println(" flowControl = "+ result );
                System.out.println();
            }
            if (i == 80){
                System.out.println(" 81th call :");
                System.out.println(" flowControl = "+ result );
            }

        }


        System.out.println("key1 = 2147463665, call times = 19 ");
        for (int i = 0; i < 19; i++) {
            result = manager.reportAndCheck(rule, 2147463665);
        }
        System.out.println( "flowcontrol = "+ result );
        System.out.println();
        System.out.println("key2 = 2147463651, call times = 18 ");
        for (int i = 0; i < 18; i++) {
            result = manager.reportAndCheck(rule, 2147463651);
        }
        System.out.println( "flowcontrol = "+ result );
        System.out.println();



    }
}
