/*
 * Copyright (c) 2010-2014 The Amdatu Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inaetics.remote.admin.itest.api;

import java.util.List;

/**
 * Simple implementation of {@link ExtendedEchoInterface}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ExtendedEchoImpl implements ExtendedEchoInterface {

    @Override
    public String echo(String name) {
        return name;
    }

    @Override
    public String shout(String message) {
        return message == null ? null : message.toUpperCase();
    }

    @Override
    public EchoData echo(EchoData data) {
        return data;
    }

    @Override
    public List<EchoData> echo(List<EchoData> data) {
        return data;
    }
}
