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

package org.flockdata.batch.resources;

import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.PayloadBatcher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batches the item to the payload ready for transmission
 *
 * @author nabil
 */
@Component
@Profile({"fd-batch", "fd-batch-dev"})
@Service
public class FdTagWriter implements ItemWriter<TagInputBean> {

    @Autowired
    private  PayloadBatcher payloadBatcher;

    @Override
    public void write(List<? extends TagInputBean> items) throws Exception {

        for (TagInputBean item : items) {
            payloadBatcher.writeTag(item, "");
        }
    }
}
