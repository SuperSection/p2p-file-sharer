package com.supersection.p2p.utils;

public record ParseResult(
        String filename,
        String contentType,
        byte[] fileContent
) {}
