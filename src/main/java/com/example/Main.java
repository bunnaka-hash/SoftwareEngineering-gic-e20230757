package com.example;

public class Main {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("Task A: " + i);
            }
        });

        Thread t2 = new Thread(() -> {
            for (char c = 'A'; c <= 'E'; c++) {
                System.out.println("Task B: " + c);
            }
        });

        t1.start();
        t2.start();
    }
}