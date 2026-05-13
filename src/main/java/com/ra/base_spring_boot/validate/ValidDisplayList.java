package com.ra.base_spring_boot.validate;

public interface ValidDisplayList {
    void validPage (int page, int size, int totalElements);
    void validSort (String sort);
}
