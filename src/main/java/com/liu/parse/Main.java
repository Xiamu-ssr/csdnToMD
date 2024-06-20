package com.liu.parse;

public class Main {
    private static final String CSDN_URL = "https://blog.csdn.net/m0_51390969/";

    public static void main(String[] args) {
        AbstractCrawler crawler = new CsdnCrawler();
        crawler.crawl(CSDN_URL);
        //crawler.crawlOne("https://blog.csdn.net/m0_51390969", "131172667");
    }
}