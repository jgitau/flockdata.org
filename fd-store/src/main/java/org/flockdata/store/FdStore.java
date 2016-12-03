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

package org.flockdata.store;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Starts fd-store KV store service
 *
 * @tag Application
 * @author mholdsworth
 * @since 17/02/2016
 */
@SpringBootApplication(scanBasePackages = {
        "org.flockdata.integration",
        "org.flockdata.store",
        "org.flockdata.authentication"})
public class FdStore {
    public static void main(String[] args) {
        try {
            new SpringApplicationBuilder(FdStore.class).web(true).run(args);
        } catch ( Exception e) {
            System.out.println(e.getLocalizedMessage());
        }

    }
}
