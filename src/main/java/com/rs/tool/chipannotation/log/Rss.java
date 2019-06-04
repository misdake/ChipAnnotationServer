package com.rs.tool.chipannotation.log;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Rss {

    private static String rsslogTemplate;
    private static String rssdayTemplate;
    private static String itemTemplate;

    public static void main(String[] args) throws IOException {
        String statePrevString = new String(Files.readAllBytes(new File("log/state.json").toPath()), StandardCharsets.UTF_8);
        String logPrevString = new String(Files.readAllBytes(new File("log/log.json").toPath()), StandardCharsets.UTF_8);
        State statePrev = Log.gson.fromJson(statePrevString, State.class);
        Log[] logPrev = Log.gson.fromJson(logPrevString, Log[].class);
        TreeMap<String, Log[]> groups = Diff.groupByDay(logPrev, new Date());

        String rss = rssForLog(statePrev.time, logPrev);
        Files.write(new File("log/rss.xml").toPath(), rss.getBytes(StandardCharsets.UTF_8));

        String rssday = rssForDay(statePrev.time, groups);
        Files.write(new File("log/rssday.xml").toPath(), rssday.getBytes(StandardCharsets.UTF_8));
    }

    static {
        try {
            rsslogTemplate = new String(Files.readAllBytes(Paths.get(Rss.class.getClassLoader().getResource("rss_log_template.xml").toURI())));
            rssdayTemplate = new String(Files.readAllBytes(Paths.get(Rss.class.getClassLoader().getResource("rss_day_template.xml").toURI())));
            itemTemplate = new String(Files.readAllBytes(Paths.get(Rss.class.getClassLoader().getResource("rss_item_template.xml").toURI())));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    public static String rssForLog(Date last, Log[] logs) {
        StringBuilder b = new StringBuilder();

        for (Log log : logs) {
            String s = itemForLog(log);
            b.append(s).append("\n");
        }

        String dateString = Log.formatter.format(last);
        return String.format(rsslogTemplate, dateString, b.toString());
    }

    public static String itemForLog(Log log) {
        String title = "";
        String description = "";
        Date pubDate = log.time;
        String link = "";
        String guid = "";

        String dateString = Log.formatter.format(pubDate);

        switch (log.logType) {
            case IMAGE_CREATE: {
                Log.ImageCreate imageCreate = (Log.ImageCreate) log;
                title = "New Image: " + imageCreate.name;
                description = "";
                link = "https://misdake.github.io/ChipAnnotationViewer/?map=" + imageCreate.name;
                guid = imageCreate.name;
                break;
            }
            case COMMENT_INSERT: {
                Log.CommentInsert commentInsert = (Log.CommentInsert) log;
                title = "New Comment: " + commentInsert.title + " for " + commentInsert.imageName + " by " + commentInsert.username;
                description = "";
                link = "https://misdake.github.io/ChipAnnotationViewer/?map=" + commentInsert.imageName + "&amp;commentId=" + commentInsert.commentId;
                guid = commentInsert.imageName + "_" + commentInsert.githubIssueId + "_" + commentInsert.commentId + "_INSERT";
                break;
            }
            case COMMENT_UPDATE: {
                Log.CommentUpdate commentUpdate = (Log.CommentUpdate) log;
                title = "Update Comment: " + commentUpdate.title + " for " + commentUpdate.imageName + " by " + commentUpdate.username;
                description = "";
                link = "https://misdake.github.io/ChipAnnotationViewer/?map=" + commentUpdate.imageName + "&amp;commentId=" + commentUpdate.commentId;
                guid = commentUpdate.imageName + "_" + commentUpdate.githubIssueId + "_" + commentUpdate.commentId + "_UPDATE_" + pubDate.getTime();
                break;
            }
        }

        return String.format(itemTemplate, title, description, dateString, link, guid);
    }


    public static String rssForDay(Date last, SortedMap<String, Log[]> groups) {
        StringBuilder b = new StringBuilder();

        for (Map.Entry<String, Log[]> e : groups.entrySet()) {
            String day = e.getKey();
            Log[] logs = e.getValue();
            String s = itemForDay(day, logs);
            b.append(s).append("\n");
        }

        return String.format(rssdayTemplate, last, b.toString());
    }

    public static String itemForDay(String day, Log[] logs) {
        StringBuilder b = new StringBuilder();

        for (Log log : logs) {
            switch (log.logType) {
                case IMAGE_CREATE: {
                    Log.ImageCreate imageCreate = (Log.ImageCreate) log;
                    String link = "https://misdake.github.io/ChipAnnotationViewer/?map=" + imageCreate.name;
                    String line = String.format("New Image: <a href='%s'>%s</a>", link, imageCreate.name);
                    b.append(line).append("<br>");
                    break;
                }
                case COMMENT_INSERT: {
                    Log.CommentInsert commentInsert = (Log.CommentInsert) log;
                    String title = commentInsert.title + " for " + commentInsert.imageName + " by " + commentInsert.username;
                    String link = "https://misdake.github.io/ChipAnnotationViewer/?map=" + commentInsert.imageName + "&amp;commentId=" + commentInsert.commentId;
                    String line = String.format("New Comment: <a href='%s'>%s</a>", link, title);
                    b.append(line).append("<br>");
                    break;
                }
                case COMMENT_UPDATE: {
                    Log.CommentUpdate commentUpdate = (Log.CommentUpdate) log;
                    String title = commentUpdate.title + " for " + commentUpdate.imageName + " by " + commentUpdate.username;
                    String link = "https://misdake.github.io/ChipAnnotationViewer/?map=" + commentUpdate.imageName + "&amp;commentId=" + commentUpdate.commentId;
                    String line = String.format("Update Comment: <a href='%s'>%s</a>", link, title);
                    b.append(line).append("<br>");
                    break;
                }
            }
        }

        String title = "Update for " + day;
        String description = b.toString();
        Date pubDate = logs[logs.length - 1].time;
        String itemlink = "https://misdake.github.io/ChipAnnotationViewer/";
        String itemguid = "log_" + day;
        String dateString = Log.formatter.format(pubDate);

        return String.format(itemTemplate, title, description, dateString, itemlink, itemguid);
    }

}
