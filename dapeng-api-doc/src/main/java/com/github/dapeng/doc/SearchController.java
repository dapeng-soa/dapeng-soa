package com.github.dapeng.doc;

import com.github.dapeng.doc.cache.ServiceCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by tangliu on 2015/11/2.
 */
@Controller
@RequestMapping("search")
public class SearchController {

    @Autowired
    private ServiceCache serviceCache;

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("tagName", "search");
    }

    @RequestMapping(method = RequestMethod.POST)
    public String index(HttpServletRequest request) {

        String searchText = request.getParameter("searchText");
        List<SearchResultItem> resultList = new ArrayList<>();

        Set<String> keys = ServiceCache.urlMappings.keySet();
        for (String key : keys) {

            if (key.toUpperCase().contains(searchText.toUpperCase())) {
                Set<String> results = ServiceCache.urlMappings.get(key);
                for (String value : results) {

                    SearchResultItem s = new SearchResultItem();
                    s.setName(key);
                    s.setUrl(value);

                    resultList.add(s);
                }
            }
        }
        request.setAttribute("services", serviceCache.getServices().values());
        request.setAttribute("resultList", resultList);
        return "api/search";
    }


    public class SearchResultItem {

        public String name;

        public String url;

        public SearchResultItem() {
            name = "";
            url = "";
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
