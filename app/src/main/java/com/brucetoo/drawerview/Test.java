package com.brucetoo.drawerview;

/**
 * Created by Bruce Too
 * On 20/12/2016.
 * At 13:48
 */

public class Test {


    private int getTime(int i){
        return 2;
    }

    public static void main(String args[]){

        Test test = new Test();

        int i = -1;
        i = Reflecter.on(test).call("getTime",1).get();
        System.out.println(i);
    }
}
