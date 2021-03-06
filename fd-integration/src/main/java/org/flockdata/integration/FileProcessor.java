/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.integration;

import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityToEntityLinkInput;
import org.flockdata.transform.PayloadBatcher;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.xml.XmlMappable;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StopWatch;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 2:29 PM
 */
@Configuration
public class FileProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private static final DecimalFormat formatter = new DecimalFormat();

    @Autowired (required = false)
    private PayloadBatcher payloadBatcher;

    private long skipCount, rowsToProcess = 0;

    public FileProcessor() {

    }

    public FileProcessor(int skipCount, int rowsToProcess) {
        this.skipCount = skipCount;
        this.rowsToProcess = rowsToProcess;
    }

    public Collection<String> resolveFiles(String source) throws IOException, NotFoundException {
        ArrayList<String> results = new ArrayList<>();
        boolean absoluteFile = true;
        if (source.contains("*") || source.contains("?") || source.endsWith(File.separator))
            absoluteFile = false;

        if (absoluteFile) {
            Reader reader;
            reader = getReader(source);
            if (reader != null) {
                reader.close();
                results.add(source);
                return results;
            }
        }

        String filter;
        String path;

        Path toResolve = Paths.get(source);

        if (source.endsWith(File.separator))
            filter = "*";
        else
            filter = toResolve.getFileName().toString();
        if (filter == null)
            filter = "*";
        path = source.substring(0, source.lastIndexOf(File.separator) + 1);
        FileFilter fileFilter = new WildcardFileFilter(filter);

        // Split the source in to path and filter.
        //path = source.substring(0, source.indexOf())

        File folder = new UrlResource("file:" + path).getFile();
        File[] listOfFiles = folder.listFiles(fileFilter);
        if (listOfFiles == null) {
            folder = new ClassPathResource(path).getFile();
            listOfFiles = folder.listFiles(fileFilter);
        }

        for (File file : listOfFiles) {
            results.add(file.toString());
        }


        return results;
    }

    public int processFile(ExtractProfile extractProfile, String source) throws IllegalAccessException, InstantiationException, IOException, FlockException, ClassNotFoundException {

        //String source = path;
        logger.info("Start processing of {}", source);
        Collection<String> files = resolveFiles(source);
        int result = 0;
        try {
            for (String file : files) {

                if (extractProfile.getContentType() == ExtractProfile.ContentType.CSV)
                    result = processCSVFile(file, extractProfile);
                else if (extractProfile.getContentType() == ExtractProfile.ContentType.XML)
                    result = processXMLFile(file, extractProfile);
                else if (extractProfile.getContentType() == ExtractProfile.ContentType.JSON) {
                    if (extractProfile.getContentModel().getDocumentType() == null)
                        result = processJsonTags(file);
                    else
                        result = processJsonEntities(file, extractProfile);
                }

            }
        } finally {
            if (result > 0) {
                getPayloadBatcher().flush();
            }
        }

        logger.info("Processed {}", source);
        return result;
    }

    private int processJsonTags(String fileName) throws FlockException {
        Collection<TagInputBean> tags;
        ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
        int processed = 0;
        try {
            File file = new File(fileName);
            InputStream stream = null;
            if (!file.exists()) {
                // Try as a resource
                stream = ClassLoader.class.getResourceAsStream(fileName);
                if (stream == null) {
                    logger.error("{} does not exist", fileName);
                    throw new FlockException(fileName+" Does not exist");
                }
            }
            TypeFactory typeFactory = mapper.getTypeFactory();
            CollectionType collType = typeFactory.constructCollectionType(ArrayList.class, TagInputBean.class);

            if (file.exists())
                tags = mapper.readValue(file, collType);
            else
                tags = mapper.readValue(stream, collType);
            for (TagInputBean tag : tags) {
                getPayloadBatcher().writeTag(tag, "JSON Tag Importer");
                processed++;
            }

        } catch (IOException e) {
            logger.error("Error writing exceptions with {} [{}]", fileName, e.getMessage());
            throw new RuntimeException("IO Exception ", e);
        } finally {
            if (processed > 0L)
                getPayloadBatcher().flush();

        }
        return tags.size();
    }

    private int processJsonEntities(String fileName, ExtractProfile extractProfile) throws FlockException {
        int rows = 0;

        File file = new File(fileName);
        InputStream stream = null;
        if (!file.exists()) {
            stream = ClassLoader.class.getResourceAsStream(fileName);
            if (stream == null) {
                logger.error("{} does not exist", fileName);
                return 0;
            }
        }
        StopWatch watch = new StopWatch();

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser;
        List<EntityToEntityLinkInput> referenceInputBeans = new ArrayList<>();

        try {

            //String docType = mappable.getDataType();
            watch.start();
            ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
            try {

                if (stream != null)
                    jParser = jfactory.createParser(stream);
                else
                    jParser = jfactory.createParser(file);


                JsonToken currentToken = jParser.nextToken();
                long then = new DateTime().getMillis();
                JsonNode node;
                if (currentToken == JsonToken.START_ARRAY || currentToken == JsonToken.START_OBJECT) {
                    while (currentToken != null && currentToken != JsonToken.END_OBJECT) {

                        while (currentToken != null && jParser.nextToken() != JsonToken.END_ARRAY) {
                            node = om.readTree(jParser);
                            if (node != null) {
                                processJsonNode(node, extractProfile.getContentModel(), referenceInputBeans);
                                if (stopProcessing(rows++, then)) {
                                    break;
                                }

                            }
                            currentToken = jParser.nextToken();

                        }
                    }
                } else if (currentToken == JsonToken.START_OBJECT) {

                    //om.readTree(jParser);
                    node = om.readTree(jParser);
                    processJsonNode(node, extractProfile.getContentModel(), referenceInputBeans);
                }

            } catch (IOException e1) {
                logger.error("Unexpected", e1);
            }


        } finally {
            getPayloadBatcher().flush();
        }

        return endProcess(watch, rows, 0);
    }

    private void processJsonNode(JsonNode node, ContentModel importProfile, List<EntityToEntityLinkInput> referenceInputBeans) throws FlockException {
        EntityInputBean entityInputBean = Transformer.toEntity(node, importProfile);
        if (!entityInputBean.getEntityLinks().isEmpty()) {
            referenceInputBeans.add(new EntityToEntityLinkInput(entityInputBean));
            entityInputBean.getEntityLinks().size();
        }

        getPayloadBatcher().writeEntity(entityInputBean);

    }

    private int processXMLFile(String file, ExtractProfile extractProfile) throws IOException, FlockException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        try {
            int rows = 0;
            StopWatch watch = new StopWatch();
            StreamSource source = new StreamSource(file);
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(source);

            XmlMappable mappable = (XmlMappable) Class.forName(extractProfile.getHandler()).newInstance();
            mappable.positionReader(xsr);

            String dataType = mappable.getDataType();
            watch.start();
            try {
                long then = new DateTime().getMillis();
                while (xsr.getLocalName().equals(dataType)) {
                    EntityInputBean entityInputBean = Transformer.toEntity(mappable, xsr, extractProfile.getContentModel());
                    rows++;
                    xsr.nextTag();
                    getPayloadBatcher().writeEntity(entityInputBean);

                    if (stopProcessing(rows, then)) {
                        break;
                    }

                }
            } finally {
                getPayloadBatcher().flush();
            }
            return endProcess(watch, rows, 0);


        } catch (XMLStreamException | JAXBException e1) {
            throw new IOException(e1);
        }
    }

    private int processCSVFile(String file, ExtractProfile extractProfile) throws IOException, IllegalAccessException, InstantiationException, FlockException, ClassNotFoundException {

        StopWatch watch = new StopWatch();
        int ignoreCount = 0;
        int currentRow = 0;

        BufferedReader br;
        Reader fileObject = getReader(file);

        br = new BufferedReader(fileObject);

        try {
            CSVReader csvReader;
            if (extractProfile.getQuoteCharacter() != null)
                csvReader = new CSVReader(br, extractProfile.getDelimiter(), extractProfile.getQuoteCharacter().charAt(0));
            else
                csvReader = new CSVReader(br, extractProfile.getDelimiter());

            String[] headerRow = null;
            String[] nextLine;
            if (extractProfile.hasHeader()) {
                while ((nextLine = csvReader.readNext()) != null) {
                    if (isHeaderRow(nextLine)) {
                        headerRow = nextLine;
                        break;
                    }
                }
            }
            watch.start();

            if (skipCount > 0)
                logger.info("Skipping first {} rows", skipCount);

            long then = System.currentTimeMillis();

            while ((nextLine = csvReader.readNext()) != null) {
                if (!ignoreRow(nextLine)) {
                    if (headerRow == null) {
                        headerRow = TransformationHelper.defaultHeader(nextLine, extractProfile.getContentModel());
                    }
                    currentRow++;
                    if (currentRow >= skipCount) {
                        if (currentRow == skipCount)
                            logger.info("Processing now begins at row {}", skipCount);

                        nextLine = preProcess(nextLine, extractProfile);

                        Map<String, Object> map = Transformer.convertToMap(headerRow, nextLine, extractProfile);

                        if (map != null) {
                            if (extractProfile.getContentModel().isTagModel() ) {
                                Collection<TagInputBean> tagInputBean = Transformer.toTags(map, extractProfile.getContentModel());
                                if (tagInputBean != null) {
                                    getPayloadBatcher().writeTags(tagInputBean, "TagInputBean");
                                }
                            } else {
                                EntityInputBean entityInputBean = Transformer.toEntity(map, extractProfile.getContentModel());
                                // Dispatch/load mechanism
                                if (entityInputBean != null)
                                    getPayloadBatcher().writeEntity(entityInputBean);
                            }
                            if (stopProcessing(currentRow, then)) {
                                break;
                            }
                        }
                    }
                } else {
                    ignoreCount++;
                }
            }
        } finally {

            getPayloadBatcher().flush();
            br.close();
        }

        return endProcess(watch, currentRow, ignoreCount);
    }

    private boolean isHeaderRow(String[] nextLine) {
        if (nextLine.length > 0) { // do we have data?
            if (nextLine[0].length() > 0) // is there a value in the first char?
                return !(nextLine[0].startsWith("#") || nextLine[0].charAt(1) == '#'); // is it a comment?
        }
        return true;
    }

    public boolean stopProcessing(long currentRow) {
        return stopProcessing(currentRow, 0L);
    }

    private boolean stopProcessing(long currentRow, long then) {
        //DAT-350

        if (rowsToProcess == 0) {
            if (currentRow % 1000 == 0)
                logger.info("Processed {} elapsed seconds {}", currentRow - skipCount, (new DateTime().getMillis() - then) / 1000d);
            return false;
        }
        boolean stop = currentRow >= skipCount + rowsToProcess;

        if (!stop && currentRow != skipCount && then > 0 && currentRow % 1000 == 0)
            logger.info("Processed {} elapsed seconds {}", currentRow - skipCount, (new DateTime().getMillis() - then) / 1000d);

        if (currentRow <= skipCount)
            return false;

        if (stop)
            logger.info("Process stopping after the {} requested rows.", rowsToProcess);

        return stop;
    }

    private boolean ignoreRow(String[] nextLine) {
        return nextLine[0].startsWith("#");
    }

    static StandardEvaluationContext context = new StandardEvaluationContext();

    private static String[] preProcess(String[] row, ExtractProfile extractProfile) {
        String[] result = new String[row.length];
        String exp = extractProfile.getPreParseRowExp();
        if ((exp == null || exp.equals("")))
            return row;
        int i = 0;
        for (String column : row) {

            Object value = evaluateExpression(column, exp);
            result[i] = value.toString();
            i++;


        }
        return result;
    }

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static Object evaluateExpression(Object value, String expression) {
        context.setVariable("value", value);
        return parser.parseExpression(expression).getValue(context);
    }

    public static Reader getReader(String file) throws NotFoundException {
        InputStream stream = ClassLoader.class.getResourceAsStream(file);

        Reader fileObject = null;
        try {
            fileObject = new FileReader(file);
        } catch (FileNotFoundException e) {
            if (stream != null)
                fileObject = new InputStreamReader(stream);

        }
        if (fileObject == null) {
            logger.error("Unable to resolve the source [{}]", file);
            throw new NotFoundException("Unable to resolve the source " + file);
        }
        return fileObject;
    }

    public int endProcess(StopWatch watch, int rows, int ignoreCount) {
        watch.stop();
        double mins = watch.getTotalTimeSeconds() / 60;
        long rowsProcessed = rows - skipCount;
        if (skipCount > 0)
            logger.info("Completed [{}] rows in [{}] secs. rpm [{}]. Skipped first [{}] rows, finished on row {}, ignored [{}] rows", rowsProcessed, formatter.format(watch.getTotalTimeSeconds()), formatter.format(rowsProcessed / mins), skipCount, rows,ignoreCount);
        else
            logger.info("Completed [{}] rows in [{}] secs. rpm [{}] Finished on row [{}], ignored [{}] rows.", rowsProcessed, formatter.format(watch.getTotalTimeSeconds()), formatter.format(rowsProcessed / mins), rows, ignoreCount);
        return rows;
    }

    public static boolean validateArgs(String pathToBatch) throws NotFoundException, IOException {
        Reader reader = getReader(pathToBatch);
        if (reader != null)
            reader.close();
        return true;
    }

    private PayloadBatcher getPayloadBatcher() {
        if (payloadBatcher == null ){
            logger.error("You are trying to use the FileProcessor but no FdBatcher has been configured for this service");
            throw new RuntimeException("Attempted use of the FileProcessor with no FdBatcher");
        }
        return payloadBatcher;
    }
}
