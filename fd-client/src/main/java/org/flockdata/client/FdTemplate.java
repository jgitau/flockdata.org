/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.client;

import com.rabbitmq.client.AlreadyClosedException;
import org.apache.commons.codec.binary.Base64;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.model.ContentStructure;
import org.flockdata.search.model.QueryParams;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Functionality for writing Entity and Tag data to a FlockData service
 * over various transport mechanisms - RabbitMQ & Http are predominately used
 *
 * @see org.flockdata.client.Importer
 * @tag Messaging, FdClient
 * @author mholdsworth
 * @since 13/10/2013
 */
@Service
public class FdTemplate implements FdIoInterface {

    private static boolean compress = true;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdTemplate.class);
    private ClientConfiguration clientConfiguration;
    private FdRabbitClient fdRabbitClient;
    private RestTemplate restTemplate = null;
    private HttpHeaders httpHeaders = null;

    @SuppressWarnings("unused")
    public static Map<String, Object> getWeightedMap(int weight) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("weight", weight);
        return properties;
    }

    @Autowired(required = false)
    void setFdRabbitClient(FdRabbitClient fdRabbitClient) {
        this.fdRabbitClient = fdRabbitClient;
    }

    public SystemUserResultBean me() {
        Login login = new Login(this);
        return login.exec().result();
    }

    private ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    @Autowired
    void setClientConfiguration(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    /**
     * Simple ping to see if the service endpoint is up
     *
     * @return "pong"
     */
    public String ping() {
        Ping ping = new Ping(this);
        ping.exec();
        return ping.result();
    }

    /**
     * Registers a data access account. Security context is that of the user/pass
     * found in the ClientConfiguration
     *
     * @param userName name of the user account (should match an id in your security domain)
     * @param company  company that the userName will belong to
     * @return details about the system user data access account
     */
    public SystemUserResultBean register(String userName, String company) {
        RegistrationPost registrationPost = new RegistrationPost(this, new RegistrationBean(company, userName));
        SystemUserResultBean result = registrationPost.exec().result();
        if (result != null)
            clientConfiguration.setApiKey(result.getApiKey());
        return result;
    }

    private String writeEntitiesAmqp(Collection<EntityInputBean> entityInputs) throws FlockException {
        try {
            // DAT-373
            fdRabbitClient.publish(entityInputs);
        } catch (IOException ioe) {
            logger.error(ioe.getLocalizedMessage());
            throw new FlockException("IO Exception", ioe.getCause());
        }
        return "OK";

    }

    private String writeTagsAmqp(Collection<TagInputBean> tagInputs) throws FlockException {
        try {
            // DAT-373
            fdRabbitClient.publishTags(tagInputs);
        } catch (IOException | AlreadyClosedException ioe) {
            logger.error(ioe.getLocalizedMessage());
            throw new FlockException("IO Exception", ioe.getCause());
        }
        return "OK";

    }

    public String writeEntities(Collection<EntityInputBean> entityInputs) throws FlockException {
        if (entityInputs.isEmpty())
            return "OK";

        return writeEntitiesAmqp(entityInputs);

    }

    public RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        }
        return restTemplate;
    }

    public String writeTags(Collection<TagInputBean> tagInputs) throws FlockException {
        if (tagInputs.isEmpty())
            return "OK";

        return writeTagsAmqp(tagInputs);

    }

    public HttpHeaders getHeaders() {
        return getHeaders(clientConfiguration.getHttpUser(), clientConfiguration.getHttpPass(), clientConfiguration.getApiKey());
    }

    public HttpHeaders getHeaders(String user, String pass, final String apiKey) {
        String auth = user + ":" + pass;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);

        if (httpHeaders != null && httpHeaders.get("Authorization").iterator().next().equals(authHeader))
            return httpHeaders;

        httpHeaders = new HttpHeaders() {
            {
                if (clientConfiguration.getHttpUser() != null && clientConfiguration.getHttpPass() != null) {
                    set("Authorization", authHeader);
                }

                if (apiKey != null && !apiKey.equals(""))
                    set("api-key", apiKey);

                setContentType(MediaType.APPLICATION_JSON);
                set("charset", ObjectHelper.charSet.toString());

                if (compress)
                    set("Accept-Encoding", "gzip,deflate");
            }
        };

        return httpHeaders;
    }

    @Override
    public String toString() {
        return "FdRestWriter{" +
                "userName='" + clientConfiguration.getHttpUser() + '\'' +
                ", serviceEndpoint='" + clientConfiguration.getServiceUrl() +
                '}';
    }

    public SystemUserResultBean login() {
        Login login = new Login(this);
        if (login.exec().error() != null) {
            logger.error("Error logging in as [{}] - {}", getUser(), login.error());
            return null;
        }

        SystemUserResultBean suResult = login.result();
        if (suResult != null) {
            if (suResult.getApiKey() != null) {
                logger.info("Configuring apiKey for user {}", clientConfiguration.getHttpUser());
                clientConfiguration.setApiKey(suResult.getApiKey());
            } else {
                logger.debug("User [{}] authenticated at [{}] but is not a registered data access user", clientConfiguration.getHttpUser(), clientConfiguration.getServiceUrl());
            }

        }
        return suResult;

    }

    public ContentModel getContentModel(String modelKey) throws IOException {
        ContentModel contentModel;
        contentModel = ContentModelDeserializer.getContentModel(modelKey);
        if (contentModel == null) {
            // See if it can be found on the server
            // format is {fortress}:{docType}, or tag:doctype
            String[] args = modelKey.split(":");
            if (args.length == 2) {
                contentModel = getContentModel(args[0], args[1]);
            }
        }
        return contentModel;
    }

    public ContentModel getContentModel(String type, String clazz) {
        ModelGet modelGet = new ModelGet(this, type, clazz);
        modelGet.exec();
        String error = modelGet.exec().error();
        if (error != null)
            logger.error("Get Model resulted in {} for {} {} on {} for {}",
                    error, type, clazz,
                    clientConfiguration.getServiceUrl(),
                    clientConfiguration.getHttpUser());

        return modelGet.result();
    }

    ExtractProfile getExtractProfile(String name, ContentModel contentModel) {
        ExtractProfile extractProfile = null;

        try {
            extractProfile = ExtractProfileDeserializer.getImportProfile(name, contentModel);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);

        }
        if (extractProfile == null)
            extractProfile = new ExtractProfileHandler(contentModel);
        return extractProfile;
    }

    /**
     * Only called for integration testing - resets the host port which is proxied
     *
     * @param rabbitHost url
     * @param rabbitPort port
     */
    public void resetRabbitClient(String rabbitHost, Integer rabbitPort) {
        fdRabbitClient.resetRabbitClient(rabbitHost, rabbitPort);
    }

    @Override
    public SystemUserResultBean validateConnectivity() throws FlockException {
        boolean error = false;
        SystemUserResultBean me = null;
        if (clientConfiguration.getApiKey() == null || clientConfiguration.getApiKey().length() == 0) {
            error = true;
            me = me();
            if (me != null && me.isActive()) {   // Resolve the api key from a login result
                clientConfiguration.setApiKey(me.getApiKey());
                error = false;
            }

        }
        if (error) {
            throw new FlockException(String.format("Connectivity failed to %s for user %s - apiKey set == [%b] Without a key track requests will fail", clientConfiguration.getServiceUrl(), clientConfiguration.getHttpUser(), clientConfiguration.isApiKeyValid()));
        }
        return me;
    }


    public SystemUserResultBean login(String user, String pass) {
        httpHeaders = null;
        getClientConfiguration()
                .setHttpUser(user)
                .setHttpPass(pass)
                .setApiKey(null);

        return login();
    }

    public Map<String, Object> search(QueryParams qp) {
        SearchEsPost postQuery = new SearchEsPost(this, qp);
        if (postQuery.exec().error() != null)
            logger.error(postQuery.error());
        return postQuery.exec().result();
    }

    public String getUrl() {
        return clientConfiguration.getServiceUrl();
    }

    public String getUser() {
        return clientConfiguration.getHttpUser();
    }

    public String getPass() {
        return clientConfiguration.getHttpPass();
    }

    public void setServiceUrl(String serviceUrl) {
        logger.info("setting service URL to {}", serviceUrl);
        clientConfiguration.setServiceUrl(serviceUrl);
    }

    public ContentStructure entityFields(String fortress, String documentType) {
        ModelFieldStructure query = new ModelFieldStructure(this, fortress, documentType);
        query.exec();
        if (query.error() == null)
            return query.result();

        logger.error(query.error());
        return null;

    }
}
