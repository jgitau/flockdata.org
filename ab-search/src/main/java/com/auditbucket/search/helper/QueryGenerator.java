package com.auditbucket.search.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: macpro
 * Date: 04/08/2014
 * Time: 15:28
 * To change this template use File | Settings | File Templates.
 */
public class QueryGenerator {
    private static Logger logger = LoggerFactory.getLogger(QueryGenerator.class);

    public static String getSimpleQuery(String queryString, Boolean highlightEnabled) {
        logger.debug("getSimpleQuery {}", queryString);
        StringBuilder simpleQuery = new StringBuilder();
        simpleQuery.append("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"should\": [\n" +
                "        {\n" +
                "          \"query_string\": {\n" +
                "            \"query\": \"" + queryString + "\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }");

        if (highlightEnabled) {
            simpleQuery.append(",\n" +
                    "  \"highlight\": {\n" +
                    "    \"fields\": {\n" +
                    "      \"*\": {}\n" +
                    "    }\n" +
                    "  }");
        }
        simpleQuery.append("\n}");
        return simpleQuery.toString();
    }
}