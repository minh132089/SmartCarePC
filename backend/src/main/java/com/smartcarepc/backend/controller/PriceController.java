package com.smartcarepc.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.service.price.Offer;
import com.smartcarepc.backend.service.price.PriceEngine;

import lombok.RequiredArgsConstructor;

/** Bảng giá tốt nhất cho một từ khoá: giá VND, cửa hàng, còn hàng, bảo hành, lúc kiểm tra, link mua */
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceEngine engine;

    @GetMapping
    public List<Offer> prices(@RequestParam("q") String query) {
        String q = query == null ? "" : query.strip();
        if (q.length() < 2 || q.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khoá phải dài từ 2 tới 100 ký tự");
        }
        return engine.search(q);
    }
}
