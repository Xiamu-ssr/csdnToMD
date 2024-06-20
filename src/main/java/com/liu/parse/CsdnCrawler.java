package com.liu.parse;


import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CsdnCrawler extends AbstractCrawler {
    @Override
    protected String getBaseUrl(String url) {
        return url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
    }

    @Override
    protected String createJekyllHeader(String title, String date, Elements tagsElements) {
        String jekyllTitle = "title: " + title + "\n";
        String jekyllDate = "date: " + date + "\n";
        String jekyllCategories = "";
        String jekyllTags = "";

        // 提取分类和标签
        Elements categoryElements = new Elements();
        Elements tagElements = new Elements();

        boolean isCategory = false;
        for (Element element : tagsElements.first().children()) {
            if (element.tagName().equals("span")) {
                String label = element.text();
                if (label.contains("分类专栏")) {
                    isCategory = true;
                } else if (label.contains("文章标签")) {
                    isCategory = false;
                }
            } else if (element.tagName().equals("a")) {
                if (isCategory) {
                    categoryElements.add(element);
                } else {
                    tagElements.add(element);
                }
            }
        }


        // 生成 categories 和 tags 字段
        if (categoryElements.size() > 0) {
            jekyllCategories = "categories:\n" + getTagsBoxValue(categoryElements);
        }
        if (tagElements.size() > 0) {
            jekyllTags = "tags:\n" + getTagsBoxValue(tagElements);
        }

        return "---\nlayout: post\n" + jekyllTitle + jekyllDate + "author: 'Xiamu'\nheader-img: 'img/post-bg-2015.jpg'\n" + jekyllTags + jekyllCategories + "\n---\n";
    }

    private String getTagsBoxValue(Elements elements) {
        StringBuilder tags = new StringBuilder();
        for (Element element : elements) {
            tags.append("- ").append(element.text().replace("\t", "").replace("\n", "").replace("\r", "")).append("\n");
        }
        return tags.toString();
    }
}