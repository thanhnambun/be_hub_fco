package com.ra.base_spring_boot.validate.impl;

import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.validate.ValidDisplayList;
import org.springframework.stereotype.Component;

@Component
public class ValidDisplayListImpl implements ValidDisplayList {
    @Override
    public void validPage(int page, int size, int totalElements) {
        if (page < 0) {
            throw new HttpBadRequest("Invalid page index! Must be >= 0.");
        }

        if (size <= 0) {
            throw new HttpBadRequest("Invalid page size! Must be > 0.");
        }

        int totalPages = (int) Math.ceil(totalElements / (double) size);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (page >= totalPages) {
            throw new HttpBadRequest("Invalid page index! Total pages: " + totalPages);
        }
    }

    @Override
    public void validSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return;
        }

        String normalized = sort.trim().toLowerCase();

        if (!normalized.equals("asc") && !normalized.equals("desc")) {
            throw new HttpBadRequest("Invalid sort direction! Must be 'asc' or 'desc'.");
        }
    }

}
