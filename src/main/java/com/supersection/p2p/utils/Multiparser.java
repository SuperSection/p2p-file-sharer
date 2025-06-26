package com.supersection.p2p.utils;


public record Multiparser(byte[] data, String boundary) {

    public ParseResult parse() {
        try {

            String dataAsString = new String(data);

            String filenameMarker = "filename=\"";
            int filenameStart = dataAsString.indexOf(filenameMarker);

            if (filenameStart == -1) {
                return null;
            }

            filenameStart += filenameMarker.length();
            int filenameEnd = dataAsString.indexOf("\"", filenameStart);
            String filename = dataAsString.substring(filenameStart, filenameEnd);

            String contentTypeMarker = "Content-Type: ";
            int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
            String contentType = "application/octet-stream"; // Default

            if (contentTypeStart != -1) {
                contentTypeStart += contentTypeMarker.length();
                int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
            }

            String headerEndMarker = "\r\n\r\n";
            int headerEnd = dataAsString.indexOf(headerEndMarker);
            if (headerEnd == -1) {
                return null;
            }

            int contentStart = headerEnd + headerEndMarker.length();

            byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
            int contentEnd = findSequence(data, boundaryBytes, contentStart);

            if (contentEnd == -1) {
                boundaryBytes = ("\r\n--" + boundary).getBytes();
                contentEnd = findSequence(data, boundaryBytes, contentStart);
            }

            if (contentEnd == -1 || contentEnd <= contentStart) {
                return null;
            }

            byte[] fileContent = new byte[contentEnd - contentStart];
            System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);

            return new ParseResult(filename, contentType, fileContent);

        } catch (Exception e) {
            System.out.println("Error parsing multipart data " + e.getMessage());
            return null;
        }
    }

    private static int findSequence(byte[] data, byte[] sequence, int startPos) {
        outer:
            for (int i = startPos; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i+j] != sequence[j]) {
                        continue outer;
                    }
                }
                return 1;
            }
            return -1;
    }
}