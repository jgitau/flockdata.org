package org.flockdata.shared;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.search.model.QueryParams;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides centralized access to the way that FD handles Entity data for different
 * databases
 * <p/>
 * Created by mike on 23/07/15.
 */
@Configuration
public class IndexManager {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${fd.search.index.prefix:fd.}")
    private String prefix ;

    @Value("${fd.search.index.typeSuffix:true}")
    private Boolean typeSuffix ;   // use docType as an index suffix?

    IndexManager(){}

    public IndexManager(String prefix, boolean typeSuffix){
        this.prefix = prefix;
        this.typeSuffix = typeSuffix;
    }

    @PostConstruct
    void dumpConfig(){
        logger.info("**** FlockData index variables prefix [{}], suffixing with type [{}]",prefix, typeSuffix);
    }

    public String getPrefix() {
        return prefix;
    }

    public Boolean isSuffixed() {
        return typeSuffix;
    }

    /**
     *
     * @param store   target datastore
     * @param entity  object containing properties to parse
     * @return fully qualified index
     */
    public String parseIndex(Store store, Entity entity) {
        if (entity == null)
            return null;

        if ( store== Store.RIAK)
            return parseBucket(entity);
        else
            return parseIndex(entity);
    }

    /**
     * Default way of building an Index. Works for most database types including
     * ElasticSearch
     * @param entity properties
     * @return parsed index
     */
    public String parseIndex(Entity entity) {
        if (entity.getSegment().isDefault())
            return entity.getSegment().getFortress().getRootIndex() + getSuffix(entity);
        else {
            String index = entity.getSegment().getFortress().getRootIndex() + getSuffix(entity);
            index = index + "." + entity.getSegment().getCode().toLowerCase();
            return index;
        }
    }

    public String parseBucket(Entity entity) {
        return (entity.getSegment().getKey()).toLowerCase();
    }

    /**
     * The suffix, if any, to use for the index. Depends on fd.search.index.typeSuffix==true
     * @param entity to analyse
     * @return coded DocumentType
     */
    private String getSuffix(Entity entity) {
        if (isSuffixed())
            return "."+parseType(entity.getType());
        else
            return "";
    }

    public String getIndexRoot(Fortress fortress) {
        return getIndexRoot(fortress.getCompany().getCode(), fortress.getCode());
    }

    // Returns the root level of the index with no doctype or consideration of a segment
    public String getIndexRoot(String company, String fortress) {
        String fort = (fortress == null || fortress.equals("*") ? "" : "."+fortress.toLowerCase());
        return getPrefix() + company.toLowerCase() + fort ;
    }

    @Deprecated // Parse from the Entity
    public String parseIndex(QueryParams queryParams) {
        String indexRoot = getIndexRoot(queryParams.getCompany(), queryParams.getFortress());
        if ( isDefaultSegment(queryParams.getSegment()))
            return indexRoot;
        return String.format("%s.%s", indexRoot, queryParams.getSegment());

    }

    /**
     * Computes ES indexes, including wildcards, from the supplied query parameters
     *
     * @param queryParams  Args holding parameters to use in the query
     * @return one index per doc type
     * @throws FlockException
     */
    public String[] getIndexesToQuery(QueryParams queryParams) throws FlockException {
        return getIndexesToQuery(queryParams.getCompany(), queryParams.getFortress(), queryParams.getSegment(), queryParams.getTypes());
    }

    /**
     * @param company  owns the index
     * @param fortress owns the index data
     * @param segment  optional segment to restrict by
     * @param types    types to scan
     * @return One index line per Root+Type combination
     */
    public String[] getIndexesToQuery(String company, String fortress, String segment, String[] types) {
        int length = 1;
        if (types != null && types.length > 0)
            length = 1;
        String[] results = new String[length];
        Collection<String> found = new ArrayList<>();

        String indexRoot = getPrefix() + company.toLowerCase() ;
        String segmentFilter ="";

        if (!isDefaultSegment(segment))
            segmentFilter = "."+segment.toLowerCase();

        String fortressFilter ;
        if ( fortress == null || fortress.equals("*"))
            fortressFilter=".*";
       else
            fortressFilter = (segmentFilter.equals("")?"."+fortress.toLowerCase()+"*":"."+fortress.toLowerCase());

        indexRoot = indexRoot+fortressFilter+ segmentFilter;

        if (types == null || types.length == 0) {
            results[0] = indexRoot ;
        } else {
            int count = 0;
            for (String type : types) {
                if (!found.contains(indexRoot)) {
                    results[count] = indexRoot; //+ "."+type.toLowerCase();
                    found.add(indexRoot);
                    count++;
                }
            }
        }

        return results;
    }

    public String parseType(Entity entity) {
        return parseType(entity.getType());
    }

    public String parseType(String type) {
        return type.toLowerCase();
    }

    // Determines if the segment is a regular default
    private static boolean isDefaultSegment(String segment){
        return segment == null || segment.equals(FortressSegment.DEFAULT);
    }

    public String resolveKey(Store store, LogRequest logRequest) {
        if ( store == Store.NONE){
            // ElasticSearch
            return logRequest.getEntity().getSearchKey();
        }
        return logRequest.getLogId().toString();

    }
}
