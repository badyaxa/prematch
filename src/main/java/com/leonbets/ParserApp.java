package com.leonbets;

import com.leonbets.service.LeonService;
import com.leonbets.service.impl.LeonApiJsonService;
import com.leonbets.service.impl.LeonJsoupService;
import com.leonbets.service.impl.LeonSeleniumService;

public class ParserApp {

    public static void main(String[] args) {

        System.out.println("=================Json Approach=================");
        LeonService apiJsonService = new LeonApiJsonService();
        apiJsonService.printToConsole();

        System.out.println("=================Jsoup Approach=================");
        LeonService jsoupService = new LeonJsoupService();
        jsoupService.printToConsole();

        System.out.println("=================Selenium Approach=================");
        LeonService seleniumService = new LeonSeleniumService();
        seleniumService.printToConsole();
    }
}
