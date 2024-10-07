/*
 *  Copyright 2022 Red Hat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wildfly.feature.pack.grpc;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by {@link ServerInterceptor}s to indicate the order in which they execute.
 *
 */
public class InterceptorTracker {

    private static String flag = "";
    private static List<Integer> list = new ArrayList<Integer>();

    public static synchronized void checkin(String s) {
        flag += s;
    }

    public static synchronized void checkin(Integer i) {
        list.add(i);
    }

    public static synchronized String getFlag() {
        return flag;
    }

    public static synchronized List<Integer> getList() {
        return list;
    }

    public static synchronized void reset() {
        flag = "";
        list = new ArrayList<Integer>();
    }
}
