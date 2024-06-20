package com.liu.parse;

import com.liu.utils.FileUtils;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import net.htmlparser.jericho.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCrawler {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    //图片是否分离到各自md文件同名文件夹
    public static final Boolean imgIsolate = true;
    //md文件夹路径
    public static final String mdDir = "./_posts/";
    //图片统一路径，imgIsolate需为false
    public static final String imgDir = Paths.get(System.getProperty("user.dir"), "images").toString();

    public void crawl(String url) {
        createDirectory(mdDir);
        String baseUrl = getBaseUrl(url);
        String listUrl = baseUrl + "/article/list/";

        for (int i = 1; ; i++) {
            String startUrl = listUrl + i;
            Document doc = fetchDocument(startUrl);
            if (doc == null) break;

            Element articleListElement = doc.select("div.article-list").first();
            if (articleListElement == null) break;

            List<CompletableFuture<Void>> futureList = new LinkedList<>();
            for (Element articleElement : articleListElement.children()) {
                String articleId = articleElement.attr("data-articleid");
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> crawlDetailById(baseUrl, articleId), executorService);
                futureList.add(future);
            }
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        }
        executorService.shutdown();
    }

    public void crawlOne(String baseUrl, String articleId) {
        System.out.println("》》》》》》》爬虫开始《《《《《《《");
        createDirectory(mdDir);
        crawlDetailById(baseUrl, articleId);
        System.out.println("》》》》》》》爬虫结束《《《《《《《");
    }

    /**
     * 按id爬取单篇文章
     *
     * @param baseUrl   基本url
     * @param articleId 文章id
     */
    protected void crawlDetailById(String baseUrl, String articleId) {
        String startUrl = baseUrl + "/article/details/" + articleId;
        Document doc = fetchDocument(startUrl);
        if (doc == null) return;

        //文章主体内容
        Element htmlElement = doc.select("div#content_views").first();
        //文章标题
        String title = doc.selectFirst(".title-article").text();
        //文件名称
        String fileName = title;
        System.out.println("[" + fileName+ "]" + "正在爬取...");
        // 获取时间字符串
        //更好的时间获取
        String time = DynamicScraperTime(startUrl);
        //String time = doc.selectFirst("span.time").text();

        // 使用正则表达式提取时间
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        Matcher matcher = pattern.matcher(time);
        if (matcher.find()) {
            String extractedTime = matcher.group();
            String formattedTime = extractedTime.replace(" ", "-").replace(":", "-");
            time = formattedTime;
            fileName = time;
        } else {
            time = "";
            System.out.println("[" + title+ "]" + "未找到时间字符串");
            return;
        }
        //判断该文章是否已经存在
        if (isFileExists(fileName, time.split(" ")[0])){
            System.out.println("[" + title+ "]" + "文章已经存在，跳过");
            return;
        }

        //处理Front-matter
        String jekyllHeader = createJekyllHeader(title, time, doc.select("div.tags-box"));

        //处理正文内容
        String html = htmlElement.html();
        Source source = new Source(html);
        source.fullSequentialParse();

        //如果图片统一放置，则创建images
        if (!imgIsolate){
            File imagesDir = new File(imgDir);
            if (!imagesDir.exists()) {
                imagesDir.mkdir();
            }
        }
        List<net.htmlparser.jericho.Element> imgElements = source.getAllElements("img");
        for (net.htmlparser.jericho.Element imgElement : imgElements) {
            String src = imgElement.getAttributeValue("src");
            String alt = imgElement.getAttributeValue("alt");
            try {
                String imageDir = FileUtils.downloadImage(src, fileName);
                String markdownImageTag = "";
                //如果图片不分离，则延续相对路径
                //如果图片分离，则只需要图片名称，这是扩展语法
                if (!imgIsolate){
                    markdownImageTag = "![](" + imageDir.replace("\\", "/") + ")";
                }else {
                    markdownImageTag = "{% asset_img " + imageDir +" This is an example image %}";
                }
                html = html.replace(imgElement.toString(), markdownImageTag);
            } catch (Exception e) {
                System.err.println("下载图片失败: " + e.getMessage());
            }
        }

        FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
        String markdown = converter.convert(html).replace("\\[", "[").replace("\\]", "]");
        String cleanedMarkdown = cleanMarkdown(markdown);
        String completeContent = jekyllHeader + cleanedMarkdown;
        saveToFile(fileName, time.split(" ")[0], completeContent);
    }

    protected abstract String getBaseUrl(String url);

    protected abstract String createJekyllHeader(String title, String date, Elements tagsElements);

    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            System.err.println("Fetching URL failed: " + e.getMessage());
            return null;
        }
    }

    private void createDirectory(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * 保存md文件
     *
     * @param fileName 文件名
     * @param date     日期
     * @param content  所容纳之物
     */
    private void saveToFile(String fileName, String date, String content) {
        String mdFileName = "";
        if (!imgIsolate){
            mdFileName = mdDir + date + '-' + fileName + ".md";
        }else {
            mdFileName = mdDir + fileName + ".md";
        }
        try (FileWriter writer = new FileWriter(mdFileName)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * md文件是否存在
     *
     * @param mdDir    md目录
     * @param fileName 文件名
     * @param date     日期
     * @return boolean
     */
    public static boolean isFileExists(String fileName, String date) {
        String mdFileName = "";
        if (!imgIsolate) {
            mdFileName = mdDir + date + '-' + fileName + ".md";
        } else {
            mdFileName = mdDir + fileName + ".md";
        }
        File file = new File(mdFileName);
        return file.exists();
    }

    public static String cleanMarkdown(String markdown) {
        markdown = removeToc(markdown);
        markdown = replaceAnchorWithHeading(markdown);
        return markdown;
    }

    // 删除包含“#### 文章目录”的部分
    private static String removeToc(String markdown) {
        String tocPattern = "(?s)<br />\\s*#### 文章目录.*?<br />";
        return markdown.replaceAll(tocPattern, "");
    }

    // 将形如 {#xxx} 替换为 # 使后面的文字成为一级标题
    private static String replaceAnchorWithHeading(String markdown) {
        String anchorPattern = "\\{#.*?\\}(.*)";
        Pattern pattern = Pattern.compile(anchorPattern);
        Matcher matcher = pattern.matcher(markdown);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, "# " + matcher.group(1).trim());
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 动态刮取时间
     *
     * @return {@link String}
     */
    private static String DynamicScraperTime(String url){
        // 设置ChromeDriver路径
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\mumu\\Software\\chromedriver-win64\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        try {
            // 打开目标URL
            driver.get(url);

            // 等待元素加载
            WebDriverWait wait = new WebDriverWait(driver, 2);
            WebElement timeElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.time")));

            // 获取时间文本
            String timeText = timeElement.getText();
            //System.out.println("Time: " + timeText);
            return timeText;
        } finally {
            // 关闭浏览器
            driver.quit();
        }
    }
}