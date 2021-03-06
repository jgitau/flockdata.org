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

package org.flockdata.test.engine.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.flockdata.authentication.FdRoles;
import org.flockdata.engine.FdEngine;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.profile.ContentModelHandler;
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.*;
import org.flockdata.test.engine.MapBasedStorageProxy;
import org.flockdata.test.engine.Neo4jConfigTest;
import org.flockdata.track.bean.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

/**
 * Base class for Web App context driven classes
 * Created by mike on 12/02/16.
 */
@WebAppConfiguration(value = "src/main/resources")
@ActiveProfiles({"dev", "web-dev", "fd-auth-test"})
@SpringApplicationConfiguration({FdEngine.class,
        Neo4jConfigTest.class,
        MapBasedStorageProxy.class})

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class MvcBase {

    private static final String apiRoot = "/api";
    private static final String OTHERCO = "otherco";
    static final String LOGIN_PATH = apiRoot + "/login";
    static final String apiPath = apiRoot + "/v1";
    static final String ANYCO = "anyco";

    public static String harry = "harry";
    public static String mike_admin = "mike";
    static String sally_admin = "sally"; // admin in a different company
    ResultMatcher OK = MockMvcResultMatchers.status().isOk();
    static Logger logger = LoggerFactory.getLogger(MvcBase.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    SystemUserResultBean suHarry;
    SystemUserResultBean suMike;
    SystemUserResultBean suSally;
    SystemUserResultBean suIllegal;

    @Autowired
    public WebApplicationContext wac;

    @Autowired
    Neo4jTemplate neo4jTemplate;

    private MockMvc mockMvc;

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    public void cleanUpGraph() throws Exception {
        // DAT-348 - override this if you're running a multi-threaded tests where multiple transactions
        //           might be started giving you sporadic failures.
        neo4jTemplate.query("match (n)-[r]-() delete r", null);
        neo4jTemplate.query("match (n) delete n", null);

    }

    @Before
    public void setupMvc() throws Exception {
        engineConfig.setMultiTenanted(false);
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders
                    .webAppContextSetup(wac)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }
        cleanUpGraph();
        suMike = makeDataAccessProfile(ANYCO, mike_admin);
        suHarry = makeDataAccessProfile(mike(), ANYCO, harry);// Harry works at Anyco where Mike is the administrator
        suSally = makeDataAccessProfile(OTHERCO, sally_admin);
        suIllegal = new SystemUserResultBean(new SystemUser("illegal", "noone", null, false).setApiKey("blahh"));


    }

    public SystemUserResultBean makeDataAccessProfile(String companyName, String accessUser) throws Exception {
        return makeDataAccessProfile(mike(), companyName, accessUser);
    }

    public MockMvc mvc() throws Exception {
        if (mockMvc == null)
            setupMvc();
        return mockMvc;
    }

    static void setSecurityEmpty() {
        SecurityContextHolder.clearContext();
    }

    /**
     * @return mike - works for AnyCo
     */
    public RequestPostProcessor mike() {
        return user(mike_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    /**
     * @return sally - works for OtherCo
     */
    public RequestPostProcessor sally() {
        return user(sally_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    public RequestPostProcessor harry() {
        return user(harry).password("123").roles(FdRoles.FD_USER);
    }

    static RequestPostProcessor noUser() {
        return user("noone");
    }

    public void setSecurity() throws Exception {
    }

    FortressResultBean updateFortress(RequestPostProcessor user, String code, FortressInputBean update, ResultMatcher resultMatch) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath + "/fortress/{code}", code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user)
                                .content(
                                        JsonUtils
                                                .toJson(update)))
                .andExpect(resultMatch)
                .andReturn();
        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), FortressResultBean.class);
        }

        throw response.getResolvedException();

    }

    FortressResultBean updateFortress(RequestPostProcessor user, String code, FortressInputBean update) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath + "/fortress/{code}", code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user)
                                .content(
                                        JsonUtils
                                                .toJson(update)))
                .andReturn();
        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), FortressResultBean.class);
        }

        throw response.getResolvedException();

    }

    FortressResultBean makeFortress(RequestPostProcessor user, String fortressName)
            throws Exception {
        return this.makeFortress(user, new FortressInputBean(fortressName, true));
    }

    public FortressResultBean makeFortress(RequestPostProcessor user, FortressInputBean fortressInputBean) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath + "/fortress/")
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.toJson(fortressInputBean))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toObject(json, FortressResultBean.class);

    }

    public Collection<DocumentResultBean> getDocuments(SystemUser su, Collection<String> fortresses) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders
                .post(apiPath + "/doc/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentResultBean.class);
    }

    public DocumentResultBean getDocument(RequestPostProcessor user, String fortress, String docName) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders
                .get(apiPath + "/doc/{fortress}/{docName}", fortress,docName)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(JsonUtils.toJson(fortress))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toObject(json, DocumentResultBean.class);
    }

    public Collection<DocumentType> getRelationships(SystemUserResultBean su, Collection<String> fortresses) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath + "/query/relationships/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentType.class);
    }

    public MatrixResults getMatrixResult(SystemUser su, MatrixInputBean input) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath + "/query/matrix/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), MatrixResults.class);
    }

    public SystemUserResultBean makeDataAccessProfile(RequestPostProcessor user, String company, String accessUser) throws Exception {
        return makeDataAccessProfile(user, company, accessUser, MockMvcResultMatchers.status().isCreated());
    }

    public SystemUserResultBean makeDataAccessProfile(RequestPostProcessor user, String company, String accessUser, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath + "/profiles/")
                        .content(JsonUtils.toJson(new RegistrationBean(company, accessUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), SystemUserResultBean.class);
        }
        throw response.getResolvedException();
    }

    public EntityBean getEntity(RequestPostProcessor user, String key) throws Exception {
        return getEntity(user, key, MockMvcResultMatchers.status().isOk());
    }

    public EntityBean getEntity(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/{key}", key)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(status).andReturn();

        if (response.getResolvedException() != null) {
            throw response.getResolvedException();
        }
        String json = response.getResponse().getContentAsString();
        return JsonUtils.toObject(json.getBytes(), EntityBean.class);
    }

    public Map<String, Object> getHealth(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/admin/health/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toMap(json);
    }

    public Map<String, Object> getHealth(SystemUserResultBean su) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/admin/health/")
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                .contentType(MediaType.APPLICATION_JSON)
                .with(noUser())
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toMap(json);
    }

    public String ping() throws Exception {
        ResultActions result = mvc()
                .perform(
                        MockMvcRequestBuilders.get(apiRoot + "/ping"));
        return result.andReturn().getResponse().getContentAsString();
    }

    /**
     * Tests logging in to the API over the REST endpoint.
     *
     * @param user who are you?
     * @param pass password
     * @return MvcResult
     * @throws Exception anything goes wrong
     */
    public MvcResult login(String user, String pass) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, pass)
        );

        return mvc()
                .perform(
                        MockMvcRequestBuilders.post(LOGIN_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.toJson(new LoginRequest(user, pass))))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    }

    public MvcResult login(RequestPostProcessor user) throws Exception {

        return mvc()
                .perform(
                        MockMvcRequestBuilders.post(LOGIN_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.toJson(user)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    }

    public TrackRequestResult track(RequestPostProcessor user, EntityInputBean eib) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath + "/track/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(JsonUtils.toJson(eib))
        ).andDo(log())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TrackRequestResult.class);
    }

    public Company getCompany(String name, RequestPostProcessor user) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders
                .get(apiPath + "/company/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toObject(json, Company.class);
    }

    public boolean findCompanyIllegal(String name, RequestPostProcessor user) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath + "/company/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)

        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
        return true;
    }

    public Collection<Company> findCompanies(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/company/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();
        return JsonUtils.toCollection(json, Company.class);

    }

    public String authPing(RequestPostProcessor user, ResultMatcher expectedResult) throws Exception {

        ResultActions result = mvc()
                .perform(
                        MockMvcRequestBuilders.get(apiPath + "/admin/ping")
                                .with(user)
                )
                .andExpect(expectedResult);
        return result.andReturn().getResponse().getContentAsString();

    }

    public void purgeFortress(RequestPostProcessor user, String fortressName, ResultMatcher expectedResult) throws Exception {

        mvc()
                .perform(
                        MockMvcRequestBuilders.delete(apiPath + "/admin/{fortressName}", fortressName)
                                .with(user)
                ).andExpect(expectedResult);

    }

    public FortressResultBean getFortress(RequestPostProcessor user, String code) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath + "/fortress/{code}", code)
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), FortressResultBean.class);
        }
        throw response.getResolvedException();


    }

    public FortressInputBean getDefaultFortress(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath + "/fortress/defaults")
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), FortressInputBean.class);
        }
        throw response.getResolvedException();


    }

    public ContentModel getDefaultContentModel(RequestPostProcessor user, ContentValidationRequest content) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath + "/model/default")
                                .content(JsonUtils.toJson(content))
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentModelHandler.class);
        }
        throw response.getResolvedException();


    }

    public Collection<FortressResultBean> getFortresses(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath + "/fortress/")
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toCollection(json, FortressResultBean.class);

    }

    public Collection<TagResultBean> getTags(RequestPostProcessor user, String label) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/" + label)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toCollection(json, TagResultBean.class);

    }

    public TagResultBean getTagWithPrefix(RequestPostProcessor user, String label, String keyPrefix, String code) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");

        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/tag/{label}/{prefix}/{code}", label, keyPrefix, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TagResultBean.class);

    }

    public TagResultBean getTag(RequestPostProcessor user, String label, String code, ResultMatcher resultMatch) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/tag/{label}/{code}", label, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)

                )
                .andExpect(resultMatch).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TagResultBean.class);
    }

    public void getTagNotFound(RequestPostProcessor user, String label, String code) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/" + label + "/" + code)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)

        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


    }

    public Collection<DocumentResultBean> getDocuments(RequestPostProcessor user, String fortress) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/doc/" + fortress)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentResultBean.class);
    }

    public Collection<ConceptResultBean> getLabelsForDocument(RequestPostProcessor user, String docResultName) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/concept/{doc}/values" , docResultName)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, ConceptResultBean.class);

    }

    public Collection<TagResultBean> createTag(RequestPostProcessor user, TagInputBean tag) throws Exception {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        tags.add(tag);
        MvcResult response = mvc().perform(MockMvcRequestBuilders.put(apiPath + "/tag/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(tags))
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, TagResultBean.class);
    }

    public Collection<TagResultBean> getCountries(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/geo/")
                //.contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, TagResultBean.class);
    }

    public Collection<TagResultBean> getTags(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, TagResultBean.class);
    }

    ContentValidationRequest batchRequest(RequestPostProcessor user, ContentValidationRequest validationRequest) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath + "/batch/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user)
                                .content(JsonUtils.toJson(validationRequest))).andReturn();

        if (response.getResolvedException() == null) {
            byte[] json = response.getResponse().getContentAsByteArray();
            return JsonUtils.toObject(json, ContentValidationRequest.class);

        }

        throw (response.getResolvedException());

    }

    public Map<String, Object> getConnectedTags(RequestPostProcessor user, String label, String code, String relationship, String targetLabel) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/" + label + "/" + code + "/path/" + relationship + "/" + targetLabel)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toMap(json);

    }

    public Collection<EntityLogResult> getEntityLogs(RequestPostProcessor user, String key) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/" + key + "/log")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, EntityLogResult.class);
    }

    public void getEntityLogsIllegalEntity(RequestPostProcessor user, String key) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/" + key + "/log")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
    }

    public Collection<EntityTagResult> getEntityTags(RequestPostProcessor user, String key) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/{key}/tags", key)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, EntityTagResult.class);
    }

    public DocumentResultBean makeDocuments(RequestPostProcessor user, MetaFortress fortress, DocumentTypeInputBean docTypes) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath + "/fortress/{code}/doc", fortress.getCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user)
                                .content(JsonUtils.toJson(docTypes))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toObject(json, DocumentResultBean.class);
    }

    public Collection<DocumentResultBean> getDocumentWithSegments(RequestPostProcessor user, String fortressCode, String docType) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/fortress/{fortress}/{doc}/segments", fortressCode, docType)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json.getBytes(), DocumentResultBean.class);

    }

    public Collection<FortressSegment> getDocumentWithSegments(RequestPostProcessor user, String fortressCode) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/fortress/{fortressCode}/segments", fortressCode)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, FortressSegment.class);

    }

    public Collection<Map<String, TagResultBean>> getTagPaths(RequestPostProcessor user, String label, String code, String targetLabel) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        targetLabel = URLEncoder.encode(targetLabel, "UTF-8");

        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath + "/path/{label}/{code}/{depth}/{lastLabel}", label, code, "4", targetLabel)
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return FdJsonObjectMapper.getObjectMapper().readValue(json, new TypeReference<Collection<Map<String, TagResultBean>>>() {
        });
//        return JsonUtils.getAsType(json, type )


    }

    public ContentModelResult makeTagModel(RequestPostProcessor user, String code, ContentModel contentModel, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath + "/model/tag/{code}", code)
                        .content(JsonUtils.toJson(contentModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentModelResult.class);
        }
        throw response.getResolvedException();
    }

    public ContentModelResult makeContentModel(RequestPostProcessor user, String fortress, String documentType, ContentModel contentModel, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath + "/model/{fortress}/{documentType}", fortress, documentType)
                        .content(JsonUtils.toJson(contentModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentModelResult.class);
        }
        throw response.getResolvedException();
    }

    public Collection<ContentModelResult> makeContentModels(RequestPostProcessor user, Collection<ContentModel> contentModel, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath + "/model/")
                        .content(JsonUtils.toJson(contentModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toCollection(json.getBytes(), ContentModelResult.class);
        }
        throw response.getResolvedException();
    }

    public Collection<ContentModel> findContentModels(RequestPostProcessor user, Collection<String> contentModelKeys, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath + "/model/download")
                        .content(JsonUtils.toJson(contentModelKeys))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toCollection(json.getBytes(), ContentModel.class);
        }
        throw response.getResolvedException();
    }

    public Collection<ContentModelResult> findContentModels(RequestPostProcessor user, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/model/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toCollection(json.getBytes(), ContentModelResult.class);
        }
        throw response.getResolvedException();
    }

    public ContentModelResult findContentModelByKey(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/model/{key}", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentModelResult.class);
        }
        throw response.getResolvedException();

    }

    public void deleteContentModel(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.delete(apiPath + "/model/{key}", key)
                        .with(user)
                ).andExpect(status).andReturn();
        if (response.getResolvedException() != null) {
            throw response.getResolvedException();
        }
    }

    public ContentModel getContentModel(RequestPostProcessor user, String fortress, String documentType, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/model/{fortress}/{documentType}", fortress, documentType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentModelHandler.class);
        }
        throw response.getResolvedException();
    }

    public ContentModel getContentModel(RequestPostProcessor user, String code, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/model/tag/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentModelHandler.class);
        }
        throw response.getResolvedException();
    }

    ContentValidationResults validateContentModel(RequestPostProcessor user, ContentValidationRequest contentProfile, ResultMatcher result) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath + "/model/validate")
                        .content(JsonUtils.toJson(contentProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(result).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), ContentValidationResults.class);
        }
        throw response.getResolvedException();
    }

    public MatrixResults getContentStructure(RequestPostProcessor user, String fortressCode, ResultMatcher status) throws Exception {
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath + "/concept/{code}/structure", fortressCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andExpect(status).andReturn();

        if (response.getResolvedException() == null) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), MatrixResults.class);
        }
        throw response.getResolvedException();
    }


}
