package com.nokia.as.main;

import com.nokia.as.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static Logger logger;

    public static void main(String[] args) {
        try {
            logger = LoggerFactory.getLogger(Main.class);

            logger.info("START");

            StringBuilder globalJson = new StringBuilder("[");

            String indexHtml = FileUtil.readFile("recipes/index.html", null);

            HashMap<String, String> headLines = new HashMap<String, String>();

            Elements headLinesElem = Jsoup.parse(indexHtml).getElementsByClass("list").get(0)
                    .getElementsByClass("col-lg-4");

            for(Element elem : headLinesElem) {
                String id = StringUtils.substringBetween(
                        elem.getElementsByTag("a").get(0).attr("href"),
                        "recipe/", ".html");
                String headLine = elem.getElementsByTag("p").get(0).html();
                headLines.put(id, headLine);
            }

            Stream<Path> walk = Files.walk(Paths.get("recipes/html"));

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            for(int i = 0; i < result.size(); i++) {
                String html = FileUtil.readFile(result.get(i), null);
                Document doc = Jsoup.parse(html);

                Element container = doc.getElementsByClass("container").get(0);

                String name = container
                        .getElementsByClass("col-lg-12").get(0)
                        .getElementsByTag("h1").get(0).html();

                String publicationDate = container
                        .getElementsByClass("col-lg-12").get(0)
                        .getElementsByTag("h4").get(0).html();

                String servesHowManyPeople = container
                        .getElementsByClass("info").get(0).getElementsByClass("row").get(0)
                        .getElementsByClass("col-lg-4").get(0).getElementsByTag("p").get(1)
                        .html();
                servesHowManyPeople = StringUtils.substringBetween(servesHowManyPeople, "</i> ", " ");

                String preparationTime = container
                        .getElementsByClass("info").get(0).getElementsByClass("row").get(0)
                        .getElementsByClass("col-lg-4").get(1).getElementsByTag("p").get(1)
                        .html();
                String unit = preparationTime;
                preparationTime = StringUtils.substringBetween(preparationTime, "</i> ", " ");
                unit = StringUtils.substringBetween(unit, preparationTime+" ", "<");
                if("min".equals(unit)) {
                    preparationTime = "00:" + preparationTime + ":00";
                }
                else {
                    preparationTime = preparationTime + ":00:00";
                }

                String cookingTime = container
                        .getElementsByClass("info").get(0).getElementsByClass("row").get(0)
                        .getElementsByClass("col-lg-4").get(2).getElementsByTag("p").get(1)
                        .html();
                unit = cookingTime;
                cookingTime = StringUtils.substringBetween(cookingTime, "</i> ", " ");
                unit = StringUtils.substringBetween(unit, cookingTime+" ", "<");
                if("min".equals(unit)) {
                    cookingTime = "00:" + cookingTime + ":00";
                }
                else {
                    cookingTime = cookingTime + ":00:00";
                }

                String description = container
                        .getElementsByTag("p").get(6).html()
                        .replace("\"", "\\\"");

                String mainPicture = container.
                        getElementsByTag("img").get(0).attr("src").split("img/")[1];

                String[] units = {"g", "mg", "L", "l", "kg", "ml"};
                ArrayList<String> ingredients = new ArrayList<String>();
                Elements ingredientsElem = container.getElementsByClass("ingredient-direction").get(0)
                        .getElementsByTag("ul").get(0).getElementsByTag("li");
                ingLoop: for(Element elem : ingredientsElem) {
                    String line = elem.ownText();
                    unit = line.split(" ")[0];
                    for(String u : units) {
                        if(unit.contains(u)) {
                            String unitFirstPart = unit.split(u)[0];
                            if(StringUtils.isNumeric(unitFirstPart)) {
                                ingredients.add(unitFirstPart+"||"+u+"||"+line.split(unit + " ")[1]);
                                continue ingLoop;
                            }
                        }
                    }
                    if(StringUtils.isNumeric(unit) || unit.contains("/")) {
                        ingredients.add(unit.replace("1/2", "0.5")
                                .replace("3/4", "0.75")
                                +"||null||"+line.split(unit + " ")[1]);
                    }
                    else {
                        ingredients.add("null||null||"+line);
                    }
                }

                ArrayList<String> directions = new ArrayList<String>();
                Elements directionsElem = container.getElementsByClass("directions").get(0)
                        .getElementsByTag("li");
                for(Element elem : directionsElem) {
                    directions.add(elem.ownText());
                }

                String id = mainPicture.split("\\.")[0];

                String headLine = headLines.get(id);

                StringBuilder json = new StringBuilder();

                json.append("{")
                        .append("\"id\":\"").append(id).append("\",")
                        .append("\"name\":\"").append(name).append("\",")
                        .append("\"publicationDate\":\"").append(publicationDate).append("\",")
                        .append("\"mainPicture\":\"").append(mainPicture).append("\",")
                        .append("\"headLine\":\"").append(headLine).append("\",")
                        .append("\"servesHowManyPeople\":").append(servesHowManyPeople).append(",")
                        .append("\"preparationTime\":\"").append(preparationTime).append("\",")
                        .append("\"cookingTime\":\"").append(cookingTime).append("\",")
                        .append("\"description\":\"").append(description).append("\",")
                        .append("\"ingredients\":[");
                for(int j = 0; j < ingredients.size(); j++) {
                    String[] ingredientsSplitted = ingredients.get(j).split("\\|\\|");
                    json.append("{\"name\":\"").append(ingredientsSplitted[2]).append("\"");
                    if(!"null".equals(ingredientsSplitted[0])) {
                        json.append(",\"amount\":").append(ingredientsSplitted[0]);
                    }
                    if(!"null".equals(ingredientsSplitted[1])) {
                        json.append(",\"unit\":\"").append(ingredientsSplitted[1]).append("\"");
                    }
                    json.append("}");
                    if(j < ingredients.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("],\"directions\":[");
                for(int j = 0; j < directions.size(); j++) {
                    json.append("{\"description\":\"").append(directions.get(j)).append("\"");
                    json.append("}");
                    if(j < directions.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");

                globalJson.append(json.toString());
                if(i < result.size() - 1) {
                    globalJson.append(",");
                }
            }

            globalJson.append("]");

            System.out.println(globalJson);

        } catch (IOException e) {
            logger.error("Exception in cc-scraper tool build\n" + e);
        }

    }

}
