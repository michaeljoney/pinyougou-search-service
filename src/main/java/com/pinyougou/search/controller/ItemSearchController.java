package com.pinyougou.search.controller;

import com.pinyougou.search.service.ItemSearchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.channels.Selector;
import java.util.Map;

@RestController
@RequestMapping("/itemsearch")
public class ItemSearchController {

    private ItemSearchService itemSearchService;

    public Map<String, Object>search(@RequestBody Map<String, ? extends Object> searchMap){

        return itemSearchService.search(searchMap);
    }
}
