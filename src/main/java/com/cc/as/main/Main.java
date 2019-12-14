package com.cc.as.main;

import com.cc.as.scraping.Scraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static Logger logger;

    public static void main(String[] args) {
        try {
            logger = LoggerFactory.getLogger(Main.class);

            logger.info("START");

            Scraper scraper = new Scraper();

            scraper.exportToJSON();

        } catch (Exception e) {
            logger.error("Exception in cc-scraper tool build\n" + e);
        }

    }

}
